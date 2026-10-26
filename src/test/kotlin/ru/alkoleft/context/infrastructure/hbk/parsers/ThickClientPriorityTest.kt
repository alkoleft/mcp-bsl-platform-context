/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.alkoleft.context.business.entities.ApiStatus

/**
 * Признак «только толстый клиент» — основа понижения обычных форм в поиске.
 */
class ThickClientPriorityTest {
    @Test
    fun `thick client only is detected by first sentence of availability`() {
        assertTrue(ApiStatus(availability = "Толстый клиент.").isThickClientOnly)
        assertTrue(ApiStatus(availability = "Толстый клиент.\n\n**Внимание:** только для Windows").isThickClientOnly)
        assertFalse(ApiStatus(availability = "Тонкий клиент, веб-клиент, толстый клиент.").isThickClientOnly)
        assertFalse(ApiStatus(availability = "Сервер, толстый клиент, внешнее соединение.").isThickClientOnly)
        assertFalse(ApiStatus().isThickClientOnly)
    }

    @Test
    fun `thick client only items sort after others within same priority`() {
        data class R(val name: String, val priority: Int, val status: ApiStatus)
        val items =
            listOf(
                R("ПолеHTMLДокумента", 3, ApiStatus(availability = "Толстый клиент.")),
                R("ПолеФормы", 3, ApiStatus(availability = "Тонкий клиент, веб-клиент, толстый клиент.")),
                R("Точное", 1, ApiStatus(availability = "Толстый клиент.")),
            )
        val sorted = items.sortedWith(compareBy<R> { it.priority }.thenBy { if (it.status.isThickClientOnly) 1 else 0 })
        assertEquals(listOf("Точное", "ПолеФормы", "ПолеHTMLДокумента"), sorted.map { it.name })
    }
}
