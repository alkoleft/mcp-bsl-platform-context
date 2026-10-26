/*
 * Copyright (c) 2024-2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.presentation.formatters

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.alkoleft.context.business.entities.ApiStatus
import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.entities.MethodDefinition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.entities.PropertyDefinition
import ru.alkoleft.context.business.entities.Signature
import ru.alkoleft.context.business.entities.apiStatus
import ru.alkoleft.context.business.services.ResponseFormatterService
import ru.alkoleft.context.exceptions.DomainException

/**
 * Сервис форматирования в Markdown
 *
 * Форматирует результаты поиска и детальную информацию в читаемый Markdown
 */
@Service
class MarkdownFormatterService : ResponseFormatterService {
    private val logger = KotlinLogging.logger {}

    /**
     * Форматирует сообщение об ошибке для вывода в Markdown.
     *
     * @param e Исключение, которое требуется отформатировать.
     * @return Строка с сообщением об ошибке в формате Markdown.
     */
    override fun formatError(e: Throwable) = if (e is DomainException) "❌ ${e.message}" else "❌ **Ошибка:** ${e.message}"

    /**
     * Форматирует поисковый запрос для вывода в Markdown.
     *
     * @param query Строка поискового запроса.
     * @return Строка с заголовком для результатов поиска.
     */
    override fun formatQuery(query: String) = "# Результаты поиска: '$query'\n\n"

    /**
     * Форматирует результаты поиска в кратком виде (summary).
     * Выводит только основные сведения о найденных методах, свойствах и типах.
     * Используется для отображения списка результатов поиска.
     *
     * @param result Список найденных определений.
     * @return Строка с кратким описанием результатов поиска.
     */
    override fun formatSearchResults(result: List<Definition>): String {
        logger.debug { "formatSearchResults called with result.size=${result.size}" }

        if (result.isEmpty()) {
            return "❌ **Не найдено:** Ничего не найдено для запроса\n"
        }

        // Если найден только один результат, выводим его полное описание
        if (result.size == 1) {
            return formatMember(result.first())
        }

        // Если результатов несколько, выводим краткий список
        return buildString {
            appendLine("## Найдено ${result.size} элементов")
            appendLine()

            result.forEach { definition ->
                val typeLabel =
                    when (definition) {
                        is MethodDefinition -> if (definition.isEvent) "Event (${definition.ownerType})" else "Method"
                        is PropertyDefinition -> "Property"
                        is PlatformTypeDefinition -> if (definition.isEnum) "Enum" else "Type"
                    }

                val description = definition.description.takeIf { it.isNotBlank() } ?: "Нет описания"

                appendLine("### ${definition.name}")
                appendLine("**Тип элемента:** $typeLabel")
                definition.deprecatedStatus()?.let { append(formatDeprecation(it, compact = true)) }
                if (definition.apiStatus.isThickClientOnly) {
                    appendLine("**⚠️ Только толстый клиент** (обычные формы / обычное приложение)")
                }
                appendLine("**Описание:** $description")
                appendLine()
            }
        }
    }

    /**
     * Форматирует подробную информацию об одном найденном элементе.
     *
     * @param definition Определение элемента (тип, метод или свойство).
     * @return Строка с подробным описанием элемента в формате Markdown.
     */
    override fun formatMember(definition: Definition?): String {
        logger.debug { "formatInfo called with definition=$definition" }
        return when (definition) {
            is PlatformTypeDefinition -> formatPlatformType(definition)
            is PropertyDefinition -> formatProperty(definition)
            is MethodDefinition -> formatMethod(definition)
            null -> "❌ **Не найдено:** Ничего не найдено для запроса\n"
        }
    }

