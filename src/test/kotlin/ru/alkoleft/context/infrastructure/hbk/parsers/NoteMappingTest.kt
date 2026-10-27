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
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.MethodPageParser
import ru.alkoleft.context.infrastructure.hbk.parsers.specialized.PropertyPageParser
import ru.alkoleft.context.infrastructure.persistent.storage.toEntity

/**
 * Раздел «Примечание:» должен доходить до сущностей хранилища (раньше терялся в Mapper).
 * HTML — упрощённая копия структуры страниц shcntx_ru.hbk (8.3.27).
 */
class NoteMappingTest {
    private val methodWithNote =
        """
        <html><head></head><body>
        <p class="V8SH_title">Глобальный контекст (Global context)</p>
        <p class="V8SH_heading">ПоместитьФайл (PutFile)</p>
        <p class="V8SH_chapter">Синтаксис:</p>ПоместитьФайл(&lt;Адрес&gt;)
        <p class="V8SH_chapter">Описание:</p>Помещает файл во временное хранилище.
        <p class="V8SH_chapter">Доступность: </p><p>Тонкий клиент.<br>Вызов метода выполняет обращение к серверу.<br></p>
        <p class="V8SH_chapter">Примечание:</p>Если для конфигурации свойство <a href="v8help://x/ModalityUseMode9805.html">РежимИспользованияМодальности</a> установлено в <a href="v8help://x/DontUse9804.html">НеИспользовать</a>, следует использовать метод <a href="v8help://x/BeginPutFile.html">НачатьПомещениеФайла</a>.
        <p class="V8SH_chapter">Использование в версии:</p><p>Доступен, начиная с версии 8.2.</p>
        </body></html>
        """.trimIndent()

    private val propertyWithoutNote =
        """
        <html><head></head><body>
        <p class="V8SH_title">ТаблицаЗначений (ValueTable)</p>
        <p class="V8SH_heading">Колонки (Columns)</p>
        <p class="V8SH_chapter">Описание:</p>Тип: КоллекцияКолонокТаблицыЗначений. Коллекция колонок.
        </body></html>
        """.trimIndent()

    @Test
    fun `method note reaches entity`() {
        val method = MethodPageParser().parse(methodWithNote.byteInputStream()).toEntity()

        assertTrue(method.note.contains("РежимИспользованияМодальности"), method.note)
        assertTrue(method.note.contains("НачатьПомещениеФайла"), method.note)
        assertEquals("Тонкий клиент.\nВызов метода выполняет обращение к серверу.", method.status.availability)
    }

    @Test
    fun `missing note is empty, not null`() {
        val property = PropertyPageParser().parse(propertyWithoutNote.byteInputStream()).toEntity()

        assertEquals("", property.note)
    }
}
