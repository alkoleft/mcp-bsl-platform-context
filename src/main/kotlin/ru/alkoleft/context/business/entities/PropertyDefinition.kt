/*
 * Copyright (c) 2024-2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.entities

/**
 * Доменная сущность, представляющая определение свойства.
 * Содержит информацию о свойстве, его типе и сигнатуре.
 */
data class PropertyDefinition(
    override val name: String,
    override val description: String,
    val propertyType: String,
    val isReadOnly: Boolean = false,
    val status: ApiStatus = ApiStatus(),
    /** Раздел «Примечание:» страницы справки. */
    val note: String = "",
) : Definition
