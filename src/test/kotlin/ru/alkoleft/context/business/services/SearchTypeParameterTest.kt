/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.services

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.exceptions.InvalidSearchQueryException

/**
 * Параметр type инструмента search: «null» и «all» строкой — поиск без фильтра, а не ошибка.
 */
class SearchTypeParameterTest {
    private val service = ContextSearchService(mockk(relaxed = true))

    @Test
    fun `null and all as strings mean no filter`() {
        listOf(null, "", "null", "NULL", " all ", "any", "все").forEach {
            assertNull(service.getApiType(it), "type='$it'")
        }
    }

    @Test
    fun `known types still map`() {
        assertEquals(ApiType.METHOD, service.getApiType("method"))
        assertEquals(ApiType.PROPERTY, service.getApiType("property"))
        assertEquals(ApiType.TYPE, service.getApiType("type"))
    }

    @Test
    fun `unknown type is still an error`() {
        assertThrows<InvalidSearchQueryException> { service.getApiType("метод2") }
    }
}
