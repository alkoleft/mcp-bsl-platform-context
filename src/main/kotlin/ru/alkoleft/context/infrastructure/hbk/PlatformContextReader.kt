/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.alkoleft.context.infrastructure.hbk.models.EnumInfo
import ru.alkoleft.context.infrastructure.hbk.models.MethodInfo
import ru.alkoleft.context.infrastructure.hbk.models.ObjectInfo
import ru.alkoleft.context.infrastructure.hbk.models.Page
import ru.alkoleft.context.infrastructure.hbk.parsers.PlatformContextPagesParser
import ru.alkoleft.context.infrastructure.hbk.reader.HbkContentReader
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

/**
 * Основной класс для чтения контекста платформы 1С:Предприятие из HBK файлов.
 *
 * Этот класс координирует весь процесс извлечения и парсинга документации
 * из HBK файлов платформы 1С:Предприятие. Он обходит структуру страниц,
 * определяет их типы и делегирует парсинг специализированным парсерам.
 *
 * Основные возможности:
 * - Чтение и анализ структуры HBK файла
 * - Определение типов страниц (перечисления, объекты, глобальный контекст)
 * - Координация работы специализированных парсеров
 * - Сбор информации о перечислениях, объектах, методах и свойствах
 *
 * @see ru.alkoleft.context.infrastructure.hbk.reader.HbkContentReader для доступа к содержимому HBK файла
 * @see PlatformContextPagesParser для парсинга различных типов страниц
 */
class PlatformContextReader {
    private val enums = mutableListOf<EnumInfo>()
    private val objects = mutableListOf<ObjectInfo>()

    /**
     * Читает контекст платформы из HBK файла.
     *
     * @param path Путь к HBK файлу
     */
    fun read(
        path: Path,
        block: Context.() -> Unit,
    ) {
        val reader = HbkContentReader()
        reader.read(path) {
            val visitor = PagesVisitor(PlatformContextPagesParser(this))
            val rootPages = visitor.collectPages(toc.pages)
            val context =
                Context(
                    visitor = visitor,
                    globalContextPage = rootPages.globalContext,
                    enumPages = rootPages.enums,
                    typePages = rootPages.types,
                )
            context.block()
        }
    }

    companion object {
        private val EVENT_MODULES =
            mapOf(
                "События приложения" to "Модуль приложения",
                "События модуля сеанса" to "Модуль сеанса",
                "События обычного приложения" to "Модуль обычного приложения",
                "События внешнего соединения" to "Модуль внешнего соединения",
            )
        private val EVENT_MODULE_ORDER = EVENT_MODULES.values.toList()

        fun eventModuleName(catalogTitle: String) = EVENT_MODULES[catalogTitle] ?: catalogTitle
    }

    class Context(
        private val visitor: PagesVisitor,
        private val globalContextPage: Page,
        private val enumPages: List<Page>,
        private val typePages: List<Page>,
    ) {
        fun types() =
            sequence {
                for (page in typePages) {
                    drillDown(page)
                }
            }.map(visitor::visitTypePage)

        fun enums() =
            sequence {
                for (page in enumPages) {
                    drillDown(page)
                }
            }.mapNotNull { page ->
                // Ошибка разбора одной страницы перечисления не должна ронять загрузку всего контекста
                runCatching { visitor.visitEnumPage(page) }
                    .onFailure { logger.warn { "Пропущено перечисление ${page.htmlPath}: ${it.message}" } }
                    .getOrNull()
            }

        fun globalMethods() =
            sequence {
                for (page in globalContextPage.children) {
                    if (page.htmlPath.contains("/methods/")) {
                        yield(page)
                    }
                }
            }.flatMap(visitor::visitMethodsPage)

        /**
         * Глобальные события с указанием модуля, к которому они относятся.
         * Одно и то же имя (например, ПриНачалеРаботыСистемы) встречается в нескольких модулях,
         * поэтому события упорядочены по приоритету модуля: сначала модуль (управляемого) приложения.
         */
        fun globalEvents(): List<Pair<String, MethodInfo>> =
            globalContextPage.children
                .filter { it.htmlPath.contains("/events/") }
                .map { page -> eventModuleName(page.title.en.ifBlank { page.title.ru }) to page }
                .sortedBy { (module, _) -> EVENT_MODULE_ORDER.indexOf(module).let { if (it < 0) Int.MAX_VALUE else it } }
                .flatMap { (module, page) -> visitor.visitEventsPage(page).map { module to it }.toList() }

        fun globalProperties() =
            globalContextPage.children
                .first { it.title.en == "Свойства" }
                .let(visitor::visitPropertiesPage)
    }
}
