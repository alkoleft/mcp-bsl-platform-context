/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.search

import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.infrastructure.search.SimpleSearchEngine.SearchResult
import ru.alkoleft.context.infrastructure.search.indexes.Indexes

class TypeMemberSearch(
    private val indexes: Indexes,
) {
    fun search(
        query: String,
        apiType: ApiType? = null,
    ): List<SearchResult> {
        val words = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val results = mutableListOf<SearchResult>()

        // Пробуем разные комбинации слов как "тип.элемент"
        for (i in words.indices) {
            val typeName = words[i]
            val memberQuery = words.drop(i + 1).joinToString(" ")

            if (memberQuery.isNotEmpty()) {
                indexes.types
                    .get(typeName)
                    .forEach { type ->
                        val members = searchMembersInType(type, memberQuery, apiType)
                        members.forEach { member ->
                            results.add(typeMember(member))
                        }
                    }
            }
        }

        return results
    }

    /**
     * Члены типа, имя которых содержит [memberQuery]. Фильтр [apiType] — как в [RegularSearch]:
     * METHOD — только методы, PROPERTY — только свойства, TYPE — членов типа нет;
     * без фильтра (и для CONSTRUCTOR) — методы, свойства и события.
     */
    fun searchMembersInType(
        type: PlatformTypeDefinition,
        memberQuery: String,
        apiType: ApiType? = null,
    ): List<Definition> {
        val results = mutableListOf<Definition>()
        val queryLower = memberQuery.lowercase()
        val anyKind = apiType == null || apiType == ApiType.CONSTRUCTOR

        // Поиск среди методов
        if (anyKind || apiType == ApiType.METHOD) {
            type.methods
                .filter { it.name.lowercase().contains(queryLower) }
                .forEach { results.add(it) }
        }

        // Поиск среди свойств
        if (anyKind || apiType == ApiType.PROPERTY) {
            type.properties
                .filter { it.name.lowercase().contains(queryLower) }
                .forEach { results.add(it) }
        }

        // События — только без фильтра по виду элемента
        if (anyKind) {
            type.events
                .filter { it.name.lowercase().contains(queryLower) }
                .forEach { results.add(it) }
        }

        return results
    }
}

fun typeMember(item: Definition) = SearchResult(item, 2, 0, "type-member")
