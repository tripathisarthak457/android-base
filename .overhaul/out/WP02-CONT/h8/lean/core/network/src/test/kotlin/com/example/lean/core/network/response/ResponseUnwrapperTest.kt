package com.example.lean.core.network.response

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResponseUnwrapperTest {

    @Test
    fun `passthrough decodes the body itself, and an empty body as an empty object`() {
        val unwrapper = PassthroughUnwrapper()

        assertEquals(Item(7), unwrapper.payload("""{"id":7,"extra":true}""", Item.serializer()))
        assertEquals(Unit, unwrapper.payload("", Unit.serializer()))
    }

    @Test
    fun `passthrough reads a flat error and a nested one alike`() {
        val unwrapper = PassthroughUnwrapper()

        val flat = unwrapper.error(422, """{"message":"Invalid","code":"bad","errors":{"email":"Taken"}}""")
        val nested = unwrapper.error(409, """{"error":{"code":"conflict","detail":"Already there"}}""")

        assertEquals("Invalid", flat.message)
        assertEquals("bad", flat.code)
        assertEquals(mapOf("email" to listOf("Taken")), flat.fieldErrors)
        assertEquals("conflict", nested.code)
        assertEquals("Already there", nested.message)
    }

    @Test
    fun `an error page that is not json says nothing`() {
        val error = PassthroughUnwrapper().error(502, "<html>Bad gateway</html>")

        assertEquals(502, error.status)
        assertNull(error.message)
        assertNull(error.code)
    }

    @Test
    fun `an envelope's payload is its data`() {
        val unwrapper = EnvelopeUnwrapper()

        assertEquals(Item(3), unwrapper.payload("""{"status":true,"data":{"id":3}}""", Item.serializer()))
        assertEquals("Nope", unwrapper.error(400, """{"message":"Nope","code":"E1"}""").message)
    }

    @Test
    fun `retry-after is read as seconds or as a date, never as a negative wait`() {
        val now = Instant.fromEpochSeconds(100)

        assertEquals(30L, retryAfterSeconds("30", now))
        assertEquals(20L, retryAfterSeconds("Thu, 01 Jan 1970 00:02:00 GMT", now))
        assertEquals(0L, retryAfterSeconds("Thu, 01 Jan 1970 00:00:01 GMT", now))
        assertNull(retryAfterSeconds("soon", now))
        assertNull(retryAfterSeconds(null, now))
    }

    @Serializable
    private data class Item(val id: Int)
}
