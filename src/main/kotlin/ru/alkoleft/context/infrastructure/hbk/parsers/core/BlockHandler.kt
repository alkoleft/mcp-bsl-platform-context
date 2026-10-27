/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.core

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import ru.alkoleft.context.business.entities.ApiStatus
import ru.alkoleft.context.infrastructure.hbk.models.MethodParameterInfo
import ru.alkoleft.context.infrastructure.hbk.models.RelatedObject
import ru.alkoleft.context.infrastructure.hbk.models.ValueInfo

private val PARAMETER_NAME_PATTERN = """<([^&]+)>\s*(?:\(([^)]+)\))?""".toRegex()
private val NAMES_PATTERN = """([^(]+)\s*\(([^)]+)\)""".toRegex()

/**
 * Базовый интерфейс для обработчиков блоков HTML документации.
 *
 * Этот интерфейс расширяет KsoupHtmlHandler и добавляет функциональность
 * для получения результата обработки и очистки состояния обработчика.
 *
 * @param R Тип результата обработки блока
 *
 * @see KsoupHtmlHandler для базовой функциональности обработки HTML
 */
interface BlockHandler<R> : KsoupHtmlHandler {
    /**
     * Получает результат обработки блока.
     *
     * @return Результат обработки
     */
    fun getResult(): R

    /**
     * Очищает состояние обработчика для повторного использования.
     */
    fun cleanState()
}

/**
 * Базовая реализация обработчика блоков.
 *
 * Предоставляет пустые реализации методов KsoupHtmlHandler,
 * которые могут быть переопределены в наследниках.
 *
 * @param R Тип результата обработки блока
 */
abstract class BaseBlockHandler<R> : BlockHandler<R> {
    override fun onOpenTag(
        name: String,
        attributes: Map<String, String>,
        isImplied: Boolean,
    ) {
    }

    override fun onCloseTag(
        name: String,
        isImplied: Boolean,
    ) {
    }
}

/**
 * Обработчик блока имени сущности.
 *
 * Извлекает название сущности из HTML блока.
 *
 * @return Пара (русское название, английское название)
 */
class NameBlockHandler : BlockHandler<Pair<String, String>> {
    private var title = StringBuilder()
    private var heading = StringBuilder()
    private var inHeading = false
    private var inTitle = false

    // Блоки статуса в шапке страницы (до первого раздела V8SH_chapter)
    private var statusSection = StatusSection.NONE
    private var statusDivDepth = 0
    private var inNotUsed = false
    private var inListItem = false
    private val deprecated = StringBuilder()
    private val since = StringBuilder()
    private val listItem = StringBuilder()
    private val replacements = mutableListOf<String>()

    override fun onOpenTag(
        name: String,
        attributes: Map<String, String>,
        isImplied: Boolean,
    ) {
        val tag = name.lowercase()
        if (tag == "div") {
            if (statusSection != StatusSection.NONE) {
                statusDivDepth++
            } else {
                statusSection =
                    when (attributes["class"]) {
                        DEPRECATED_CLASS -> StatusSection.DEPRECATED
                        SINCE_CLASS -> StatusSection.SINCE
                        else -> StatusSection.NONE
                    }
                statusDivDepth = if (statusSection != StatusSection.NONE) 1 else 0
            }
            return
        }
        if (statusSection != StatusSection.NONE) {
            when (tag) {
                "p" -> inNotUsed = (attributes["class"] == NOT_USED_CLASS)
                "li" -> {
                    inListItem = true
                    listItem.clear()
                }
            }
            return
        }
        if (tag == "p") {
            inHeading = (attributes["class"] == "V8SH_heading")
            inTitle = (attributes["class"] == "V8SH_title")
        }
    }

    override fun onCloseTag(
        name: String,
        isImplied: Boolean,
    ) {
        val tag = name.lowercase()
        if (statusSection != StatusSection.NONE) {
            when (tag) {
                "div" -> {
                    statusDivDepth--
                    if (statusDivDepth <= 0) {
                        statusSection = StatusSection.NONE
                        statusDivDepth = 0
                    }
                }
                "p" -> inNotUsed = false
                "li" -> {
                    if (inListItem && statusSection == StatusSection.DEPRECATED) {
                        listItem.toString().normalizeSpaces().takeIf { it.isNotEmpty() }?.let(replacements::add)
                    }
                    inListItem = false
                }
            }
            return
        }
        inHeading = false
    }

