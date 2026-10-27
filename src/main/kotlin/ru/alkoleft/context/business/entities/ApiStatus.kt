/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.entities

/**
 * Статус элемента API платформы: устаревание, рекомендуемая замена,
 * версия появления и доступность (клиент/сервер/...).
 *
 * Источник в справке HBK:
 * - `<div class="__DEPRECATED_SHOW_STYLE__">` — «Не рекомендуется использовать, начиная с версии …»
 *   и список «Рекомендуется использовать:»;
 * - `<div class="__SINCE_SHOW_STYLE__">` — «Доступен, начиная с версии …»;
 * - раздел «Доступность:».
 */
data class ApiStatus(
    val deprecated: String? = null,
    val replacements: List<String> = emptyList(),
    val since: String? = null,
    val availability: String? = null,
) {
    val isDeprecated: Boolean
        get() = deprecated != null

    /**
     * Элемент доступен только в толстом клиенте (обычные формы, обычное приложение, часть COM-объектов).
     * Определяется по первому предложению раздела «Доступность».
     */
    val isThickClientOnly: Boolean
        get() =
            availability
                ?.lineSequence()
                ?.firstOrNull()
                ?.substringBefore('.')
                ?.trim()
                ?.equals(THICK_CLIENT, ignoreCase = true) == true

    companion object {
        const val THICK_CLIENT = "Толстый клиент"
    }
}

/** Статус любого элемента API. */
val Definition.apiStatus: ApiStatus
    get() =
        when (this) {
            is MethodDefinition -> status
            is PropertyDefinition -> status
            is PlatformTypeDefinition -> status
        }
