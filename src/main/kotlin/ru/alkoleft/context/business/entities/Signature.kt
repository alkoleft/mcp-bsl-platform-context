/*
 * Copyright (c) 2024-2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.entities

/**
 * Value Object: Сигнатура метода или свойства
 */
data class Signature(
    val name: String,
    val parameters: List<ParameterDefinition>,
    val description: String,
    val status: ApiStatus = ApiStatus(),
)
