/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.persistent.storage

import ru.alkoleft.context.business.entities.MethodDefinition
import ru.alkoleft.context.business.entities.ParameterDefinition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.entities.PropertyDefinition
import ru.alkoleft.context.business.entities.Signature
import ru.alkoleft.context.infrastructure.hbk.models.ConstructorInfo
import ru.alkoleft.context.infrastructure.hbk.models.EnumInfo
import ru.alkoleft.context.infrastructure.hbk.models.EnumValueInfo
import ru.alkoleft.context.infrastructure.hbk.models.MethodInfo
import ru.alkoleft.context.infrastructure.hbk.models.MethodParameterInfo
import ru.alkoleft.context.infrastructure.hbk.models.MethodSignatureInfo
import ru.alkoleft.context.infrastructure.hbk.models.ObjectInfo
import ru.alkoleft.context.infrastructure.hbk.models.PropertyInfo

fun MethodInfo.toEntity() =
    MethodDefinition(
        name = nameRu,
        description = description,
        returnType = returnValue?.type ?: "",
        returnDescription = returnValue?.description?.trim() ?: "",
        signature = signatures.map(MethodSignatureInfo::toEntity),
        status = status,
    )

fun MethodSignatureInfo.toEntity() =
    Signature(
        name = name,
        parameters = parameters.map(MethodParameterInfo::toEntity),
        description = description,
    )

fun ConstructorInfo.toEntity() =
    Signature(
        name = name,
        parameters = parameters.map(MethodParameterInfo::toEntity),
        description = description,
        status = status,
    )

fun MethodParameterInfo.toEntity() =
    ParameterDefinition(
        name = name,
        type = type,
        description = description,
        required = !isOptional,
    )

fun PropertyInfo.toEntity() =
    PropertyDefinition(
        name = nameRu,
        description = description,
        propertyType = typeName,
        isReadOnly = readonly,
        status = status,
    )

fun ObjectInfo.toEntity() =
    PlatformTypeDefinition(
        name = nameRu,
        description = description,
        methods = methods?.map(MethodInfo::toEntity) ?: emptyList(),
        properties = properties?.map(PropertyInfo::toEntity) ?: emptyList(),
        constructors = constructors?.map(ConstructorInfo::toEntity) ?: emptyList(),
        status = status,
        events = events?.map { it.toEventEntity(nameRu) } ?: emptyList(),
    )

fun MethodInfo.toEventEntity(owner: String) = toEntity().copy(isEvent = true, ownerType = owner)

fun EnumInfo.toEntity() =
    PlatformTypeDefinition(
        name = nameRu,
        description = description,
        methods = emptyList(),
        properties = values.map { it.toEntity(nameRu) },
        constructors = emptyList(),
        status = status,
        isEnum = true,
    )

fun EnumValueInfo.toEntity(enumName: String) =
    PropertyDefinition(
        name = nameRu,
        description = description,
        propertyType = enumName,
        isReadOnly = true,
        status = status,
    )

/**
 * В справке встречаются одноимённые типы (например, два «ЭлементыФормы»: для обычной формы
 * и для формы клиентского приложения). Индекс хранит одно значение на имя, поэтому второй тип
 * был бы недоступен. Вариант с самой широкой доступностью сохраняет исходное имя, остальные
 * получают суффикс с доступностью: «ЭлементыФормы (Толстый клиент)». В описаниях указываются
 * взаимные ссылки.
 */
fun disambiguateTypeNames(types: List<PlatformTypeDefinition>): List<PlatformTypeDefinition> {
    val result = types.toMutableList()
    val usedNames = types.map { it.name.lowercase() }.toMutableSet()
    types.indices
        .groupBy { types[it].name.lowercase() }
        .values
        .filter { it.size > 1 }
        .forEach { group ->
            val ordered = group.sortedByDescending { availabilityWidth(types[it].status.availability) }
            val primary = types[ordered.first()]
            val alternatives =
                ordered.drop(1).mapIndexed { n, i ->
                    val base = types[i].name
                    val suffix = availabilitySuffix(types[i].status.availability) ?: "вариант ${n + 2}"
                    var candidate = "$base ($suffix)"
                    var k = 2
                    while (!usedNames.add(candidate.lowercase())) {
                        candidate = "$base ($suffix, ${k++})"
                    }
                    i to candidate
                }
            result[ordered.first()] =
                primary.copy(
                    description =
                        primary.description.trimEnd() +
                            "\n\nЕсть одноимённый тип: " + alternatives.joinToString(", ") { "`${it.second}`" } + ".",
                )
            alternatives.forEach { (i, alternativeName) ->
                result[i] =
                    types[i].copy(
                        name = alternativeName,
                        description = types[i].description.trimEnd() + "\n\nОдноимённый тип с более широкой доступностью: `${primary.name}`.",
                    )
            }
        }
    return result
}

/** Количество перечисленных клиентов/контекстов в строке «Доступность» (первое предложение). */
internal fun availabilityWidth(availability: String?): Int =
    availability
        ?.lineSequence()
        ?.firstOrNull()
        ?.substringBefore('.')
        ?.split(',')
        ?.count { it.isNotBlank() } ?: 0

/** Короткая подпись доступности для имени: первое предложение, не длиннее 80 символов. */
internal fun availabilitySuffix(availability: String?): String? {
    val text =
        availability
            ?.lineSequence()
            ?.firstOrNull()
            ?.substringBefore('.')
            ?.trim()
            ?.takeIf { it.isNotBlank() } ?: return null
    return if (text.length <= 80) text else text.take(80).substringBeforeLast(',').trim() + "…"
}
