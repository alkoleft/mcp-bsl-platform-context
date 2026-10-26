/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.specialized

import ru.alkoleft.context.business.entities.ApiStatus
import ru.alkoleft.context.infrastructure.hbk.exceptions.HandlerProcessingNotImplemented
import ru.alkoleft.context.infrastructure.hbk.models.EnumInfo
import ru.alkoleft.context.infrastructure.hbk.models.RelatedObject
import ru.alkoleft.context.infrastructure.hbk.parsers.core.AvailabilityBlockHandler
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
    private var status = ApiStatus()
    private var availability: String? = null

    override fun clean() {
        nameRu = ""
        nameEn = ""
        description = ""
        relatedObjects = null
        example = null
        status = ApiStatus()
        availability = null
    }

    override fun createHandler(blockTitle: String): BlockHandler<*>? =
        when (blockTitle) {
            "Описание:" -> DescriptionBlockHandler()
            "См. также:" -> RelatedObjectsBlockHandler()
            "Пример:" -> ExampleBlockHandler() // Placeholder, can be a specific handler
            "Доступность:" -> AvailabilityBlockHandler()
            // "Значения", "Использование в версии:", "Свойства:" и прочие блоки не нужны
            else -> null
        }

    override fun onBlockFinished(handler: BlockHandler<*>) {
        when (handler) {
            is NameBlockHandler ->
                handler.getResult().apply {
                    nameRu = first
                    nameEn = second
                    status = handler.getStatus()
                }

            is DescriptionBlockHandler -> description = handler.getResult()
            is RelatedObjectsBlockHandler -> relatedObjects = handler.getResult()
            is ExampleBlockHandler -> example = handler.getResult()
            is AvailabilityBlockHandler -> availability = handler.getResult().ifBlank { null }
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
            status = status.copy(availability = availability),
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
