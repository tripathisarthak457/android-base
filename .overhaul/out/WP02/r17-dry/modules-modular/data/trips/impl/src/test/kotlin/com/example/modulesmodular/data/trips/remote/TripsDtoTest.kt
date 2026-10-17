package com.example.modulesmodular.data.trips.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Decodes a real answer, kept in src/test/resources, so a change in the wire format fails here
 * first.
 */
class TripsDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the list answer decodes, and unknown fields are ignored`() {
        val dtos = json.decodeFromString<List<TripsDto>>(resource("trips.json"))

        assertEquals(listOf("1", "2"), dtos.map { it.id })
        assertEquals("Second", dtos[1].toDomain().title)
    }

    @Test
    fun `a missing title decodes to an empty one instead of failing the list`() {
        val dto = json.decodeFromString<TripsDto>("""{"id": "7"}""")

        assertEquals("", dto.toDomain().title)
    }

    private fun resource(name: String): String {
        val url = checkNotNull(javaClass.classLoader?.getResource(name)) {
            "$name is missing from src/test/resources"
        }
        return url.readText()
    }
}
