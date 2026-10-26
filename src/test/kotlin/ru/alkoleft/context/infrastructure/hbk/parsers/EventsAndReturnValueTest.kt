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
import ru.alkoleft.context.infrastructure.hbk.PlatformContextReader
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.MethodPageParser
import ru.alkoleft.context.infrastructure.persistent.storage.toEntity
import ru.alkoleft.context.infrastructure.persistent.storage.toEventEntity

/**
 * События (разбираются парсером методов) и описание возвращаемого значения.
 * HTML — упрощённая копия структуры страниц shcntx_ru.hbk (8.3.27).
 */
class EventsAndReturnValueTest {
    private val eventPage =
        """
        <html><head></head><body>
        <p class="V8SH_title">ФормаКлиентскогоПриложения (ClientApplicationForm)</p>
        <p class="V8SH_heading">ПриВставкеИзБуфераОбмена (OnPasteFromClipboard)</p>
        <div class="__SINCE_SHOW_STYLE__"><p class="not_used">Доступен, начиная с версии 8.3.24.</p></div>
        <p class="V8SH_chapter">Синтаксис:</p>ПриВставкеИзБуфераОбмена(&lt;Значение&gt;, &lt;СтандартнаяОбработка&gt;)
        <p class="V8SH_chapter">Параметры:</p>
        <div class="V8SH_rubric">&lt;Значение&gt;</div>Тип: Картинка, Массив.<br>Значение, полученное из буфера обмена.
        <div class="V8SH_rubric">&lt;СтандартнаяОбработка&gt;</div>Тип: Булево.<br>Признак стандартной обработки.
        <p class="V8SH_chapter">Описание:</p>Вызывается при вставке из буфера обмена.
        <p class="V8SH_chapter">Доступность:</p><p>Тонкий клиент, веб-клиент.</p>
        <p class="V8SH_chapter">Использование в версии:</p><p>Доступен, начиная с версии 8.3.24.</p>
        </body></html>
        """.trimIndent()

    private val asyncMethodPage =
        """
        <html><head></head><body>
        <p class="V8SH_title">СредстваБуфераОбмена (ClipboardTools)</p>
        <p class="V8SH_heading">ПолучитьДанныеАсинх (GetDataAsync)</p>
        <p class="V8SH_chapter">Синтаксис:</p>ПолучитьДанныеАсинх(&lt;ФорматДанных&gt;)
        <p class="V8SH_chapter">Возвращаемое значение:</p>Тип: Обещание.<br>Результат выполнения обещания – данные типа Строка, ДвоичныеДанные, Картинка или Неопределено.
        <p class="V8SH_chapter">Описание:</p>Асинхронная функция.
        </body></html>
        """.trimIndent()

    @Test
    fun `event page is parsed and mapped as event of owner type`() {
        val info = MethodPageParser().parse(eventPage.byteInputStream())
        val event = info.toEventEntity("ФормаКлиентскогоПриложения")

        assertEquals("ПриВставкеИзБуфераОбмена", event.name)
        assertTrue(event.isEvent)
        assertEquals("ФормаКлиентскогоПриложения", event.ownerType)
        assertEquals(1, event.signature.size)
        assertEquals(listOf("Значение", "СтандартнаяОбработка"), event.signature.first().parameters.map { it.name })
        assertEquals("Доступен, начиная с версии 8.3.24.", event.status.since)
        assertEquals("Тонкий клиент, веб-клиент.", event.status.availability)
    }

    @Test
    fun `return value description is kept`() {
        val method = MethodPageParser().parse(asyncMethodPage.byteInputStream()).toEntity()

        assertFalse(method.isEvent)
        assertEquals("Обещание", method.returnType)
        assertTrue(method.returnDescription.contains("Строка, ДвоичныеДанные, Картинка или Неопределено"), method.returnDescription)
    }

    @Test
    fun `global event catalogs are mapped to module names`() {
        assertEquals("Модуль приложения", PlatformContextReader.eventModuleName("События приложения"))
        assertEquals("Модуль внешнего соединения", PlatformContextReader.eventModuleName("События внешнего соединения"))
        assertEquals("Новый раздел", PlatformContextReader.eventModuleName("Новый раздел"))
    }
}
