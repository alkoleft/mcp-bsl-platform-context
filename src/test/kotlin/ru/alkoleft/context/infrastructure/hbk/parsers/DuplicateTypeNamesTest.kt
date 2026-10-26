/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.alkoleft.context.business.entities.ApiStatus
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.infrastructure.persistent.storage.disambiguateTypeNames

/**
 * Одноимённые типы справки (ЭлементыФормы для обычной и управляемой формы) должны быть доступны оба.
 */
class DuplicateTypeNamesTest {
    private fun type(
        name: String,
        availability: String?,
        description: String = "Описание $name",
    ) = PlatformTypeDefinition(
        name = name,
        description = description,
        methods = emptyList(),
        properties = emptyList(),
        constructors = emptyList(),
        status = ApiStatus(availability = availability),
    )

    @Test
    fun `narrower duplicate gets availability suffix, wider keeps name`() {
        val ordinary = type("ЭлементыФормы", "Толстый клиент.", "Обычная форма")
        val managed = type("ЭлементыФормы", "Тонкий клиент, веб-клиент, мобильный клиент, толстый клиент.", "Управляемая форма")
        val other = type("ТаблицаЗначений", "Сервер, толстый клиент.")

        val result = disambiguateTypeNames(listOf(ordinary, other, managed))

        assertEquals(listOf("ЭлементыФормы (Толстый клиент)", "ТаблицаЗначений", "ЭлементыФормы"), result.map { it.name })
        assertTrue(result[2].description.startsWith("Управляемая форма"))
        assertTrue(result[2].description.contains("`ЭлементыФормы (Толстый клиент)`"))
        assertTrue(result[0].description.contains("Одноимённый тип с более широкой доступностью: `ЭлементыФормы`"))
        assertEquals(other, result[1])
    }

    @Test
    fun `duplicates without availability get numbered suffix`() {
        val result = disambiguateTypeNames(listOf(type("Тип", null), type("Тип", null)))

        assertEquals(setOf("Тип", "Тип (вариант 2)"), result.map { it.name }.toSet())
    }
}
