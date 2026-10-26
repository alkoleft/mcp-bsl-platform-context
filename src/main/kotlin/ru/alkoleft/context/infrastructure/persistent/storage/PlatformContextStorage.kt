/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.persistent.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.alkoleft.context.business.entities.MethodDefinition
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.entities.PropertyDefinition
import ru.alkoleft.context.infrastructure.hbk.models.MethodInfo
import ru.alkoleft.context.infrastructure.hbk.models.PropertyInfo
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

class PlatformContextStorage(
    private val loader: PlatformContextLoader,
    val platformPath: Path,
) {
    var methods: List<MethodDefinition> = emptyList()
    var properties: List<PropertyDefinition> = emptyList()
    var types: List<PlatformTypeDefinition> = emptyList()
    private val indexInitialized = AtomicBoolean(false)
    private val lock = ReentrantReadWriteLock()

    fun load() {
        logger.debug { "Вызвана загрузка платформенного контекста" }
        // Быстрая проверка с read lock
        lock.read {
            if (indexInitialized.get()) {
                logger.debug { "Контекст уже инициализирован (read lock)" }
                return
            }
        }

        // Инициализация с write lock
        return lock.write {
            // Двойная проверка в write lock
            if (indexInitialized.get()) {
                logger.debug { "Контекст уже инициализирован (write lock)" }
                return
            } else {
                logger.info { "Инициализация платформенного контекста..." }
                loadPlatformContext()
                logger.info { "Платформенный контекст успешно инициализирован" }
            }
        }
    }

    private fun loadPlatformContext() {
        if (indexInitialized.get()) {
            logger.debug { "Контекст уже инициализирован (loadPlatformContext)" }
            return
        }
        logger.info { "Загрузка платформенного контекста..." }
        loader.loadPlatformContext(platformPath) {
            methods = globalMethods().map(MethodInfo::toEntity).toList()
            properties = globalProperties().map(PropertyInfo::toEntity).toList()
            val objectTypes = types().map { it.toEntity() }.toList()
            val enumTypes = enums().map { it.toEntity() }.toList()
            logger.info { "Загружено перечислений: ${enumTypes.size}" }
            types = objectTypes + enumTypes
        }
        logger.info { "Загружено: методы=${methods.size}, свойства=${properties.size}, типы=${types.size}" }
        indexInitialized.set(true)
    }
}