    override fun onText(text: String) {
        when (statusSection) {
            StatusSection.DEPRECATED ->
                when {
                    inListItem -> listItem.append(text)
                    inNotUsed -> deprecated.append(text)
                }

            StatusSection.SINCE -> if (inNotUsed) since.append(text)
            StatusSection.NONE ->
                if (inHeading) {
                    heading.append(text)
                } else if (inTitle) {
                    title.append(text)
                }
        }
    }

    /**
     * Статус элемента из шапки страницы: устаревание, замены, версия появления.
     * Раздел «Доступность:» обрабатывается отдельно [AvailabilityBlockHandler].
     */
    fun getStatus(): ApiStatus =
        ApiStatus(
            deprecated = deprecated.toString().normalizeSpaces().ifEmpty { null },
            replacements = replacements.toList(),
            since = since.toString().normalizeSpaces().ifEmpty { null },
        )

    override fun getResult(): Pair<String, String> = readName(if (heading.isNotEmpty()) heading.trim() else title.trim())

    override fun cleanState() {
        heading.clear()
        title.clear()
        inHeading = false
        inTitle = false
        statusSection = StatusSection.NONE
        statusDivDepth = 0
        inNotUsed = false
        inListItem = false
        deprecated.clear()
        since.clear()
        listItem.clear()
        replacements.clear()
    }

    private enum class StatusSection { NONE, DEPRECATED, SINCE }

    private companion object {
        const val DEPRECATED_CLASS = "__DEPRECATED_SHOW_STYLE__"
        const val SINCE_CLASS = "__SINCE_SHOW_STYLE__"
        const val NOT_USED_CLASS = "not_used"
    }

    fun readName(text: CharSequence): Pair<String, String> {
        if (text.isBlank()) {
            throw IllegalArgumentException("Имя страницы должно быть заполнено")
        }
        val match = NAMES_PATTERN.find(text)

        return if (match != null) {
            Pair(match.groupValues[1].trim(), match.groupValues[2].trim())
        } else {
            Pair(text.toString(), "")
        }
    }
}

/**
 * Обработчик блока синтаксиса метода.
 *
 * Извлекает синтаксис вызова метода из HTML блока.
 *
 * @return Строка с синтаксисом метода
 */
class SyntaxBlockHandler : BaseBlockHandler<String>() {
    private val syntax = StringBuilder()

    override fun onText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            syntax.append(text)
        }
    }

    override fun getResult(): String = syntax.toString().trim()

    override fun cleanState() {
        syntax.clear()
    }
}

/**
 * Обработчик блока параметров метода.
 *
 * Парсит список параметров метода, извлекая для каждого параметра:
 * - Название
 * - Тип
 * - Флаг необязательности
 * - Описание
 *
 * @return Список информации о параметрах метода
 */
class ParametersBlockHandler : MarkdownHtmlHandler<List<MethodParameterInfo>>() {
    private val parameters = mutableListOf<MethodParameterInfo>()

    // Состояние текущего параметра
    private var currentParameterName = ""
    private var currentParameterNameBufer = StringBuilder()
    private var currentParameterOptional = false
    private val currentParameterType = StringBuilder()
    private var inParameterType = false
    private var blockType = BlockType.NONE

    override fun onOpenTag(
        name: String,
        attributes: Map<String, String>,
        isImplied: Boolean,
    ) {
        when {
            name == "div" && attributes["class"] == "V8SH_rubric" -> {
                storeCurrentParameter()
                blockType = BlockType.NAME
            }
            blockType == BlockType.DESCRIPTION -> super.onOpenTag(name, attributes, isImplied)
        }
    }

    override fun onCloseTag(
        name: String,
        isImplied: Boolean,
    ) {
        when (blockType) {
            BlockType.NAME -> {
                PARAMETER_NAME_PATTERN.find(currentParameterNameBufer.toString())?.let {
                    currentParameterName = it.groupValues[1]
                    currentParameterOptional = it.groupValues.getOrNull(2) == "необязательный"
                }
                blockType = BlockType.NONE
            }

            BlockType.DESCRIPTION -> super.onCloseTag(name, isImplied)
            else -> {}
        }
    }

