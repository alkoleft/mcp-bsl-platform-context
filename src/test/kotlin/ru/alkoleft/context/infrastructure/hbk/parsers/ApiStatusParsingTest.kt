/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.ConstructorPageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.EnumPageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.EnumValuePageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.MethodPageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.PropertyPageParser

/**
 * Разбор статуса элемента API: «Не рекомендуется использовать…», замены, версия, доступность.
 * HTML — упрощённая копия структуры страниц shcntx_ru.hbk (8.3.27).
 */
class ApiStatusParsingTest {
    private val deprecatedMethodPage =
        """
        <html><head></head><body>
        <h1 class="V8SH_pagetitle">Глобальный контекст.ТестМетод (Global context.TestMethod)</h1>
        <p class="V8SH_title">Глобальный контекст (Global context)</p>
        <p class="V8SH_heading">ТестМетод (TestMethod)</p>
        <div class="__DEPRECATED_SHOW_STYLE__"><p class="not_used">Не рекомендуется использовать, начиная с версии 8.3.13.</p>
        <p class="__DEPRECATED_SHOW_STYLE__">Рекомендуется использовать:</p>
        <ul class="__DEPRECATED_SHOW_STYLE__"><li><a href="v8help://x/A.html">НовыйМетодА</a></li><li><a href="v8help://x/B.html">НовыйМетодБ</a></li></ul></div>
        <div class="__SINCE_SHOW_STYLE__"><p class="not_used">Доступен, начиная с версии 8.2.</p></div>
        <p class="V8SH_chapter">Синтаксис:</p>ТестМетод(&lt;Адрес&gt;)
        <p class="V8SH_chapter">Описание:</p>Описание метода.
        <p class="V8SH_chapter">Доступность: </p><p>Тонкий клиент, сервер.</p>
        <p class="V8SH_chapter">Использование в версии:</p><p>Доступен, начиная с версии 8.2.</p>
        </body></html>
        """.trimIndent()

    private val deprecatedPropertyPage =
        """
        <html><head></head><body>
        <h1 class="V8SH_pagetitle">МетодСжатияZIP.Сжатие (ZIPCompressionMethod.Deflate)</h1>
        <p class="V8SH_title">МетодСжатияZIP (ZIPCompressionMethod)</p>
        <p class="V8SH_heading">Сжатие (Deflate)</p>
        <div class="__DEPRECATED_SHOW_STYLE__"><p class="not_used">Не рекомендуется использовать, начиная с версии 8.3.26.</p></div>
        <div class="__SINCE_SHOW_STYLE__"><p class="not_used">Доступен, начиная с версии 8.0.</p></div>
        <p class="V8SH_chapter">Описание:</p>Описание свойства.
        </body></html>
        """.trimIndent()

    private val regularMethodPage =
        """
        <html><head></head><body>
        <p class="V8SH_title">Глобальный контекст (Global context)</p>
        <p class="V8SH_heading">СтрТест (StrTest)</p>
        <div class="__SINCE_SHOW_STYLE__"><p class="not_used">Доступен, начиная с версии 8.0.</p></div>
        <p class="V8SH_chapter">Синтаксис:</p>СтрТест()
        <p class="V8SH_chapter">Описание:</p>Описание.
        </body></html>
        """.trimIndent()

    @Test
    fun `deprecated method has status, replacements, since and availability`() {
        val result = MethodPageParser().parse(deprecatedMethodPage.byteInputStream())

        assertEquals("ТестМетод", result.nameRu)
        assertEquals("TestMethod", result.nameEn)
        assertTrue(result.status.isDeprecated)
        assertEquals("Не рекомендуется использовать, начиная с версии 8.3.13.", result.status.deprecated)
        assertEquals(listOf("НовыйМетодА", "НовыйМетодБ"), result.status.replacements)
        assertEquals("Доступен, начиная с версии 8.2.", result.status.since)
        assertEquals("Тонкий клиент, сервер.", result.status.availability)
    }

    @Test
    fun `deprecated property without replacements`() {
        val result = PropertyPageParser().parse(deprecatedPropertyPage.byteInputStream())

        assertEquals("Сжатие", result.nameRu)
        assertEquals("Не рекомендуется использовать, начиная с версии 8.3.26.", result.status.deprecated)
        assertTrue(result.status.replacements.isEmpty())
        assertEquals("Доступен, начиная с версии 8.0.", result.status.since)
    }

