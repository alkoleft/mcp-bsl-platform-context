/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.parsers.core

import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import java.io.InputStream

/**
 * Абстрактный базовый класс для парсеров HTML страниц HBK документации.
 *
 * Этот класс предоставляет общую функциональность для парсинга HTML страниц
 * документации платформы 1С:Предприятие. Он использует Ksoup для парсинга HTML
 * и делегирует обработку конкретных элементов специализированным обработчикам.
 *
 * Основные возможности:
 * - Парсинг HTML потоков с использованием Ksoup
 * - Управление состоянием обработчика
 * - Предоставление результата парсинга
 *
 * @param T Тип результата парсинга
 * @param handler Обработчик для конкретного типа страницы
 *
 * @see PageProxyHandler для создания специализированных обработчиков
 * @see com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser для парсинга HTML
 */
abstract class PageParser<T>(
    protected val handler: PageProxyHandler<T>,
) {
    /**
     * Парсит HTML поток и возвращает результат.
     *
     * @param inputStream Поток с HTML содержимым
     * @return Результат парсинга
     */
    fun parse(inputStream: InputStream): T {
        handler.cleanState()
        parsePage(inputStream, handler)
        handler.onParsingFinished()
        return handler.getResult()
    }

    /**
     * Выполняет парсинг HTML страницы с использованием Ksoup.
     *
     * @param inputStream Поток с HTML содержимым
     * @param handler Обработчик HTML элементов
     */
    protected fun parsePage(
        inputStream: InputStream,
        handler: KsoupHtmlHandler,
    ) {
        val parser = KsoupHtmlParser(handler = handler)
        inputStream.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).use {
                it.forEachLine { line -> parser.write(line) }
            }
        }
    }
}
