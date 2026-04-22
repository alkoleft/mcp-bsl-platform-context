/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.toc

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.alkoleft.context.infrastructure.hbk.exceptions.TocParsingError
import ru.alkoleft.context.infrastructure.hbk.models.Chunk
import ru.alkoleft.context.infrastructure.hbk.models.NameContainer
import ru.alkoleft.context.infrastructure.hbk.models.NameObject
import ru.alkoleft.context.infrastructure.hbk.models.PropertiesContainer

private val logger = KotlinLogging.logger {}

/**
 * Парсер для разбора структуры оглавления (Table of Contents) HBK файлов.
 *
 * Этот класс отвечает за парсинг бинарных данных оглавления HBK файлов
 * платформы 1С:Предприятие. Он преобразует бинарные данные в текстовый формат
 * и затем токенизирует и разбирает структуру оглавления.
 *
 * Основные возможности:
 * - Парсинг бинарных данных оглавления
 * - Токенизация текстового содержимого
 * - Разбор иерархической структуры чанков
 * - Извлечение метаданных страниц (ID, родительские связи, названия)
 * - Обработка двуязычных названий
 *
 * @see Toc для представления разобранного оглавления
 * @see Tokenizer для токенизации содержимого
 * @see PeekableIterator для навигации по токенам
 */
internal class TocParser {
    /**
     * Парсит бинарные данные и возвращает структуру оглавления.
     *
     * @param bytes Бинарные данные оглавления
     * @return Последовательность чанков оглавления
     */
    fun parseContent(bytes: ByteArray): Sequence<Chunk> {
        logger.debug { "Чтение из ByteArray, размер: ${bytes.size}" }
        val content = bytes.toString(Charsets.UTF_8)
        return parseContent(content)
    }

    /**
     * Парсит строку содержимого и возвращает структуру оглавления.
     *
     * @param content Текстовое содержимое оглавления
     * @return Последовательность чанков оглавления
     */
    private fun parseContent(content: String): Sequence<Chunk> {
        logger.debug { "Токенизация содержимого..." }
        val tokens = Tokenizer.tokenize(content)
        logger.debug { "Токенов получено: ${tokens.size}" }
        logger.debug { "Первые 20 токенов: ${tokens.take(20)}" }
        val iterator = PeekableIterator(tokens.iterator())
        return parseTableOfContent(iterator)
    }

    /**
     * Парсит корневую структуру TableOfContent
     */
    private fun parseTableOfContent(iterator: PeekableIterator<String>): Sequence<Chunk> {
        logger.debug { "Парсинг TableOfContent..." }
        expectToken(iterator, "{", "TableOfContent: ожидался '{'")
        parseNumber(iterator, "TableOfContent: ожидалось число chunkCount")

        return sequence {
            while (iterator.hasNext() && iterator.peek() != "}") {
                logger.debug { "TableOfContent: парсинг chunk" }
                val chunk = parseChunk(iterator)
                yield(chunk)
            }
        }
    }

    /**
     * Парсит отдельный chunk
     */
    private fun parseChunk(iterator: PeekableIterator<String>): Chunk {
        logger.debug { "  [Chunk] Начало парсинга chunk..." }
        expectToken(iterator, "{", "Chunk: ожидался '{'")
        val id = parseNumber(iterator, "Chunk: ожидался id")
        val parentId = parseNumber(iterator, "Chunk: ожидался parentId")
        val childCount = parseNumber(iterator, "Chunk: ожидался childCount")

        val childIds = mutableListOf<Int>()
        for (i in 0 until childCount) {
            childIds.add(parseNumber(iterator, "Chunk: ожидался childId #${i + 1}"))
        }

        val properties = parsePropertiesContainer(iterator)
        expectToken(iterator, "}", "Chunk: ожидался '}' в конце chunk")
        logger.debug { "  [Chunk] id=$id, parentId=$parentId, childCount=$childCount, childIds=$childIds" }
        return Chunk(id, parentId, childCount, childIds, properties)
    }