    /**
     * Форматирует полное описание найденных элементов.
     * Включает подробную информацию о каждом методе, свойстве и типе.
     * Используется для отображения детальной информации по каждому найденному элементу.
     *
     * @param definitions Список найденных определений.
     * @return Строка с полным описанием всех найденных элементов.
     */
    override fun formatTypeMembers(definitions: List<Definition>): String {
        logger.debug { "formatDefinitions called with definitions.size=${definitions.size}" }
        if (definitions.isEmpty()) {
            return ("❌ **Не найдено:** Ничего не найдено для запроса\n")
        }
        val methods = definitions.filterIsInstance<MethodDefinition>().filterNot { it.isEvent }
        val events = definitions.filterIsInstance<MethodDefinition>().filter { it.isEvent }
        val properties = definitions.filterIsInstance<PropertyDefinition>()
        val types = definitions.filterIsInstance<PlatformTypeDefinition>()
        return buildString {
            // Методы
            if (methods.isNotEmpty()) {
                appendLine("## Методы\n")
                methods.forEach { method ->
                    appendLine(formatMethod(method))
                }
            }

            // Свойства
            if (properties.isNotEmpty()) {
                appendLine("## Свойства\n")
                properties.forEach { property ->
                    appendLine(formatProperty(property))
                }
            }

            // События
            if (events.isNotEmpty()) {
                appendLine("## События\n")
                events.forEach { event ->
                    appendLine(formatMethod(event))
                }
            }
        }
    }

    /**
     * Форматирует список конструкторов для типа.
     *
     * @param result Список сигнатур конструкторов.
     * @return Строка с описанием конструкторов в формате Markdown.
     */
    override fun formatConstructors(
        result: List<Signature>,
        typeName: String,
    ): String {
        logger.debug { "formatConstructors called with result.size=${result.size}" }
        return buildString {
            appendLine("Конструкторы объекта $typeName")
            result.forEach {
                appendLine("## Конструктор: ${it.name} (${it.description})")
                append(formatDeprecation(it.status))
                appendLine(formatSignature(it, "Новый $typeName"))
            }
        }
    }

    /**
     * Форматирует тип платформы в Markdown
     */
    private fun formatPlatformType(type: PlatformTypeDefinition): String =
        buildString {
            appendLine("# ${type.name}\n")
            append(formatDeprecation(type.status))

            if (type.description.isNotBlank()) {
                appendLine("${type.description}\n")
            }

            // Методы
            if (type.hasMethods()) {
                appendLine("## Методы\n")
                type.methods.forEach {
                    appendLine(formatMethodSummary(it))
                }
            }

            // Свойства
            if (type.hasProperties()) {
                appendLine(if (type.isEnum) "## Значения\n" else "## Свойства\n")
                type.properties.forEach {
                    appendLine(formatPropertySummary(it))
                }
            }
            // События
            if (type.events.isNotEmpty()) {
                appendLine("## События\n")
                type.events.forEach {
                    appendLine(formatMethodSummary(it))
                }
            }

            // Конструкторы
            if (!type.constructors.isEmpty()) {
                appendLine("## Конструкторы")
                type.constructors.forEach {
                    appendLine(formatConstructorSummary(it))
                }
            }

            append(formatStatusDetails(type.status))
        }

    /**
     * Форматирует метод в Markdown (публичный метод)
     */
    private fun formatMethod(method: MethodDefinition): String =
        buildString {
            if (method.isEvent) {
                appendLine("### Событие ${method.ownerType?.let { "$it." } ?: ""}${method.name}\n")
            } else {
                appendLine("### ${method.name}\n")
            }
            append(formatDeprecation(method.status))

            if (method.description.isNotBlank()) {
                appendLine("${method.description}\n")
            }

            appendLine(formatSignatures(method.signature, method.name, showRequired = !method.isEvent))

            if (method.returnType.isNotBlank()) {
                appendLine("**Возвращаемый тип:** `${method.returnType}`")
                if (method.returnDescription.isNotBlank()) {
                    appendLine(method.returnDescription)
                }
                appendLine()
            }

            append(formatStatusDetails(method.status))
        }

