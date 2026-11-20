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

class RegularSearch(
    private val indexes: Indexes,
    private val resolve: (String) -> List<PlatformTypeDefinition>,
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
            resolve(query).forEach { results.add(SearchResult(it, 0, 0, "exact-type")) }
            indexes.types
                .get(query)
                .forEach { results.add(regular(it)) }
        }

        return results
    }
}

fun regular(item: Definition) = SearchResult(item, 3, 0, "regular")
