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
        note = note?.trim().orEmpty(),
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
        note = note?.trim().orEmpty(),
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
        note = note?.trim().orEmpty(),
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
        tocName = tocName,
        note = note?.trim().orEmpty(),
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
        tocName = tocName,
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
 * В справке встречаются одноимённые типы. Индекс хранит одно значение на имя, поэтому без
 * переименования второй тип был бы недоступен. Случаи на справке 8.3.27:
 * - разные типы с разной доступностью (ЭлементыФормы для обычной формы и для формы клиентского
 *   приложения) — вариант с самой широкой доступностью сохраняет имя, остальные получают суффикс
 *   с доступностью: «ЭлементыФормы (Толстый клиент)»;
 * - несколько записей оглавления с разными именами указывают на одну страницу справки
 *   (СтандартнаяКомандаПланировщика / СтандартнаяКомандаПоляПланировщика) — вариант получает
 *   своё имя из оглавления.
 * Основной вариант: шире доступность → меньше устаревших элементов → имя в оглавлении совпадает с заголовком.
 */
fun disambiguateTypeNames(types: List<PlatformTypeDefinition>): List<PlatformTypeDefinition> {
    val result = types.toMutableList()
    val usedNames = types.map { it.name.lowercase() }.toMutableSet()
    types.indices
        .groupBy { types[it].name.lowercase() }
        .values
        .filter { it.size > 1 }
        .forEach { group ->
            val ordered =
                group.sortedWith(
                    compareByDescending<Int> { availabilityWidth(types[it].status.availability) }
                        .thenBy { deprecatedShare(types[it]) }
                        .thenBy { if (types[it].tocName.isNullOrBlank() || types[it].tocName.equals(types[it].name, true)) 0 else 1 },
                )
            val primary = types[ordered.first()]
            val alternatives =
                ordered.drop(1).mapIndexed { n, i ->
                    val variant = types[i]
                    val sameAvailability = availabilitySuffix(variant.status.availability) == availabilitySuffix(primary.status.availability)
                    val tocName = variant.tocName?.trim()
                    val (candidateBase, note) =
                        when {
                            !sameAvailability && availabilitySuffix(variant.status.availability) != null ->
                                "${variant.name} (${availabilitySuffix(variant.status.availability)})" to
                                    "Одноимённый тип с более широкой доступностью: `${primary.name}`."

                            !tocName.isNullOrBlank() && !tocName.equals(variant.name, true) && tocName.lowercase() !in usedNames ->
                                tocName to
                                    "Имя из оглавления справки; страница справки общая с типом `${primary.name}`."

                            deprecatedShare(variant) > deprecatedShare(primary) ->
                                "${variant.name} (устаревший вариант)" to
                                    "Актуальный вариант: `${primary.name}`."

                            else ->
                                "${variant.name} (вариант ${n + 2})" to
                                    "Одноимённый тип: `${primary.name}`."
                        }
                    var candidate = candidateBase
                    var k = 2
                    while (!usedNames.add(candidate.lowercase())) {
                        candidate = "$candidateBase ($k)"
                        k++
                    }
                    Triple(i, candidate, note)
                }
            result[ordered.first()] =
                primary.copy(
                    description =
                        primary.description.trimEnd() +
                            "\n\nЕсть одноимённый тип: " + alternatives.joinToString(", ") { "`${it.second}`" } + ".",
                )
            alternatives.forEach { (i, alternativeName, note) ->
                result[i] = types[i].copy(name = alternativeName, description = types[i].description.trimEnd() + "\n\n" + RENAMED_TYPE_NOTE + " " + note)
            }
        }
    return result
}

/** Пометка в описании переименованного одноимённого типа (используется для подсчёта в логе). */
const val RENAMED_TYPE_NOTE = "Переименован из-за одноимённого типа в справке."

/** Доля устаревших элементов типа (сам тип, методы, свойства/значения, события). */
internal fun deprecatedShare(type: PlatformTypeDefinition): Double {
    val statuses = listOf(type.status) + type.methods.map { it.status } + type.properties.map { it.status } + type.events.map { it.status }
    return statuses.count { it.isDeprecated }.toDouble() / statuses.size
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
