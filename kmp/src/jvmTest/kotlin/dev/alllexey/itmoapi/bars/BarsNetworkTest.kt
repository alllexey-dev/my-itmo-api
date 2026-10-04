package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarsNetworkTest {
    @Test
    fun ioFailureIsTypedWithoutErasingTheSessionOrExposingItsHeaderInDiagnostics() = runTest {
        val sentinel = "synthetic-sensitive-network-sentinel"
        val header = "Bearer synthetic-existing-session"
        val storage = RuntimeBarsStorage().also { it.setAuthorization(header) }
        val engine = MockEngine { throw IOException(sentinel) }
        val client = BarsClient(engine, BarsConfiguration(restUrl = Url("https://synthetic.invalid/backend/rest/")), storage)
        try {
            val error = assertFailsWith<MyItmoException.Network> { client.getCurrentUser() }
            assertFalse(sentinel in error.toString())
            assertFalse(header in error.toString())
            assertTrue(storage.getAuthorization() == header)
        } finally { client.close(); engine.close() }
    }
}
