package __PKG__.data.__NAME__.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Decodes a real answer, kept in src/test/resources, so a change in the wire format fails here first. */
class __P__DtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the list answer decodes, and unknown fields are ignored`() {
        val dtos = json.decodeFromString<List<__P__Dto>>(resource("__NAME__.json"))

        assertEquals(listOf("1", "2"), dtos.map { it.id })
        assertEquals("Second", dtos[1].toDomain().title)
    }

    @Test
    fun `a missing title decodes to an empty one instead of failing the list`() {
        val dto = json.decodeFromString<__P__Dto>("""{"id": "7"}""")

        assertEquals("", dto.toDomain().title)
    }

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "$name is missing from src/test/resources" }.readText()
}