    @Test
    fun `regular method is not deprecated and parser state is reset between pages`() {
        val parser = MethodPageParser()
        parser.parse(deprecatedMethodPage.byteInputStream())
        val result = parser.parse(regularMethodPage.byteInputStream())

        assertEquals("СтрТест", result.nameRu)
        assertFalse(result.status.isDeprecated)
        assertTrue(result.status.replacements.isEmpty())
        assertEquals("Доступен, начиная с версии 8.0.", result.status.since)
        assertNull(result.status.availability)
    }

    private val enumPage =
        """
        <html><head></head><body>
        <p class="V8SH_heading">МетодСжатияФайлаАрхива (ArchiveFileCompressionMethod)</p>
        <p class="V8SH_chapter">Значения</p><p>BZIP2 (BZIP2)</p>
        <p class="V8SH_chapter">Описание:</p>Содержит варианты методов сжатия записываемого файла архива.
        <p class="V8SH_chapter">Доступность:</p><p>Сервер, толстый клиент, внешнее соединение.</p>
        <p class="V8SH_chapter">Использование в версии:</p><p>Доступен, начиная с версии 8.3.26.</p>
        <p class="V8SH_chapter">Неизвестный блок:</p><p>не должен ронять разбор</p>
        </body></html>
        """.trimIndent()

    private val deprecatedEnumValuePage =
        """
        <html><head></head><body>
        <p class="V8SH_title">МетодСжатияZIP (ZIPCompressionMethod)</p>
        <p class="V8SH_heading">Сжатие (Deflate)</p>
        <div class="__DEPRECATED_SHOW_STYLE__"><p class="not_used">Не рекомендуется использовать, начиная с версии 8.3.26.</p></div>
        <p class="V8SH_chapter">Описание:</p>Метод сжатия Deflate.
        <p class="V8SH_chapter">Доступность:</p><p>Сервер, толстый клиент.</p>
        </body></html>
        """.trimIndent()

    private val deprecatedConstructorPage =
        """
        <html><head></head><body>
        <p class="V8SH_title">ТестОбъект (TestObject)</p>
        <p class="V8SH_heading">По имени файла</p>
        <div class="__DEPRECATED_SHOW_STYLE__"><p class="not_used">Не рекомендуется использовать, начиная с версии 8.3.20.</p>
        <p class="__DEPRECATED_SHOW_STYLE__">Рекомендуется использовать:</p>
        <ul class="__DEPRECATED_SHOW_STYLE__"><li><a href="v8help://x/C.html">По потоку</a></li></ul></div>
        <p class="V8SH_chapter">Синтаксис:</p>Новый ТестОбъект(&lt;ИмяФайла&gt;)
        <p class="V8SH_chapter">Описание:</p>Создает объект.
        </body></html>
        """.trimIndent()

    @Test
    fun `enum page keeps availability and ignores unknown blocks`() {
        val result = EnumPageParser().parse(enumPage.byteInputStream())

        assertEquals("МетодСжатияФайлаАрхива", result.nameRu)
        assertEquals("Содержит варианты методов сжатия записываемого файла архива.", result.description)
        assertEquals("Сервер, толстый клиент, внешнее соединение.", result.status.availability)
        assertFalse(result.status.isDeprecated)
    }

    @Test
    fun `deprecated enum value`() {
        val result = EnumValuePageParser().parse(deprecatedEnumValuePage.byteInputStream())

        assertEquals("Сжатие", result.nameRu)
        assertEquals("Не рекомендуется использовать, начиная с версии 8.3.26.", result.status.deprecated)
        assertEquals("Сервер, толстый клиент.", result.status.availability)
    }

    @Test
    fun `deprecated constructor with replacement`() {
        val result = ConstructorPageParser().parse(deprecatedConstructorPage.byteInputStream())

        assertEquals("Не рекомендуется использовать, начиная с версии 8.3.20.", result.status.deprecated)
        assertEquals(listOf("По потоку"), result.status.replacements)
    }
}
