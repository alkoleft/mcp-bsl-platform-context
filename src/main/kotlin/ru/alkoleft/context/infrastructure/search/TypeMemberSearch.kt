/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.search

import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.infrastructure.search.SimpleSearchEngine.SearchResult
import ru.alkoleft.context.infrastructure.search.indexes.Indexes

class TypeMemberSearch(
    private val indexes: Indexes,
) {
    fun search(query: String): List<SearchResult> {
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
                        val members = searchMembersInType(type, memberQuery)
                        members.forEach { member ->
                            results.add(typeMember(member))
                        }
                    }
            }
        }

        return results
    }

    fun searchMembersInType(
        type: PlatformTypeDefinition,
        memberQuery: String,
    ): List<Definition> {
        val results = mutableListOf<Definition>()
        val queryLower = memberQuery.lowercase()

        // Поиск среди методов
        type.methods
            .filter { it.name.lowercase().contains(queryLower) }
            .forEach { results.add(it) }

        // Поиск среди свойств
        type.properties
            .filter { it.name.lowercase().contains(queryLower) }
            .forEach { results.add(it) }
        type.events
            .filter { it.name.lowercase().contains(queryLower) }
            .forEach { results.add(it) }

        return results
    }
}

fun typeMember(item: Definition) = SearchResult(item, 2, 0, "type-member")
