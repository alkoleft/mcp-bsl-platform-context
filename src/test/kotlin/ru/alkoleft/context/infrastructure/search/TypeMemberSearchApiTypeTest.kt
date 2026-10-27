/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.alkoleft.context.business.entities.MethodDefinition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.entities.PropertyDefinition
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.infrastructure.search.indexes.Indexes
import ru.alkoleft.context.infrastructure.search.indexes.StartWithIndex

/**
 * Поиск «Тип Член» учитывает фильтр по виду элемента (параметр type инструмента search).
 */
class TypeMemberSearchApiTypeTest {
    private val form =
        PlatformTypeDefinition(
            name = "ФормаКлиентскогоПриложения",
            description = "",
            methods = listOf(MethodDefinition("ОбновитьОтображениеДанных", "", "", emptyList())),
            properties = listOf(PropertyDefinition("Открыта", "", "Булево")),
            constructors = emptyList(),
            events = listOf(MethodDefinition("ПриОткрытии", "", "", emptyList(), isEvent = true)),
        )

    private val search =
        TypeMemberSearch(
            Indexes(properties = StartWithIndex(), methods = StartWithIndex(), types = StartWithIndex())
                .also {
                    it.types.load(listOf(form), PlatformTypeDefinition::name)
                    it.methods.load(emptyList(), MethodDefinition::name)
                    it.properties.load(emptyList(), PropertyDefinition::name)
                },
        )

    private fun names(apiType: ApiType?) = search.search("ФормаКлиентскогоПриложения о", apiType).map { it.item.name }.toSet()

    @Test
    fun `without filter methods, properties and events are found`() {
        assertEquals(setOf("ОбновитьОтображениеДанных", "Открыта", "ПриОткрытии"), names(null))
    }

    @Test
    fun `method filter returns only methods`() {
        assertEquals(setOf("ОбновитьОтображениеДанных"), names(ApiType.METHOD))
    }

    @Test
    fun `property filter returns only properties`() {
        assertEquals(setOf("Открыта"), names(ApiType.PROPERTY))
    }

    @Test
    fun `type filter returns no members`() {
        assertEquals(emptySet<String>(), names(ApiType.TYPE))
    }
}
