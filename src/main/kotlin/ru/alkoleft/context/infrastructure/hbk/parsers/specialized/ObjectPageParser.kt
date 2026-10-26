/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.specialized

import ru.alkoleft.context.business.entities.ApiStatus
import ru.alkoleft.context.infrastructure.hbk.exceptions.HandlerProcessingNotImplemented
import ru.alkoleft.context.infrastructure.hbk.models.ObjectInfo
import ru.alkoleft.context.infrastructure.hbk.models.RelatedObject
import ru.alkoleft.context.infrastructure.hbk.parsers.core.AvailabilityBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.BlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.DescriptionBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.ExampleBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.NameBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.NoteBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageProxyHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.RelatedObjectsBlockHandler

/**
 * Обработчик для парсинга страниц объектов платформы 1С:Предприятие.
 *
 * Этот класс отвечает за извлечение и структурирование информации об объектах
 * из HTML страниц документации HBK. Он обрабатывает основные блоки страницы,
 * такие как описание, примеры использования, связанные объекты и заметки.
 *
 * Поддерживаемые блоки:
 * - Описание: подробное описание объекта
 * - Пример: примеры использования объекта
 * - См. также: связанные объекты
 * - Примечание: дополнительные заметки об объекте
 *
 * Игнорируемые блоки:
 * - Свойства, Методы, События, Конструкторы: обрабатываются отдельными парсерами
 * - Доступность, Использование в версии: служебная информация
 *
 * Особенности:
 * - Извлечение названий на русском и английском языках
 * - Обработка описания функциональности
 * - Поддержка примеров использования
 * - Обработка связанных объектов и заметок
 *
 * @see PageProxyHandler для базовой функциональности
 * @see ObjectInfo для структуры результата
 */
class ObjectPageProxyHandler : PageProxyHandler<ObjectInfo>() {
    private var nameRu = ""
    private var nameEn = ""
    private var description = ""
    private var example: String? = null
    private var note: String? = null
    private var relatedObjects: List<RelatedObject>? = null
    private var status = ApiStatus()
    private var availability: String? = null

    override fun clean() {
        nameRu = ""
        nameEn = ""
        description = ""
        example = null
        note = null
        relatedObjects = null
        status = ApiStatus()
        availability = null
    }

    override fun createHandler(blockTitle: String): BlockHandler<*>? =
        when (blockTitle) {
            "Описание:" -> DescriptionBlockHandler()
            "Пример:" -> ExampleBlockHandler()
            "См. также:" -> RelatedObjectsBlockHandler()
            "Примечание:" -> NoteBlockHandler()
            "Доступность:" -> AvailabilityBlockHandler()
            "Свойства:", "Методы:", "События:", "Конструкторы:", "Использование в версии:" -> null // Игнорируем эти блоки
            else -> null // Игнорируем неизвестные блоки
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
            is ExampleBlockHandler -> example = handler.getResult()
            is RelatedObjectsBlockHandler -> relatedObjects = handler.getResult()
            is NoteBlockHandler -> note = handler.getResult()
            is AvailabilityBlockHandler -> availability = handler.getResult().ifBlank { null }
            else -> throw HandlerProcessingNotImplemented(handler)
        }
    }

    override fun getResult(): ObjectInfo =
        ObjectInfo(
            nameRu = nameRu,
            nameEn = nameEn,
            description = description,
            example = example,
            note = note,
            relatedObjects = relatedObjects,
            status = status.copy(availability = availability),
        )
}

/**
 * Парсер для страниц объектов платформы 1С:Предприятие.
 *
 * Этот класс специализируется на парсинге HTML страниц документации,
 * содержащих информацию об объектах. Он извлекает структурированную
 * информацию о названии, описании, примерах использования и связанных объектах.
 *
 * Основные возможности:
 * - Извлечение названий на двух языках
 * - Обработка описания функциональности объекта
 * - Поддержка примеров использования
 * - Обработка связанных объектов
 * - Поддержка дополнительных заметок
 * - Структурированное представление информации об объекте
 *
 * @see PageParser для базовой функциональности парсинга
 * @see ObjectInfo для структуры результата
 * @see ObjectPageProxyHandler для обработки конкретных блоков
 */
class ObjectPageParser : PageParser<ObjectInfo>(ObjectPageProxyHandler())
