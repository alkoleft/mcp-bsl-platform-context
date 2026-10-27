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
import ru.alkoleft.context.business.entities.PropertyDefinition
import ru.alkoleft.context.infrastructure.persistent.storage.disambiguateTypeNames

/**
 * Одноимённые типы справки (ЭлементыФормы для обычной и управляемой формы) должны быть доступны оба.
 */
class DuplicateTypeNamesTest {
    private fun type(
        name: String,
        availability: String?,
        description: String = "Описание $name",
        tocName: String? = null,
        properties: List<PropertyDefinition> = emptyList(),
    ) = PlatformTypeDefinition(
        name = name,
        description = description,
        methods = emptyList(),
        properties = properties,
        constructors = emptyList(),
        status = ApiStatus(availability = availability),
        tocName = tocName,
    )

    private fun value(
        name: String,
        deprecated: Boolean,
    ) = PropertyDefinition(
        name = name,
        description = "",
        propertyType = "Перечисление",
        status = ApiStatus(deprecated = if (deprecated) "Не рекомендуется использовать, начиная с версии 8.3.23." else null),
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

    @Test
    fun `renamed enum copies get their table of contents name, current copy stays primary`() {
        val av = "Тонкий клиент, веб-клиент, толстый клиент."
        val old = type("СтандартнаяКомандаПоляПланировщика", av, tocName = "СтандартнаяКомандаПланировщика", properties = listOf(value("УдалитьЭлементы", true)))
        val current = type("СтандартнаяКомандаПоляПланировщика", av, tocName = "СтандартнаяКомандаПоляПланировщика", properties = listOf(value("УдалитьЭлементы", false)))

        val result = disambiguateTypeNames(listOf(old, current))

        assertEquals(listOf("СтандартнаяКомандаПланировщика", "СтандартнаяКомандаПоляПланировщика"), result.map { it.name })
        assertTrue(result[1].properties.none { it.status.isDeprecated })
        assertTrue(result[0].description.contains("страница справки общая с типом `СтандартнаяКомандаПоляПланировщика`"))
    }

    @Test
    fun `availability suffix wins over table of contents name when availability differs`() {
        val ordinary = type("Расширение элементов управления, расположенных в форме", "Толстый клиент.", tocName = "Расширение элементов формы")
        val managed = type("Расширение элементов управления, расположенных в форме", "Тонкий клиент, веб-клиент, толстый клиент.")

        val result = disambiguateTypeNames(listOf(ordinary, managed))

        assertEquals("Расширение элементов управления, расположенных в форме (Толстый клиент)", result[0].name)
    }
}
