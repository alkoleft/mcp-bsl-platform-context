/*
 * Copyright (c) 2024-2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.entities

/**
 * Доменная сущность, представляющая определение типа платформы.
 * Содержит информацию о типе API, методах и свойствах.
 */
data class PlatformTypeDefinition(
    override val name: String,
    override val description: String,
    val methods: List<MethodDefinition>,
    val properties: List<PropertyDefinition>,
    val constructors: List<Signature>,
    val status: ApiStatus = ApiStatus(),
) : Definition {
    /**
     * Проверяет, содержит ли тип методы
     */
    fun hasMethods(): Boolean = methods.isNotEmpty()

    /**
     * Проверяет, содержит ли тип свойства
     */
    fun hasProperties(): Boolean = properties.isNotEmpty()
}
