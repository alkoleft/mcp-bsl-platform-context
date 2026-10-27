/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.alkoleft.context.infrastructure.hbk.models.ConstructorInfo
import ru.alkoleft.context.infrastructure.hbk.models.EnumInfo
import ru.alkoleft.context.infrastructure.hbk.models.MethodInfo
import ru.alkoleft.context.infrastructure.hbk.models.ObjectInfo
import ru.alkoleft.context.infrastructure.hbk.models.Page
import ru.alkoleft.context.infrastructure.hbk.models.PageType
import ru.alkoleft.context.infrastructure.hbk.models.PropertyInfo
import ru.alkoleft.context.infrastructure.hbk.parsers.PlatformContextPagesParser

private val logger = KotlinLogging.logger { }

private val CATALOG_PAGE_PATTERN = """/catalog\d+\.html""".toRegex()

class PagesVisitor(
    private val parser: PlatformContextPagesParser,
) {
    fun collectPages(pages: List<Page>): RootPages {
        var globalContext: Page? = null
        val enums = mutableListOf<Page>()
        val types = mutableListOf<Page>()

        pages
            .filter { it.htmlPath.isNotEmpty() }
            .forEach {
                val pageType = rootPageType(it)
                when (pageType) {
                    PageType.GLOBAL_CONTEXT -> globalContext = it
                    PageType.ENUMS_CATALOG -> enums += it
                    PageType.TYPES_CATALOG -> types += it
                }
            }
        return object : RootPages {
            override val globalContext: Page = globalContext!!
            override val enums: List<Page> = enums.toList()
            override val types: List<Page> = types.toList()
        }
    }

    interface RootPages {
        val globalContext: Page
        val enums: List<Page>
        val types: List<Page>
    }

    /**
     * Обрабатывает страницу перечисления.
     *
     * @param page Страница перечисления
     * @param parser Парсер для обработки страниц
     */
    fun visitEnumPage(page: Page): EnumInfo {
        val values =
            page.children
                .filter { it.htmlPath.contains("/properties/") }
                .map { parser.parseEnumValuePage(it) }
        return parser
            .parseEnumPage(page)
            .apply { this.values.addAll(values) }
            .copy(tocName = page.title.ru.ifBlank { null })
    }

    /**
     * Обрабатывает страницу типа (объекта).
     *
     * @param page Страница типа
     * @param parser Парсер для обработки страниц
     */
    fun visitTypePage(page: Page): ObjectInfo {
        val objectInfo = parser.parseObjectPage(page)
        var properties: Sequence<PropertyInfo>? = null
        var methods: Sequence<MethodInfo>? = null
        var constructors: Sequence<ConstructorInfo>? = null
        var events: Sequence<MethodInfo>? = null

        for (subPage in page.children) {
            when (subPage.title.en) {
                "Свойства" -> properties = visitPropertiesPage(subPage)
                "Методы" -> methods = visitMethodsPage(subPage)
                "Конструкторы" -> constructors = getConstructorsFromPage(subPage)
                "События" -> events = visitEventsPage(subPage)
            }
        }
        return objectInfo.copy(
            methods = methods?.toList(),
            properties = properties?.toList(),
            constructors = constructors?.toList(),
            events = events?.toList(),
            tocName = page.title.ru.ifBlank { null },
        )
    }

    fun visitPropertiesPage(page: Page) =
        page.children
            .asSequence()
            .filter { it.htmlPath.contains("/properties/") } // TODO проверить на обязательность
            .filter { !it.title.ru.startsWith("<") }
            .map { parser.parsePropertyPage(it) }

    fun visitMethodsPage(page: Page) =
        page.children
            .asSequence()
            .map { parser.parseMethodPage(it) }

    /** Страницы событий устроены как страницы методов (Синтаксис, Параметры, Описание...). */
    fun visitEventsPage(page: Page) =
        page.children
            .asSequence()
            .filter { it.htmlPath.contains("/events/") }
            .mapNotNull { eventPage ->
                // Ошибка разбора одного события не должна ронять загрузку типа
                runCatching { parser.parseMethodPage(eventPage) }
                    .onFailure { logger.warn { "Пропущено событие ${eventPage.htmlPath}: ${it.message}" } }
                    .getOrNull()
            }

    private fun getConstructorsFromPage(page: Page) =
        page.children
            .asSequence()
            .filter { it.htmlPath.contains("/ctors/") }
            .map { parser.parseConstructorPage(it) }
}

private fun rootPageType(page: Page) =
    when {
        isGlobalContextPage(page) -> PageType.GLOBAL_CONTEXT
        isEnumCatalog(page) -> PageType.ENUMS_CATALOG
        else -> PageType.TYPES_CATALOG
    }

private fun isGlobalContextPage(page: Page): Boolean = page.htmlPath.contains("Global context.html")

private fun isCatalogPage(page: Page) = CATALOG_PAGE_PATTERN.find(page.htmlPath) != null

private fun isEnumCatalog(page: Page) = page.title.en == "Системные наборы значений" || page.title.en == "Системные перечисления"

suspend fun SequenceScope<Page>.drillDown(base: Page) {
    base.children.forEach { child ->
        when {
            child.htmlPath.isEmpty() -> {}
            isCatalogPage(child) -> drillDown(child)
            else -> yield(child)
        }
    }
}
