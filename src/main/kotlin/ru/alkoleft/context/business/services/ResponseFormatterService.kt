/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.services

import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.entities.Signature

interface ResponseFormatterService {
    fun formatError(e: Throwable): String

    fun formatQuery(query: String): String

    fun formatSearchResults(result: List<Definition>): String

    fun formatMember(definition: Definition?, ownerName: String? = null): String

    fun formatTypeMembers(definitions: List<Definition>): String

    fun formatConstructors(
        result: List<Signature>,
        typeName: String,
    ): String
}
