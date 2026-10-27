/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.search

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.entities.apiStatus
import ru.alkoleft.context.business.entities.MethodDefinition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.entities.PropertyDefinition
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.business.valueobjects.SearchQuery
import ru.alkoleft.context.infrastructure.persistent.storage.PlatformContextStorage
import ru.alkoleft.context.infrastructure.search.indexes.HashIndex
import ru.alkoleft.context.infrastructure.search.indexes.Indexes
import ru.alkoleft.context.infrastructure.search.indexes.StartWithIndex

private val logger = KotlinLogging.logger {}

class SimpleSearchEngine(
    val context: PlatformContextStorage,
) : SearchEngine {
    private val hashIndexes =
        Indexes(
            methods = HashIndex(),
            properties = HashIndex(),
            types = HashIndex(),
        )
    private val startWithIndexes =
        Indexes(
            methods = StartWithIndex(),
            properties = StartWithIndex(),
            types = StartWithIndex(),
        )

    private val compoundTypeSearch = CompoundTypeSearch(startWithIndexes)
    private val typeMemberSearch = TypeMemberSearch(startWithIndexes)
    private val regularSearch = RegularSearch(startWithIndexes)
    private val wordOrderSearch = WordOrderSearch(context)

    override fun initialize(context: PlatformContextStorage) {
        logger.info { "Инициализация поисковых индексов..." }

        try {
            context.load()
            hashIndexes.load(context)
            startWithIndexes.load(context)

            logger.info {
                "Поисковые индексы успешно инициализированы: " +
                    "типы=${hashIndexes.types.size}, " +
                    "методы=${hashIndexes.methods.size}, " +
                    "свойства=${hashIndexes.properties.size}"
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Ошибка при инициализации поисковых индексов" }
            throw RuntimeException("Не удалось инициализировать поисковые индексы", ex)
        }
    }

    fun <T> initializeIfNeed(block: () -> T): T {
        synchronized(this) {
            if (hashIndexes.types.isEmpty()) {
                initialize(context)
            }
        }
        return block()
    }

    override fun search(searchQuery: SearchQuery): List<Definition> =
        initializeIfNeed { search(searchQuery.query, searchQuery.maxResults, searchQuery.apiType) }

    override fun findType(name: String) = initializeIfNeed { hashIndexes.types.get(name).firstOrNull() }

    override fun findProperty(name: String) = initializeIfNeed { hashIndexes.properties.get(name).firstOrNull() }

    override fun findMethod(name: String) = initializeIfNeed { hashIndexes.methods.get(name).firstOrNull() }

    override fun findTypeMember(
        type: PlatformTypeDefinition,
        memberName: String,
    ): Definition? {
        val memberName = memberName.lowercase()
        // Поиск среди методов
        return initializeIfNeed {
            type.methods
                .find { it.name.lowercase() == memberName }
                ?: type.properties
                    .find { it.name.lowercase() == memberName }
                ?: type.events
                    .find { it.name.lowercase() == memberName }
        }
    }

    fun search(
        query: String,
        limit: Int?,
        type: ApiType? = null,
    ): List<Definition> {
        val effectiveLimit = limit ?: 10
        // Нормализация запроса
        val normalizedQuery = query.trim().lowercase()

        // Поиск в соответствующих индексах
        val searchResults = performIntelligentSearch(normalizedQuery, type)

        return searchResults.take(minOf(effectiveLimit, 50))
    }

    private fun performIntelligentSearch(
        query: String,
        type: ApiType?,
    ): List<Definition> {
        val words = query.split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (words.isEmpty()) return emptyList()

        // Все возможные типы поиска с приоритетами
        val searchResults = mutableListOf<SearchResult>()

        // 1. Поиск составных типов (compound-type search) - приоритет 1
        if (words.size >= 2) {
            searchResults.addAll(compoundTypeSearch.search(query))
        }

        // 2. Поиск "тип.элемент" (type-member search) - приоритет 2
        if (words.size >= 2) {
            searchResults.addAll(typeMemberSearch.search(query, type))
        }

        // 3. Обычный поиск (regular search) - приоритет 3
        searchResults.addAll(regularSearch.search(query, type))

        // 4. Поиск по порядку слов (word-order search) - приоритет 4
        if (words.size >= 2) {
            searchResults.addAll(wordOrderSearch.search(query, type))
        }

        // Удаление дубликатов и сортировка по приоритету.
        // Внутри одного вида совпадения элементы «только толстый клиент» (обычные формы и т.п.)
        // опускаются ниже: новая разработка ведётся на управляемых формах.
        return removeDuplicates(searchResults)
            .sortedWith(
                compareBy<SearchResult> { it.priority }
                    .thenBy { if (it.item.apiStatus.isThickClientOnly) 1 else 0 }
                    .thenByDescending { it.wordsMatched },
            )
            .map { it.item }
    }

    private fun removeDuplicates(results: List<SearchResult>): List<SearchResult> {
        val seen = mutableSetOf<String>()
        return results.filter { result ->
            val name = result.item.name
            seen.add(name)
        }
    }

    /**
     * Результат поиска с метаданными для сортировки
     */
    data class SearchResult(
        val item: Definition,
        val priority: Int, // 1-4, где 1 - высший приоритет
        val wordsMatched: Int, // количество совпавших слов для приоритета
        val matchType: String, // тип совпадения
    )
}

fun Indexes.load(context: PlatformContextStorage) {
    properties.load(
        context.properties,
        PropertyDefinition::name,
    )
    methods.load(
        context.methods,
        MethodDefinition::name,
    )
    types.load(
        context.types,
        PlatformTypeDefinition::name,
    )
}
