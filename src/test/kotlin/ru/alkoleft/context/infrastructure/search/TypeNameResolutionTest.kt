package ru.alkoleft.context.infrastructure.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.alkoleft.context.business.entities.PlatformTypeDefinition
import ru.alkoleft.context.business.valueobjects.ApiType
import ru.alkoleft.context.infrastructure.persistent.storage.PlatformContextLoader
import ru.alkoleft.context.infrastructure.persistent.storage.PlatformContextStorage
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeNameResolutionTest {
    private val storage = PlatformContextStorage(PlatformContextLoader(), Path.of(System.getProperty("platform.context.path")))
    private val engine = SimpleSearchEngine(storage).also { it.initialize(storage) }

    fun families() = storage.types.filter { ".<" in it.name }.also { check(it.isNotEmpty()) }.map { it.name }

    @ParameterizedTest
    @MethodSource("families")
    fun `short and full names identify the same HBK card`(name: String) {
        assertSame(storage.types.single { it.name == name }, engine.findType(name.substringBefore(".<")))
        assertSame(engine.findType(name), engine.findType(name.substringBefore(".<")))
    }

    @Test
    fun `collection row and independent types`() {
        val prefixes = listOf("", "БизнесПроцесс", "ВнешнийОтчет", "ВнешняяОбработка", "Документ", "Задача",
            "Обработка", "Отчет", "ПланВидовРасчета", "ПланВидовХарактеристик", "ПланОбмена", "ПланСчетов", "Справочник")
        for (prefix in prefixes) {
            assertEquals("Табличная часть", engine.findType(prefix + "ТабличнаяЧасть")?.name)
            assertEquals("Строка табличной части", engine.findType(prefix + "ТабличнаяЧастьСтрока")?.name)
        }
        for (name in listOf("ДанныеФормыКоллекция", "ОбъектМетаданных: ТабличнаяЧасть")) {
            assertEquals(name, engine.findType(name)?.name)
        }
    }

    @Test
    fun `longest owner and member filter`() {
        val method = engine.findType("Табличная часть")!!.methods.single { it.name == "Количество" }
        for (owner in listOf("ДокументТабличнаяЧасть", "Табличная часть")) {
            assertSame(method, engine.search("$owner Количество", 3).first())
            assertSame(method, engine.search("$owner Количество", 3, ApiType.METHOD).first())
            assertTrue(engine.search("$owner Количество", 3, ApiType.PROPERTY).none { it === method })
        }
        assertTrue(engine.search("Документ постороннееслово", 3).isNotEmpty())
    }

    @Test
    fun `collisions preserve candidates and exact names win`() {
        fun card(name: String) = PlatformTypeDefinition(name, "", emptyList(), emptyList(), emptyList())
        val first = card("Тип.<А>")
        val second = card("Тип.<Б>")
        assertEquals(listOf(first, second), TypeNames(listOf(first, second)).candidates("ТИП"))
        val exact = card("Тип")
        assertEquals(listOf(exact), TypeNames(listOf(first, exact, second)).candidates("тип"))
    }
}
