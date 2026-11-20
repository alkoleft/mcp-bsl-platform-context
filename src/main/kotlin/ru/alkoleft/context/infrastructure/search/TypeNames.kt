package ru.alkoleft.context.infrastructure.search

import ru.alkoleft.context.business.entities.PlatformTypeDefinition

class TypeNames(types: List<PlatformTypeDefinition>) {
    private val exact = types.associateBy { it.name.lowercase() }
    private val aliases = mutableMapOf<String, MutableList<PlatformTypeDefinition>>()

    init {
        for (type in types) {
            val name = type.name
            if (".<" in name) addAlias(name.substringBefore(".<"), type)
            val suffix = when {
                name.equals("Табличная часть", ignoreCase = true) -> "ТабличнаяЧасть"
                name.equals("Строка табличной части", ignoreCase = true) -> "ТабличнаяЧастьСтрока"
                else -> continue
            }
            for (prefix in prefixes) addAlias(prefix + suffix, type)
        }
    }

    private fun addAlias(name: String, type: PlatformTypeDefinition) {
        aliases.getOrPut(name.lowercase()) { mutableListOf() }.add(type)
    }

    fun candidates(name: String): List<PlatformTypeDefinition> {
        val key = name.lowercase()
        return exact[key]?.let { listOf(it) } ?: aliases[key].orEmpty()
    }

    private companion object {
        val prefixes = listOf(
            "", "БизнесПроцесс", "ВнешнийОтчет", "ВнешняяОбработка", "Документ",
            "Задача", "Обработка", "Отчет", "ПланВидовРасчета", "ПланВидовХарактеристик",
            "ПланОбмена", "ПланСчетов", "Справочник",
        )
    }
}
