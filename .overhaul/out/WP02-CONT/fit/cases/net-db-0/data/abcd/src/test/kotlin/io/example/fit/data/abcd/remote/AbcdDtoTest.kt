package io.example.fit.data.abcd.remote

import io.example.fit.data.abcd.remote.dto.AbcdDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Decodes a real answer, kept in src/test/resources, so a change in the wire format fails here
 * first.
 */
class AbcdDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the list answer decodes, and unknown fields are ignored`() {
        val dtos = json.decodeFromString<List<AbcdDto>>(resource("abcd.json"))

        assertEquals(listOf("1", "2"), dtos.map { it.id })
        assertEquals("Second", dtos[1].toDomain().title)
    }

    @Test
    fun `a missing title decodes to an empty one instead of failing the list`() {
        val dto = json.decodeFromString<AbcdDto>("""{"id": "7"}""")

        assertEquals("", dto.toDomain().title)
    }

    private fun resource(name: String): String {
        val url = checkNotNull(javaClass.classLoader?.getResource(name)) {
            "$name is missing from src/test/resources"
        }
        return url.readText()
    }
}
