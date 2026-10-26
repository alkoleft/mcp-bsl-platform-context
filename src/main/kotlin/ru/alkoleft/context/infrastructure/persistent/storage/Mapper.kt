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
