package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.qr.*
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class QrParityTest {
    @Test
    fun `pass wire tree`() {
        assertParity<api.myitmo.model.SimpleResponse<api.myitmo.model.other.QrData>, SimpleResponse<QrData>>(
            "qr/pass.json",
            SimpleResponse.serializer(QrData.serializer()),
        )
    }

    @Test
    fun `unreviewed fields remain visible`() {
        val original = JsonObject(mapOf("unexpected" to JsonPrimitive("")))
        assertEquals(original, IntendedDifferences.normalize("studyplan/absence.json", original))
        assertNotEquals(JsonObject(emptyMap()), IntendedDifferences.normalize("qr/pass.json", original))
    }
}
