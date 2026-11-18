package dev.alllexey.itmoapi

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokePayloadTest {
    @Test
    fun syntheticPayloadRoundTripsOnEveryTarget() {
        val payload = SmokePayload("synthetic smoke")

        val decoded = Json.decodeFromString<SmokePayload>(Json.encodeToString(payload))

        assertEquals(payload, decoded)
    }
}
