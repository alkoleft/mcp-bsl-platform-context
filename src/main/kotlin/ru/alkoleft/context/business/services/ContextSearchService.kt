/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.business.services

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.alkoleft.context.business.entities.Definition
import ru.alkoleft.context.business.entities.Signature
import ru.alkoleft.context.business.persistent.PlatformContextRepository
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.exceptions.InvalidSearchQueryException
import ru.alkoleft.context.exceptions.PlatformTypeNotFoundException
import ru.alkoleft.context.exceptions.TypeMemberNotFoundException

@Service
class ContextSearchService(
    private val repository: PlatformContextRepository,
) {
    private val logger = KotlinLogging.logger {}

    fun searchAll(
        query: String,
        type: String? = null,
        limit: Int? = null,
    ): List<Definition> {
        logger.debug { "searchAll called with query='$query', type='$type', limit='$limit'" }
        if (query.isBlank()) {
            logger.warn { "searchAll: query is blank" }
            throw InvalidSearchQueryException("Запрос не может быть пустым")
        }
        val effectiveLimit = limit?.coerceIn(1, 50) ?: 10
        val result = repository.search(query, effectiveLimit, getApiType(type))
        logger.debug { "searchAll result size: ${result.size}" }
        return result
    }

    fun getInfo(
        name: String,
        type: String,
    ): Definition? {
        logger.debug { "getInfo called with name='$name', type='$type'" }
        if (name.isBlank() || type.isBlank()) {
            logger.warn { "getInfo: name or type is blank" }
            throw InvalidSearchQueryException("Имя элемента и тип элемента не могут быть пустыми")
        }

        val apiType = getApiType(type)
        val result =
            when (apiType) {
                ApiType.TYPE -> repository.findType(name)
                ApiType.PROPERTY -> repository.findProperty(name)
                ApiType.METHOD -> repository.findMethod(name)
                else -> throw InvalidSearchQueryException("Получение информации для типа '$type' не поддерживается")
            }
        logger.debug { "getInfo result: $result" }
        return result
    }

    fun findMemberByTypeAndName(
        typeName: String,
        memberName: String,
    ): Definition {
        logger.debug { "findMemberByTypeAndName called with typeName='$typeName', memberName='$memberName'" }
        if (typeName.isBlank() || memberName.isBlank()) {
            logger.warn { "findMemberByTypeAndName: typeName or memberName is blank" }
            throw InvalidSearchQueryException("Имя типа и имя элемента не могут быть пустыми")
        }

        val type = repository.findType(typeName)
        if (type == null) {
            logger.warn { "findMemberByTypeAndName: type '$typeName' not found" }
            throw PlatformTypeNotFoundException(typeName)
        }

        val result =
            repository.findTypeMember(type, memberName)
                ?: throw TypeMemberNotFoundException(memberName, typeName)
        logger.debug { "findMemberByTypeAndName result: $result" }
        return result
    }

    fun findTypeMembers(typeName: String): List<Definition> {
        logger.debug { "findTypeMembers called with typeName='$typeName'" }
        if (typeName.isBlank()) {
            logger.warn { "findTypeMembers: typeName is blank" }
            throw InvalidSearchQueryException("Имя типа не может быть пустым")
        }
        val type = repository.findType(typeName)

        return if (type != null) {
            val result = type.methods + type.properties + type.events
            logger.debug { "findTypeMembers result size: ${result.size}" }
            result
        } else {
            logger.warn { "findTypeMembers: type '$typeName' not found" }
            throw PlatformTypeNotFoundException(typeName)
        }
    }

    fun findConstructors(typeName: String): List<Signature> {
        logger.debug { "findConstructors called with typeName='$typeName'" }
        if (typeName.isBlank()) {
            logger.warn { "findConstructors: typeName is blank" }
            throw InvalidSearchQueryException("Имя типа не может быть пустым")
        }
        val type = repository.findType(typeName)

        val result =
            type?.constructors ?: run {
                logger.warn { "findConstructors: type '$typeName' not found" }
                throw PlatformTypeNotFoundException(typeName)
            }
        logger.debug { "findConstructors result size: ${result.size}" }
        return result
    }

    fun getApiType(type: String?): ApiType? {
        if (type.isNullOrBlank()) return null
        return ApiType.getType(type) ?: throw InvalidSearchQueryException("Неизвестный тип '$type' искомого элемента")
    }
}
