/*
 * Copyright (c) 2024-2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.presentation.mcp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import ru.alkoleft.context.business.services.ContextSearchService
import ru.alkoleft.context.business.services.ResponseFormatterService

private val logger = KotlinLogging.logger {}

/**
 * MCP Service для поиска API платформы 1С
 *
 * Presentation Layer компонент, предоставляющий MCP интерфейс для поиска API
 */
@Service
class PlatformContextMcpController(
    private val searchService: ContextSearchService,
    private val formatter: ResponseFormatterService,
) {
    /**
     * Поиск по API платформы 1С Предприятие
     */
    @Tool(
        name = "search",
        description = "Поиск по API платформы 1С Предприятие. Используйте конкретные термины 1С для получения точных результатов.",
    )
    fun search(
        @ToolParam(
            description =
                "Поисковый запрос. Используйте конкретные термины из 1С: " +
                    "методы ('СтрНайти', 'ВыполнитьПроверкуПравДоступа'), " +
                    "типы ('Справочник', 'Документ'), " +
                    "свойства ('БезопасноеХранилище', 'ПланыСчетов', 'СредстваМультимедиа')",
        )
        query: String,
        @ToolParam(
            description =
                "Тип искомого элемента API: " +
                    "'method' - глобальные методы, " +
                    "'property' - глобальные свойства, " +
                    "'type' - типы данных, " +
                    "null - все типы",
        )
        type: String? = null,
        @ToolParam(description = "Максимальное количество результатов (по умолчанию 10, максимум 50)")
        limit: Int? = null,
    ): String {
        logger.debug { "search called with query='$query', type='$type', limit='$limit'" }
        try {
            val result = searchService.searchAll(query, type, limit)
            logger.debug { "search result size: ${result.size}" }
            return formatter.formatQuery(query) +
                formatter.formatSearchResults(result)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при выполнении поиска" }
            return formatter.formatError(e)
        }
    }

    /**
     * Получение детальной информации об API элементе
     */
    @Tool(
        name = "info",
        description = "Получение детальной информации об элементе API платформы 1С. Требует точное имя элемента.",
    )
    fun getInfo(
        @ToolParam(description = "Точное имя элемента API в 1С. Примеры: 'НайтиПоСсылке', 'СправочникСсылка', 'Ссылка', 'Код'")
        name: String,
        @ToolParam(
            description =
                "Уточнение типа элемента: " +
                    "'method' - метод/функция, " +
                    "'property' - свойство/реквизит, " +
                    "'type' - тип данных",
        )
        type: String,
    ): String {
        logger.debug { "getInfo called with name='$name', type='$type'" }
        try {
            val definition = searchService.getInfo(name, type)
            logger.debug { "getInfo result: $definition" }
            return formatter.formatMember(definition)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при получении информации об элементе" }
            return "❌ **Ошибка:** ${e.message}"
        }
    }

    /**
     * Получение информации об элементе типа (методе или свойстве)
     */
    @Tool(
        name = "getMember",
        description = "Получение информации о методе, свойстве или событии конкретного типа 1С. Используйте точные имена типов и элементов. Глобальные методы, свойства и события (ПередНачаломРаботыСистемы, ПриНачалеРаботыСистемы и т.п.) — тип 'ГлобальныйКонтекст'.",
    )
    fun getMember(
        @ToolParam(description = "Имя типа 1С. Примеры: 'СправочникСсылка', 'ДокументОбъект', 'Строка', 'Число', 'Дата'")
        typeName: String,
        @ToolParam(description = "Имя метода, свойства или события типа. Примеры: 'НайтиПоКоду', 'Записать', 'Код', 'Наименование', 'Длина', 'ПриОткрытии'")
        memberName: String,
    ): String {
        logger.debug { "getMember called with typeName='$typeName', memberName='$memberName'" }
        try {
            val definition = searchService.findMemberByTypeAndName(typeName, memberName)
            logger.debug { "getMember result: $definition" }
            return formatter.formatMember(definition)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при получении информации об элементе типа" }
            return formatter.formatError(e)
        }
    }

    /**
     * Получение полного списка всех методов и свойств для указанного типа 1С
     */
    @Tool(
        name = "getMembers",
        description = "Получение полного списка всех методов, свойств и событий для указанного типа 1С. Полный справочник API типа.",
    )
    fun getMembers(
        @ToolParam(
            description =
                "Имя типа 1С для получения полного списка методов и свойств. " +
                    "Примеры: 'СправочникСсылка', 'ДокументОбъект', 'Строка', 'ТаблицаЗначений', 'Запрос'",
        )
        typeName: String,
    ): String {
        logger.debug { "getMembers called with typeName='$typeName'" }
        try {
            val definition = searchService.findTypeMembers(typeName)
            logger.debug { "getMembers result size: ${definition.size}" }
            return formatter.formatTypeMembers(definition)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при получении информации об элементе типа" }
            return formatter.formatError(e)
        }
    }

    /**
     * Получение списка конструкторов для указанного типа 1С
     */
    @Tool(
        name = "getConstructors",
        description = "Получение списка конструкторов для указанного типа 1С. Показывает способы создания объектов данного типа.",
    )
    fun getConstructors(
        @ToolParam(
            description =
                "Имя типа 1С для получения конструкторов. " +
                    "Примеры: 'СправочникМенеджер', 'ДокументМенеджер', 'Запрос', 'ТаблицаЗначений'",
        )
        typeName: String,
    ): String {
        logger.debug { "getConstructors called with typeName='$typeName'" }
        try {
            val result = searchService.findConstructors(typeName)
            logger.debug { "getConstructors result size: ${result.size}" }
            return formatter.formatConstructors(result, typeName)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка при получении информации об элементе типа" }
            return formatter.formatError(e)
        }
    }
}