    override fun onText(text: String) {
        val trimmed = text.trim()
        when (blockType) {
            BlockType.NONE -> {
                if (isTypeBlock(trimmed)) {
                    blockType = BlockType.TYPE
                } else if (isTextTypeBlock(trimmed)) {
                    if (trimmed.endsWith(".")) {
                        currentParameterType.append(trimmed.substring(4, trimmed.length - 1).trim())
                        blockType = BlockType.DESCRIPTION
                    } else {
                        currentParameterType.append(trimmed.substring(4).trim())
                        blockType = BlockType.TYPE
                    }
                }
            }

            BlockType.NAME -> currentParameterNameBufer.append(trimmed)

            BlockType.TYPE -> {
                if (trimmed != ".") {
                    currentParameterType.append(trimmed)
                } else {
                    blockType = BlockType.DESCRIPTION
                }
            }

            BlockType.DESCRIPTION -> super.onText(text)
        }
    }

    override fun getResult(): List<MethodParameterInfo> {
        storeCurrentParameter()
        return parameters.toList()
    }

    override fun cleanState() {
        parameters.clear()
        cleanParameterState()
    }

    private fun storeCurrentParameter() {
        if (currentParameterName.isNotEmpty()) {
            parameters.add(
                MethodParameterInfo(
                    name = currentParameterName,
                    type = currentParameterType.toString().trim(),
                    isOptional = currentParameterOptional,
                    description = getMarkdown(),
                ),
            )
            cleanParameterState()
        }
    }

    private fun cleanParameterState() {
        currentParameterName = ""
        currentParameterNameBufer.clear()
        currentParameterOptional = false
        currentParameterType.clear()
        inParameterType = false
        blockType = BlockType.NONE
        super.cleanState()
    }

    /**
     * Перечисление типов блоков при парсинге параметров метода.
     *
     * Определяет текущее состояние обработки параметра:
     * - NONE: начальное состояние
     * - NAME: обработка имени параметра
     * - TYPE: обработка типа параметра
     * - DESCRIPTION: обработка описания параметра
     */
    enum class BlockType {
        NONE,
        NAME,
        TYPE,
        DESCRIPTION,
    }
}

/**
 * Обработчик блока возвращаемого значения.
 *
 * Парсит информацию о возвращаемом значении метода или типе свойства, извлекая:
 * - Тип возвращаемого значения (для методов) или тип данных (для свойств)
 * - Описание возвращаемого значения или свойства
 *
 * Обрабатывает структурированный контент, начинающийся с "Тип:"
 * и заканчивающийся точкой, после которой следует описание.
 *
 * @return Информация о возвращаемом значении или null, если информация отсутствует
 */
class ValueInfoBlockHandler : MarkdownHtmlHandler<ValueInfo?>() {
    private var returnValue: ValueInfo? = null

    // Состояние текущего возвращаемого значения
    private val currentValueType = StringBuilder()
    private var blockType = BlockType.NONE

    override fun onOpenTag(
        name: String,
        attributes: Map<String, String>,
        isImplied: Boolean,
    ) {
        if (blockType == BlockType.DESCRIPTION) {
            super.onOpenTag(name, attributes, isImplied)
        }
    }

    override fun onCloseTag(
        name: String,
        isImplied: Boolean,
    ) {
        if (blockType == BlockType.DESCRIPTION) {
            super.onCloseTag(name, isImplied)
        }
    }

    override fun onText(text: String) {
        val trimmed = text.trim()
        when (blockType) {
            BlockType.NONE -> {
                if (isTypeBlock(trimmed)) {
                    blockType = BlockType.TYPE
                } else if (isTextTypeBlock(trimmed)) {
                    if (trimmed.endsWith(".")) {
                        currentValueType.append(trimmed.substring(4, trimmed.length - 1).trim())
                        blockType = BlockType.DESCRIPTION
                    } else {
                        currentValueType.append(trimmed.substring(4).trim())
                        blockType = BlockType.TYPE
                    }
                }
            }

            BlockType.TYPE -> {
                if (trimmed != ".") {
                    currentValueType.append(trimmed)
                } else {
                    blockType = BlockType.DESCRIPTION
                }
            }

            BlockType.DESCRIPTION -> super.onText(text)
        }
    }

    override fun getResult(): ValueInfo? {
        if (returnValue == null && currentValueType.isNotEmpty()) {
            returnValue =
                ValueInfo(
                    currentValueType.toString().trim(),
                    getMarkdown(),
                )
        }
        return returnValue
    }

    override fun cleanState() {
        super.cleanState()
        returnValue = null
        currentValueType.clear()
        blockType = BlockType.NONE
    }

