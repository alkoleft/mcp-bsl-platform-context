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

class TypeMemberSearch(
    private val resolve: (String) -> List<PlatformTypeDefinition>,
) {
    fun search(
        query: String,
        apiType: ApiType? = null,
    ): List<SearchResult> {
        val words = query.split("\\s+".toRegex()).filter { it.isNotBlank() }
        for (end in words.lastIndex downTo 1) {
            val owners = resolve(words.take(end).joinToString(" "))
            if (owners.isEmpty()) continue
            val memberName = words.drop(end).joinToString(" ")
            return owners.flatMap { owner ->
                searchMembersInType(owner, memberName, apiType).map { member ->
                    if (member.name.equals(memberName, ignoreCase = true)) {
                        SearchResult(member, -1, 0, "exact-type-member")
                    } else {
                        typeMember(member)
                    }
                }
            }
        }
        return emptyList()
    }

    /**
     * Члены типа, имя которых содержит [memberQuery]. Фильтр [apiType] — как в [RegularSearch]:
     * METHOD — только методы, PROPERTY — только свойства, TYPE — членов типа нет;
     * без фильтра (и для CONSTRUCTOR) — методы и свойства.
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

        return results
    }
}

fun typeMember(item: Definition) = SearchResult(item, 2, 0, "type-member")