    /**
     * Форматирует свойство в Markdown (публичный метод)
     */
    private fun formatProperty(property: PropertyDefinition): String =
        buildString {
            appendLine("### ${property.name}\n")
            append(formatDeprecation(property.status))

            if (property.description.isNotBlank()) {
                appendLine("${property.description}\n")
            }

            appendLine("**Тип:** `${property.propertyType}`")
            appendLine("**Только для чтения:** ${if (property.isReadOnly) "Да" else "Нет"}\n")

            append(formatStatusDetails(property.status))
        }

    private fun formatSignatures(
        signatures: List<Signature>,
        name: String,
        showRequired: Boolean = true,
    ) = buildString {
        signatures.forEach { signature ->
            appendLine("## Сигнатура: ${signature.name} (${signature.description})")
            appendLine(formatSignature(signature, name, showRequired))
        }
    }

    private fun formatSignature(
        signature: Signature,
        name: String,
        showRequired: Boolean = true,
    ) = buildString {
        appendLine("```bsl")
        appendLine("$name(${signature.format()})")
        appendLine("```\n")

        // Параметры
        if (signature.parameters.isNotEmpty()) {
            appendLine("### Параметры")
            signature.parameters.forEach { param ->
                // У параметров событий в справке нет признака обязательности
                val requiredMark = if (showRequired && param.required) "(обязательный)" else ""
                val description = param.description
                appendLine("- **${param.name}** *(${param.type})* $requiredMark - $description")
            }
            appendLine()
        }
    }

    /**
     * Форматирует краткую информацию о методе
     */
    private fun formatMethodSummary(method: MethodDefinition): String {
        val returnType = if (method.returnType.isNotBlank()) ": ${method.returnType}" else ""

        val mark = deprecatedMark(method.status)
        return if (method.signature.isEmpty()) {
            "- $mark${method.name}()$returnType - ${method.description}"
        } else {
            buildString {
                method.signature.forEach {
                    appendLine("- $mark${method.name}(${it.format()})$returnType - ${it.description}")
                }
            }
        }
    }

    /**
     * Форматирует краткую информацию о свойстве
     */
    private fun formatPropertySummary(property: PropertyDefinition): String =
        "- ${deprecatedMark(property.status)}${property.name}: ${property.propertyType} - ${property.description}"

    /**
     * Форматирует краткую информацию о свойстве
     */
    private fun formatConstructorSummary(constructor: Signature): String =
        "- ${deprecatedMark(constructor.status)}${constructor.name} - ${constructor.description}"

    /**
     * Предупреждение об устаревании и рекомендуемая замена (выводится сразу под заголовком).
     */
    private fun formatDeprecation(
        status: ApiStatus,
        compact: Boolean = false,
    ): String {
        if (!status.isDeprecated) return ""
        val replacement =
            status.replacements
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { "`$it`" }
        return buildString {
            if (compact) {
                append("**⚠️ ${status.deprecated}**")
                replacement?.let { append(" Рекомендуется использовать: $it") }
                appendLine()
            } else {
                appendLine("> ⚠️ **${status.deprecated}**")
                replacement?.let { appendLine("> **Рекомендуется использовать:** $it") }
                appendLine()
            }
        }
    }

    /**
     * Версия появления и доступность (выводятся в конце описания элемента).
     */
    private fun formatStatusDetails(status: ApiStatus): String =
        buildString {
            status.since?.let { appendLine("**Версия:** $it") }
            status.availability?.let { appendLine("**Доступность:** $it") }
            if (isNotEmpty()) appendLine()
        }

    private fun deprecatedMark(status: ApiStatus) = if (status.isDeprecated) "⚠️ (не рекомендуется) " else ""

    private fun Definition.deprecatedStatus(): ApiStatus? =
        when (this) {
            is MethodDefinition -> status
            is PropertyDefinition -> status
            is PlatformTypeDefinition -> status
        }.takeIf { it.isDeprecated }
}

fun Signature.format() =
    parameters.joinToString(", ") { param ->
        "${param.name}${if (!param.required) "?" else ""}: ${param.type}"
    }
