/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.specialized

import ru.alkoleft.context.business.entities.ApiStatus
import ru.alkoleft.context.infrastructure.hbk.exceptions.HandlerProcessingNotImplemented
import ru.alkoleft.context.infrastructure.hbk.models.EnumValueInfo
import ru.alkoleft.context.infrastructure.hbk.models.RelatedObject
import ru.alkoleft.context.infrastructure.hbk.parsers.core.AvailabilityBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.BlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.DescriptionBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.NameBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageProxyHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.RelatedObjectsBlockHandler

/**
 * Обработчик для парсинга страниц значений перечислений платформы 1С:Предприятие.
 *
 * Этот класс отвечает за извлечение и структурирование информации о конкретных
 * значениях перечислений из HTML страниц документации HBK. Он обрабатывает
 * основные блоки страницы, такие как описание и связанные объекты.
 *
 * Поддерживаемые блоки:
 * - Описание: подробное описание значения перечисления
 * - См. также: связанные объекты
 *
 * Особенности:
 * - Извлечение названий на русском и английском языках
 * - Обработка описания функциональности значения
 * - Поддержка связанных объектов
 *
 * @see PageProxyHandler для базовой функциональности
 * @see EnumValueInfo для структуры результата
 */
class EnumValuePageParseHandler : PageProxyHandler<EnumValueInfo>() {
    private var nameRu = ""
    private var nameEn = ""
    private var description = ""
    private var relatedObjects: List<RelatedObject>? = null
    private var status = ApiStatus()
    private var availability: String? = null

    override fun clean() {
        nameRu = ""
        nameEn = ""
        description = ""
        relatedObjects = null
        status = ApiStatus()
        availability = null
    }

    override fun createHandler(blockTitle: String): BlockHandler<*>? =
        when (blockTitle) {
            "Описание:" -> DescriptionBlockHandler()
            "См. также:" -> RelatedObjectsBlockHandler()
            "Доступность:" -> AvailabilityBlockHandler()
            // "Использование в версии:" и прочие блоки не нужны
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
            is AvailabilityBlockHandler -> availability = handler.getResult().ifBlank { null }
            else -> throw HandlerProcessingNotImplemented(handler)
        }
    }

    override fun getResult() =
        EnumValueInfo(
            nameRu = nameRu,
            nameEn = nameEn,
            description = description,
            relatedObjects = relatedObjects,
            status = status.copy(availability = availability),
        )
}

/**
 * Парсер для страниц значений перечислений платформы 1С:Предприятие.
 *
 * Этот класс специализируется на парсинге HTML страниц документации,
 * содержащих информацию о конкретных значениях перечислений. Он извлекает
 * структурированную информацию о названии, описании и связанных объектах.
 *
 * Основные возможности:
 * - Извлечение названий на двух языках
 * - Обработка описания функциональности значения
 * - Поддержка связанных объектов
 * - Структурированное представление информации о значении перечисления
 *
 * @see PageParser для базовой функциональности парсинга
 * @see EnumValueInfo для структуры результата
 * @see EnumValuePageParseHandler для обработки конкретных блоков
 */
class EnumValuePageParser : PageParser<EnumValueInfo>(EnumValuePageParseHandler())