    /**
     * Перечисление типов блоков при парсинге возвращаемого значения.
     *
     * Определяет текущее состояние обработки:
     * - NONE: начальное состояние
     * - TYPE: обработка типа возвращаемого значения
     * - DESCRIPTION: обработка описания возвращаемого значения
     */
    enum class BlockType {
        NONE,
        TYPE,
        DESCRIPTION,
    }
}

/**
 * Обработчик блока описания.
 *
 * Извлекает описание сущности из HTML блока, преобразуя его в Markdown формат.
 * Использует базовую функциональность MarkdownHtmlHandler для обработки HTML разметки.
 *
 * @return Описание в формате Markdown
 */
open class DescriptionBlockHandler : MarkdownHtmlHandler<String>() {
    override fun getResult() = getMarkdown()
}

class SignatureDescriptionBlockHandler : DescriptionBlockHandler()

/**
 * Обработчик блока примечаний.
 *
 * Извлекает примечания из HTML блока, преобразуя их в Markdown формат.
 * Используется для обработки дополнительной информации о сущности.
 *
 * @return Примечания в формате Markdown
 */
class NoteBlockHandler : MarkdownHtmlHandler<String>() {
    override fun getResult() = getMarkdown()
}

/**
 * Обработчик раздела «Доступность:» (тонкий клиент, сервер, ... и пометки «Внимание:»).
 */
class AvailabilityBlockHandler : MarkdownHtmlHandler<String>() {
    override fun getResult() = getMarkdown()
}

private fun String.normalizeSpaces() = replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()

/**
 * Обработчик блока примеров.
 *
 * Извлекает примеры использования из HTML блока, сохраняя структуру
 * с переносами строк.
 *
 * @return Пример использования в текстовом формате
 */
class ExampleBlockHandler : BaseBlockHandler<String>() {
    private val description = StringBuilder()

    override fun onOpenTag(
        name: String,
        attributes: Map<String, String>,
        isImplied: Boolean,
    ) {
        if (name == "br" || name == "BR") {
            description.appendLine()
        }
    }

    override fun onText(text: String) {
        description.append(text)
    }

    override fun getResult(): String = description.toString().trim().replace("\u00a0", " ")

    override fun cleanState() {
        description.clear()
    }
}

/**
 * Обработчик блока связанных объектов.
 *
 * Извлекает список связанных объектов из HTML блока, содержащего ссылки.
 * Обрабатывает теги <a> и извлекает текст ссылки и href атрибут.
 *
 * @return Список связанных объектов с названием и ссылкой
 */
class RelatedObjectsBlockHandler : BaseBlockHandler<List<RelatedObject>>() {
    private val relatedObjects = mutableListOf<RelatedObject>()
    private var linkText = StringBuilder()
    private var href: String? = ""

    override fun onOpenTag(
        name: String,
        attributes: Map<String, String>,
        isImplied: Boolean,
    ) {
        linkText.clear()
        href = attributes["href"]
    }

    override fun onCloseTag(
        name: String,
        isImplied: Boolean,
    ) {
        if (name == "a" && linkText.isNotEmpty()) {
            if (href != null) {
                val text =
                    linkText
                        .toString()
                        .trim()
                        .replace(" ,", ",")
                        .replace(Regex("\\s{2,}"), " ")

                relatedObjects += RelatedObject(text, href!!)
            } else {
                throw IllegalArgumentException("Link href is empty for `$linkText`")
            }
            href = null
            linkText.clear()
        }
    }

    override fun onText(text: String) {
        linkText.append(text)
    }

    override fun getResult(): List<RelatedObject> = relatedObjects

    override fun cleanState() {
        relatedObjects.clear()
        linkText.clear()
        href = null
    }
}

/**
 * Обработчик блока флага "только чтение".
 *
 * Определяет, является ли сущность доступной только для чтения,
 * анализируя текстовый контент на наличие фразы "Только чтение".
 *
 * @return true, если сущность доступна только для чтения, false в противном случае
 */
class ReadOnlyBlockHandler : BaseBlockHandler<Boolean>() {
    private var value = false

    override fun onText(text: String) {
        value = text.startsWith("Только чтение")
    }

    override fun getResult() = value

    override fun cleanState() {
        value = false
    }
}

fun isTextTypeBlock(text: String) = text != "Тип:" && text.startsWith("Тип:")

fun isTypeBlock(text: String) = text == "Тип:"
