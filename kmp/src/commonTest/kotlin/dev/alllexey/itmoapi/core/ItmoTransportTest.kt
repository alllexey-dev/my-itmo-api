package dev.alllexey.itmoapi.core

import dev.alllexey.itmoapi.testing.fixture
import dev.alllexey.itmoapi.testing.mockClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItmoTransportTest {
    private val baseUrl = Url("https://example.invalid/api/")

    private suspend fun <T> exchange(
        body: String,
        status: Int,
        decode: suspend (ItmoTransport) -> T,
    ): T {
        val mock = mockClient { expect(HttpMethod.Get, "/api/sample") { respond(body, status) } }
        try {
            return decode(ItmoTransport(mock.client, baseUrl))
        } finally {
            mock.assertComplete()
            mock.close()
        }
    }

    @Test
    fun resultEnvelopeSuccessDecodes() = runTest {
        val result = exchange("""{"error_code":0,"result":7}""", 200) {
            it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
        }
        assertEquals(7, result.requireResult())
    }

    @Test
    fun dataEnvelopeSuccessDecodes() = runTest {
        val result = exchange("""{"code":0,"data":7}""", 200) {
            it.execute(DataResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
        }
        assertEquals(7, result.requireResult())
    }

    @Test
    fun simpleEnvelopeSuccessDecodes() = runTest {
        val result = exchange("""{"response":7}""", 200) {
            it.execute(SimpleResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
        }
        assertEquals(7, result.requireResult())
    }

    @Test
    fun nonJson502MapsStatusBeforePayloadDecoding() = runTest {
        val failure = assertFailsWith<MyItmoException.Http> {
            exchange(fixture("errors/non-json-502.html"), 502) {
                it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(502, failure.status)
        assertNull(failure.cause)
    }

    @Test
    fun errorCodeAtHttp200MapsApi() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            exchange(fixture("errors/error-code-200.json"), 200) {
                it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(200, failure.status)
        assertEquals(3, failure.errorCode)
        assertEquals("Синтетическая ошибка", failure.serverMessage)
    }

    @Test
    fun errorCodeAtHttp400RetainsCode100() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            exchange("""{"error_code":100,"error_message":"untrusted marker","result":null}""", 400) {
                it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
        assertEquals("untrusted marker", failure.serverMessage)
        assertTrue("untrusted marker" !in failure.message.orEmpty() && "untrusted marker" !in failure.toString())
        assertNull(failure.cause)
    }

    @Test
    fun malformedErrorPayloadDoesNotHideTheApiCode() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            exchange("""{"error_code":100,"result":{"unexpected":"shape"}}""", 400) {
                it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(100, failure.errorCode)
    }

    @Test
    fun dataErrorAtHttp200MapsApi() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            exchange("""{"code":9,"message":"untrusted marker","data":null}""", 200) {
                it.execute(DataResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(9, failure.errorCode)
        assertEquals("untrusted marker", failure.serverMessage)
        assertTrue("untrusted marker" !in failure.message.orEmpty())
        assertNull(failure.cause)
        assertTrue("untrusted marker" !in failure.toString())
    }

    @Test
    fun onlyStringReasonsFromTheMatchingEnvelopeAreRetained() = runTest {
        for ((codeKey, messageKey) in listOf("error_code" to "error_message", "code" to "message")) {
            for (value in listOf(null, "null", "137", "true", "{}", "[]", "\"no capacity\"", "\"\"")) {
                val otherKey = if (messageKey == "message") "error_message" else "message"
                val field = value?.let { ",\"$messageKey\":$it" }.orEmpty() + ",\"$otherKey\":\"untrusted marker\""
                val failure = assertFailsWith<MyItmoException.Api> {
                    exchange("{\"$codeKey\":137$field}", 200) {
                        it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
                    }
                }
                assertEquals(137, failure.errorCode)
                assertEquals(value?.takeIf { it.startsWith('"') }?.removeSurrounding("\""), failure.serverMessage)
            }
        }
    }

    @Test
    fun simpleEnvelopeWithHttp400MapsHttp() = runTest {
        val failure = assertFailsWith<MyItmoException.Http> {
            exchange("""{"response":null}""", 400) {
                it.execute(SimpleResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(400, failure.status)
    }

    @Test
    fun unauthenticatedStatusMapsAuthForEveryEnvelope() = runTest {
        val decoders = listOf(ResultResponse.serializer(Int.serializer()), DataResponse.serializer(Int.serializer()), SimpleResponse.serializer(Int.serializer()))
        for (status in listOf(401, 403)) {
            decoders.forEach { decoder ->
                val failure = assertFailsWith<MyItmoException.Auth> {
                    exchange("{}", status) { it.execute(decoder, HttpMethod.Get, "sample") }
                }
                assertEquals(status, failure.status)
            }
        }
    }

    @Test
    fun specificApiErrorTakesPrecedenceOverAuthenticationStatus() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            exchange("""{"error_code":100,"result":null}""", 401) {
                it.execute(ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "sample")
            }
        }
        assertEquals(401, failure.status)
        assertEquals(100, failure.errorCode)
    }

    @Test
    fun malformedSuccessBodyMapsDecodeWithoutParserCauseOrBody() = runTest {
        for (body in listOf("untrusted marker", """{"response":"untrusted marker"}""")) {
            val failure = assertFailsWith<MyItmoException.Decode> {
                exchange(body, 200) { it.execute(SimpleResponse.serializer(Int.serializer()), HttpMethod.Get, "sample") }
            }
            assertNull(failure.cause)
            assertTrue("untrusted marker" !in failure.toString())
        }
    }

    @Test
    fun directIoFailureRetainsOriginalCause() = runTest {
        val original = IOException("untrusted marker")
        val engine = MockEngine { throw original }
        val client = createItmoHttpClient(engine)
        try {
            val failure = assertFailsWith<MyItmoException.Network> {
                ItmoTransport(client, baseUrl).execute(Int.serializer(), HttpMethod.Get, "sample")
            }
            assertTrue(generateSequence(failure.cause) { it.cause }.any { it === original })
            assertTrue("untrusted marker" !in failure.toString())
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun wrappedIoFailureRetainsOriginalCauseChain() = runTest {
        val io = IOException("untrusted marker")
        val original = IllegalStateException("untrusted marker", io)
        val engine = MockEngine { throw original }
        val client = createItmoHttpClient(engine)
        try {
            val failure = assertFailsWith<MyItmoException.Network> {
                ItmoTransport(client, baseUrl).execute(Int.serializer(), HttpMethod.Get, "sample")
            }
            assertTrue(generateSequence(failure.cause) { it.cause }.any { it === original })
            assertTrue(generateSequence(failure.cause) { it.cause }.any { it === io })
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun cancellationIsNeverWrapped() = runTest {
        val cancellation = CancellationException("cancelled")
        val engine = MockEngine { throw cancellation }
        val client = createItmoHttpClient(engine)
        try {
            val failure = assertFailsWith<CancellationException> {
                ItmoTransport(client, baseUrl).execute(Int.serializer(), HttpMethod.Get, "sample")
            }
            assertTrue(generateSequence<Throwable>(failure) { it.cause }.any { it === cancellation })
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun nonIoEngineFailureMapsDecodeWithoutLeakingDiagnostics() = runTest {
        val engine = MockEngine { throw IllegalArgumentException("untrusted marker") }
        val client = createItmoHttpClient(engine)
        try {
            val failure = assertFailsWith<MyItmoException.Decode> {
                ItmoTransport(client, baseUrl).execute(Int.serializer(), HttpMethod.Get, "sample")
            }
            assertNull(failure.cause)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun harnessAssertsRepeatedQueriesIsoDatesHeadersAndBodyOnDelete() = runTest {
        val mock = mockClient {
            expect(HttpMethod.Delete, "/api/sign/schedule/lessons") {
                query("sport_type_id", "1", "2")
                query("date_start", "2026-01-05")
                query("date_end", "2026-01-11")
                header("Accept-Language", "ru")
                body("[7,8]")
                respond("""{"response":true}""")
            }
        }
        try {
            val response = ItmoTransport(mock.client, baseUrl).execute(SimpleResponse.serializer(Boolean.serializer()), HttpMethod.Delete, "sign/schedule/lessons") {
                parameter("sport_type_id", "1")
                parameter("sport_type_id", "2")
                parameter("date_start", LocalDate(2026, 1, 5).toQueryValue())
                parameter("date_end", LocalDate(2026, 1, 11).toQueryValue())
                header("Accept-Language", "ru")
                contentType(ContentType.Application.Json)
                setBody(listOf(7L, 8L))
            }
            assertTrue(response.requireResult())
            mock.assertComplete()
        } finally {
            mock.close()
        }
    }
}
