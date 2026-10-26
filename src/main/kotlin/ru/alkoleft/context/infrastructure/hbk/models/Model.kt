/*
 * Copyright (c) 2025 alkoleft. All rights reserved.
 * This file is part of the mcp-bsl-context project.
 *
 * Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package ru.alkoleft.context.infrastructure.hbk.models

import ru.alkoleft.context.business.entities.ApiStatus

/**
 * Информация о свойстве объекта платформы 1С:Предприятие.
 *
 * Содержит полную информацию о свойстве, включая его название на двух языках,
 * описание, тип, флаг только для чтения и связанные объекты.
 *
 * @property nameRu Русское название свойства
 * @property nameEn Английское название свойства
 * @property description Описание свойства
 * @property readonly Флаг, указывающий, что свойство только для чтения
 * @property typeName Название типа свойства
 * @property note Дополнительная заметка о свойстве
 * @property relatedObjects Список связанных объектов
 */
data class PropertyInfo(
    val nameRu: String,
    val nameEn: String,
    val description: String,
    val readonly: Boolean,
    val typeName: String,
    val note: String?,
    val relatedObjects: List<RelatedObject>?,
    val status: ApiStatus = ApiStatus(),
)

/**
 * Информация о методе объекта платформы 1С:Предприятие.
 *
 * Содержит информацию о методе, включая его название на двух языках,
 * сигнатуры (перегрузки), примеры использования и связанные объекты.
 *
 * @property nameRu Русское название метода
 * @property nameEn Английское название метода
 * @property description Описание метода
 * @property signatures Список сигнатур метода (перегрузок)
 * @property example Пример использования метода
 * @property note Дополнительная заметка о методе
 * @property relatedObjects Список связанных объектов
 * @property returnValue Информация о возвращаемом значении
 */
data class MethodInfo(
    val nameRu: String,
    val nameEn: String,
    val signatures: List<MethodSignatureInfo>,
    val example: String?,
    val note: String?,
    val relatedObjects: List<RelatedObject>?,
    var description: String,
    var returnValue: ValueInfo?,
    val status: ApiStatus = ApiStatus(),
)

/**
 * Информация о параметре метода.
 *
 * @property name Название параметра
 * @property type Тип параметра
 * @property isOptional Флаг, указывающий что параметр необязательный
 * @property description Описание параметра
 */
data class MethodParameterInfo(
    val name: String,
    val type: String,
    val isOptional: Boolean,
    val description: String,
)

/**
 * Информация о перечислении платформы 1С:Предприятие.
 *
 * Содержит информацию о перечислении, включая его название на двух языках,
 * описание, список значений и связанные объекты.
 *
 * @property nameRu Русское название перечисления
 * @property nameEn Английское название перечисления
 * @property description Описание перечисления
 * @property relatedObjects Список связанных объектов
 * @property values Список значений перечисления
 * @property example Пример использования перечисления
 */
data class EnumInfo(
    val nameRu: String,
    val nameEn: String,
    val description: String,
    val relatedObjects: List<RelatedObject>?,
    val values: MutableList<EnumValueInfo> = mutableListOf(),
    var example: String?,
    val status: ApiStatus = ApiStatus(),
)

/**
 * Информация о значении перечисления.
 *
 * @property nameRu Русское название значения
 * @property nameEn Английское название значения
 * @property description Описание значения
 * @property relatedObjects Список связанных объектов
 */
data class EnumValueInfo(
    val nameRu: String,
    val nameEn: String,
    val description: String,
    val relatedObjects: List<RelatedObject>?,
    val status: ApiStatus = ApiStatus(),
)

/**
 * Информация о связанном объекте.
 *
 * Представляет ссылку на другой объект документации.
 *
 * @property name Название связанного объекта
 * @property href Ссылка на страницу объекта
 */
data class RelatedObject(
    val name: String,
    val href: String,
)

/**
 * Информация о значении (тип и описание).
 *
 * @property type Тип значения
 * @property description Описание значения
 */
data class ValueInfo(
    val type: String,
    val description: String,
)

/**
 * Информация о сигнатуре метода.
 *
 * Представляет одну из перегрузок метода с параметрами и возвращаемым значением.
 *
 * @property name Название метода
 * @property syntax Синтаксис вызова метода
 * @property parameters Список параметров
 * @property description Описание сигнатуры
 */
data class MethodSignatureInfo(
    var name: String,
    var syntax: String,
    var parameters: List<MethodParameterInfo>,
    var description: String,
)

/**
 * Информация об объекте платформы 1С:Предприятие.
 *
 * Содержит информацию об объекте, включая его название на двух языках,
 * описание, примеры использования и связанные объекты.
 *
 * @property nameRu Русское название объекта
 * @property nameEn Английское название объекта
 * @property description Описание объекта
 * @property example Пример использования объекта
 * @property note Дополнительная заметка об объекте
 * @property relatedObjects Список связанных объектов
 */
data class ObjectInfo(
    val nameRu: String,
    val nameEn: String,
    val description: String,
    val example: String?,
    val note: String?,
    val relatedObjects: List<RelatedObject>?,
    val properties: List<PropertyInfo>? = null,
    val methods: List<MethodInfo>? = null,
    val constructors: List<ConstructorInfo>? = null,
    val status: ApiStatus = ApiStatus(),
    val events: List<MethodInfo>? = null,
)

/**
 * Информация о конструкторе объекта.
 *
 * Содержит информацию о конструкторе, включая его синтаксис,
 * параметры, описание и примеры использования.
 *
 * @property name Название конструктора
 * @property syntax Синтаксис вызова конструктора
 * @property parameters Список параметров
 * @property description Описание конструктора
 * @property example Пример использования конструктора
 * @property note Дополнительная заметка о конструкторе
 * @property relatedObjects Список связанных объектов
 */
data class ConstructorInfo(
    val name: String,
    val syntax: String,
    val parameters: List<MethodParameterInfo>,
    val description: String,
    val example: String?,
    val note: String?,
    val relatedObjects: List<RelatedObject>?,
    val status: ApiStatus = ApiStatus(),
)
