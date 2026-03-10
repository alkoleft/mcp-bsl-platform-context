/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.specialized

import ru.alkoleft.context.infrastructure.hbk.exceptions.HandlerProcessingNotImplemented
import ru.alkoleft.context.infrastructure.hbk.exceptions.UnknownPageBlockType
import ru.alkoleft.context.infrastructure.hbk.models.PropertyInfo
import ru.alkoleft.context.infrastructure.hbk.models.RelatedObject
import ru.alkoleft.context.infrastructure.hbk.parsers.core.BlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.NameBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.NoteBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.core.PageProxyHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.ReadOnlyBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.RelatedObjectsBlockHandler
import ru.alkoleft.context.infrastructure.hbk.parsers.core.ValueInfoBlockHandler

/**
 * Обработчик для парсинга страниц свойств объектов платформы 1С:Предприятие.
 *
 * Этот класс отвечает за извлечение и структурирование информации о свойствах
 * из HTML страниц документации HBK. Он обрабатывает различные блоки страницы,
 * такие как описание, тип, флаг только для чтения, связанные объекты и заметки.
 *
 * Поддерживаемые блоки:
 * - Описание: подробное описание свойства и его типа
 * - Использование: информация о том, является ли свойство только для чтения
 * - См. также: связанные объекты
 * - Примечание: дополнительные заметки о свойстве
 *
 * Особенности:
 * - Извлечение названий на русском и английском языках
 * - Обработка типа свойства
 * - Определение флага только для чтения
 * - Обработка описания функциональности
 * - Поддержка связанных объектов и заметок
 *
 * @see PageProxyHandler для базовой функциональности
 * @see PropertyInfo для структуры результата
 */
class PropertyPageProxyHandler : PageProxyHandler<PropertyInfo>() {
    private var nameRu = ""
    private var nameEn = ""
    private var description = ""
    private var readonly = false
    private var typeNames = ""
    private var relatedObjects: List<RelatedObject>? = null
    private var note: String? = null

    override fun createHandler(blockTitle: String): BlockHandler<*>? =
        when (blockTitle) {
            "Описание:" -> ValueInfoBlockHandler()
            "Использование:" -> ReadOnlyBlockHandler()
            "См. также:" -> RelatedObjectsBlockHandler()
            "Примечание:" -> NoteBlockHandler()
            "Доступность:", "Использование в версии:", "Использование в интерфейсе:" -> null
            else -> throw UnknownPageBlockType(blockTitle)
        }

    override fun onBlockFinished(handler: BlockHandler<*>) {
        when (handler) {
            is NameBlockHandler ->
                handler.getResult().apply {
                    nameRu = first
                    nameEn = second
                }

            is ValueInfoBlockHandler ->
                handler.getResult()?.let { info ->
                    typeNames = info.type
                    description = info.description
                }

            is ReadOnlyBlockHandler -> readonly = handler.getResult()
            is RelatedObjectsBlockHandler -> relatedObjects = handler.getResult()
            is NoteBlockHandler -> note = handler.getResult()
            else -> throw HandlerProcessingNotImplemented(handler)
        }
    }

    override fun getResult(): PropertyInfo =
        PropertyInfo(
            nameRu = nameRu,
            nameEn = nameEn,
            description = description.trim(),
            readonly = readonly,
            typeName = typeNames,
            relatedObjects = relatedObjects,
            note = note,
        )

    override fun clean() {
        nameRu = ""
        nameEn = ""
        description = ""
        readonly = false
        typeNames = ""
        relatedObjects = null
        note = null
    }
}

/**
 * Парсер для страниц свойств объектов платформы 1С:Предприятие.
 *
 * Этот класс специализируется на парсинге HTML страниц документации,
 * содержащих информацию о свойствах объектов. Он извлекает структурированную
 * информацию о названии, типе, описании, флаге только для чтения и связанных объектах.
 *
 * Основные возможности:
 * - Извлечение названий на двух языках
 * - Обработка типа свойства
 * - Определение флага только для чтения
 * - Обработка описания функциональности
 * - Поддержка связанных объектов
 * - Обработка дополнительных заметок
 * - Структурированное представление информации о свойстве
 *
 * @see PageParser для базовой функциональности парсинга
 * @see PropertyInfo для структуры результата
 * @see PropertyPageProxyHandler для обработки конкретных блоков
 */
class PropertyPageParser : PageParser<PropertyInfo>(PropertyPageProxyHandler())
