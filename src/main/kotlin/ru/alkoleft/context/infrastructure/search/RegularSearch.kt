/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.search

import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.infrastructure.search.SimpleSearchEngine.SearchResult
import ru.alkoleft.context.infrastructure.search.indexes.Indexes

private val typeSearchAliases = mapOf(
    "табличнаячасть" to "Табличная часть",
    "документтабличнаячасть" to "Табличная часть",
)

class RegularSearch(
    private val indexes: Indexes,
) {
    fun search(
        query: String,
        type: ApiType?,
    ): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        var inMethods = type == ApiType.METHOD
        var inProperty = type == ApiType.PROPERTY
        var inType = type == ApiType.TYPE

        if (!(inMethods || inProperty || inType)) {
            inMethods = true
            inProperty = true
            inType = true
        }
        if (inMethods) {
            indexes.methods
                .get(query)
                .forEach { results.add(regular(it)) }
        }
        if (inProperty) {
            indexes.properties
                .get(query)
                .forEach { results.add(regular(it)) }
        }
        if (inType) {
            typeSearchAliases[query.lowercase()]?.let { canonicalName ->
                indexes.types.get(canonicalName)
                    .firstOrNull { it.name.equals(canonicalName, ignoreCase = true) }
                    ?.let { results.add(regular(it)) }
            }
            indexes.types
                .get(query)
                .forEach { results.add(regular(it)) }
        }

        return results
    }
}

fun regular(item: Definition) = SearchResult(item, 3, 0, "regular")