    /**
     * Парсит контейнер свойств
     */
    private fun parsePropertiesContainer(iterator: PeekableIterator<String>): PropertiesContainer {
        logger.debug { "    [PropertiesContainer] Начало парсинга..." }
        expectToken(iterator, "{", "PropertiesContainer: ожидался '{'")
        val number1 = parseNumber(iterator, "PropertiesContainer: ожидался number1")
        val number2 = parseNumber(iterator, "PropertiesContainer: ожидался number2")
        val nameContainer = parseNameContainer(iterator)
        val htmlPath = parseString(iterator, "PropertiesContainer: ожидался htmlPath")
            .replace("\\", "/")
            .replace("//", "/")
        expectToken(iterator, "}", "PropertiesContainer: ожидался '}' в конце")
        logger.debug { "    [PropertiesContainer] number1=$number1, number2=$number2, htmlPath=$htmlPath" }
        return PropertiesContainer(number1, number2, nameContainer, htmlPath)
    }

    /**
     * Парсит контейнер имени
     */
    private fun parseNameContainer(iterator: PeekableIterator<String>): NameContainer {
        logger.debug { "      [NameContainer] Начало парсинга..." }
        expectToken(iterator, "{", "NameContainer: ожидался '{'")
        val number1 = parseNumber(iterator, "NameContainer: ожидался number1")
        val number2 = parseNumber(iterator, "NameContainer: ожидался number2")

        val nameObjects = mutableListOf<NameObject>()
        if (iterator.hasNext() && iterator.peek() != "}") {
            nameObjects.add(parseNameObject(iterator))

            if (iterator.hasNext() && iterator.peek() != "}") {
                nameObjects.add(parseNameObject(iterator))
            }
        }

        expectToken(iterator, "}", "NameContainer: ожидался '}' в конце")
        logger.debug { "      [NameContainer] number1=$number1, number2=$number2, nameObjects=$nameObjects" }
        return NameContainer(number1, number2, nameObjects)
    }

    /**
     * Парсит объект имени
     */
    private fun parseNameObject(iterator: PeekableIterator<String>): NameObject {
        logger.debug { "        [NameObject] Начало парсинга..." }
        expectToken(iterator, "{", "NameObject: ожидался '{'")
        val languageCode = parseString(iterator, "NameObject: ожидался languageCode")
        val name = parseString(iterator, "NameObject: ожидался name")
        expectToken(iterator, "}", "NameObject: ожидался '}' в конце")
        logger.debug { "        [NameObject] languageCode=$languageCode, name=$name" }
        return NameObject(languageCode, name)
    }

    /**
     * Парсит число
     */
    private fun parseNumber(
        iterator: PeekableIterator<String>,
        context: String,
    ): Int {
        val token =
            if (iterator.hasNext()) iterator.next() else throw TocParsingError("$context: не найден токен (конец данных)")
        return token.toIntOrNull() ?: throw TocParsingError("$context: ожидалось число, получено: '$token'")
    }

    /**
     * Парсит строку
     */
    private fun parseString(
        iterator: PeekableIterator<String>,
        context: String,
    ): String {
        val token =
            if (iterator.hasNext()) iterator.next() else throw TocParsingError("$context: не найден токен (конец данных)")
        if (!token.startsWith("\"") || !token.endsWith("\"")) {
            throw TocParsingError("$context: ожидалась строка в кавычках, получено: '$token'")
        }
        return token.substring(1, token.length - 1)
    }

    /**
     * Проверяет, что следующий токен соответствует ожидаемому
     */
    private fun expectToken(
        iterator: PeekableIterator<String>,
        expected: String,
        context: String,
    ) {
        val token =
            if (iterator.hasNext()) iterator.next() else throw TocParsingError("$context: не найден токен (конец данных)")
        if (token != expected) {
            throw TocParsingError("$context: ожидался '$expected', получен '$token'")
        }
    }
}
