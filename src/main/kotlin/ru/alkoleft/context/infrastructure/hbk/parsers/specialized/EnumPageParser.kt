/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.specialized

import ru.alkoleft.context.infrastructure.hbk.exceptions.HandlerProcessingNotImplemented
import ru.alkoleft.context.infrastructure.hbk.exceptions.UnknownPageBlockType
import ru.alkoleft.context.infrastructure.hbk.models.EnumInfo
import ru.alkoleft.context.infrastructure.hbk.models.RelatedObject
import ru.alkoleft.context.infrastructure.hbk.parsers.core.BlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.DescriptionBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.ExampleBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.NameBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageProxyHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.RelatedObjectsBlockHandler

/**
 * Обработчик для парсинга страниц перечислений платформы 1С:Предприятие.
 *
 * Этот класс отвечает за извлечение и структурирование информации о перечислениях
 * из HTML страниц документации HBK. Он обрабатывает основные блоки страницы,
 * такие как описание, связанные объекты и примеры использования.
 *
 * Поддерживаемые блоки:
 * - Описание: подробное описание перечисления
 * - См. также: связанные объекты
 * - Пример: примеры использования перечисления
 *
 * Особенности:
 * - Извлечение названий на русском и английском языках
 * - Обработка описания функциональности
 * - Поддержка связанных объектов
 * - Обработка примеров использования
 *
 * @see PageProxyHandler для базовой функциональности
 * @see EnumInfo для структуры результата
 */
class EnumPageParseHandler : PageProxyHandler<EnumInfo>() {
    private var nameRu = ""
    private var nameEn = ""
    private var description = ""
    private var relatedObjects: List<RelatedObject>? = null
    private var example: String? = null

    override fun clean() {
        nameRu = ""
        nameEn = ""
        description = ""
        relatedObjects = null
    }

    override fun createHandler(blockTitle: String): BlockHandler<*>? =
        when (blockTitle) {
            "Описание:" -> DescriptionBlockHandler()
            "См. также:" -> RelatedObjectsBlockHandler()
            "Пример:" -> ExampleBlockHandler() // Placeholder, can be a specific handler
            "Значения", "Доступность:", "Использование в версии:", "Использование в интерфейсе:", "Свойства:" -> null
            else -> throw UnknownPageBlockType(blockTitle)
        }

    override fun onBlockFinished(handler: BlockHandler<*>) {
        when (handler) {
            is NameBlockHandler ->
                handler.getResult().apply {
                    nameRu = first
                    nameEn = second
                }

            is DescriptionBlockHandler -> description = handler.getResult()
            is RelatedObjectsBlockHandler -> relatedObjects = handler.getResult()
            is ExampleBlockHandler -> example = handler.getResult()
            else -> throw HandlerProcessingNotImplemented(handler)
        }
    }

    override fun getResult() =
        EnumInfo(
            nameRu = nameRu,
            nameEn = nameEn,
            description = description,
            relatedObjects = relatedObjects,
            example = example,
        )
}

/**
 * Парсер для страниц перечислений платформы 1С:Предприятие.
 *
 * Этот класс специализируется на парсинге HTML страниц документации,
 * содержащих информацию о перечислениях. Он извлекает структурированную
 * информацию о названии, описании, связанных объектах и примерах использования.
 *
 * Основные возможности:
 * - Извлечение названий на двух языках
 * - Обработка описания функциональности
 * - Поддержка связанных объектов
 * - Обработка примеров использования
 * - Структурированное представление информации о перечислении
 *
 * @see PageParser для базовой функциональности парсинга
 * @see EnumInfo для структуры результата
 * @see EnumPageParseHandler для обработки конкретных блоков
 */
class EnumPageParser : PageParser<EnumInfo>(EnumPageParseHandler())
