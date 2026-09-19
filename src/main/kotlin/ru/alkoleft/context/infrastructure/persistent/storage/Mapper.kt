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
        signature = signatures.map(MethodSignatureInfo::toEntity),
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
    )

fun ObjectInfo.toEntity() =
    PlatformTypeDefinition(
        name = nameRu,
        description = description,
        methods = methods?.map(MethodInfo::toEntity) ?: emptyList(),
        properties = properties?.map(PropertyInfo::toEntity) ?: emptyList(),
        constructors = constructors?.map(ConstructorInfo::toEntity) ?: emptyList(),
    )
