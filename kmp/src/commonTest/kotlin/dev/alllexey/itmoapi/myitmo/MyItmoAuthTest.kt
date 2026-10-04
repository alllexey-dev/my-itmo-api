package dev.alllexey.itmoapi.myitmo

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.ItmoIdClient
import dev.alllexey.itmoapi.itmoid.TokenManager
import dev.alllexey.itmoapi.itmoid.TokenTestClock
import dev.alllexey.itmoapi.itmoid.TokenTestStorage
import dev.alllexey.itmoapi.itmoid.testTokens
import dev.alllexey.itmoapi.itmoid.tokenBody
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MyItmoAuthTest {
    private val clock = TokenTestClock()

    private fun authenticated(engine: MockEngine, storage: TokenTestStorage): HttpClient {
        val identity = ItmoIdClient(engine, clock)
        return HttpClient(engine) {
            followRedirects = false
            install(MyItmoAuth) {
                tokens = TokenManager(storage, identity, clock)
                configuration = MyItmoConfiguration.DEFAULT
            }
        }
    }

    @Test
    fun myItmoRequestsCarryBearerAndConfiguredLanguage() = runTest {
        val snapshot = testTokens(clock)
        val engine = MockEngine { request ->
            assertTrue(request.headers[HttpHeaders.Authorization] == "Bearer ${snapshot.accessToken}", "Bearer attached")
            assertEquals("ru", request.headers[HttpHeaders.AcceptLanguage])
            respond("{}")
        }
        val client = authenticated(engine, TokenTestStorage(snapshot))
        try { client.get("https://my.itmo.ru/api/personalities/persons?q=x") }
        finally { client.close(); engine.close() }
    }

    @Test
    fun explicitLanguageOnRequestIsKept() = runTest {
        val engine = MockEngine { request ->
            assertEquals("en", request.headers[HttpHeaders.AcceptLanguage])
            respond("{}")
        }
        val client = authenticated(engine, TokenTestStorage(testTokens(clock)))
        try { client.get("https://my.itmo.ru/api/personalities/persons") { headers.append(HttpHeaders.AcceptLanguage, "en") } }
        finally { client.close(); engine.close() }
    }

    @Test
    fun foreignHostsAreLeftUntouchedEvenWithMissingStorageAndUnauthorizedResponses() = runTest {
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            assertNull(request.headers[HttpHeaders.Authorization])
            assertNull(request.headers[HttpHeaders.AcceptLanguage])
            respondError(HttpStatusCode.Unauthorized)
        }
        val client = authenticated(engine, TokenTestStorage())
        try {
            val foreign = listOf(
                "https://foreign.invalid/api", "https://my.itmo.ru.foreign.invalid/api",
                "https://id.itmo.ru/auth", "http://my.itmo.ru/api", "https://my.itmo.ru:8443/api",
                "https://caller@my.itmo.ru/api", "https://qr.itmo.su.foreign.invalid/api",
            )
            foreign.forEach { client.get(it) }
            assertEquals(foreign.size, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun explicitAuthorizationWinsWithoutReadingStorageRefreshingOrDuplicatingHeaders() = runTest {
        val explicit = testTokens(clock).accessToken
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            assertTrue(request.headers.getAll(HttpHeaders.Authorization) == listOf("Bearer $explicit"), "One caller header")
            assertEquals("en", request.headers[HttpHeaders.AcceptLanguage])
            respondError(HttpStatusCode.Unauthorized)
        }
        val client = authenticated(engine, TokenTestStorage())
        try {
            client.get("https://my.itmo.ru/api") {
                headers.append(HttpHeaders.Authorization, "Bearer $explicit")
                headers.append(HttpHeaders.AcceptLanguage, "en")
            }
            assertEquals(1, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun qrHostGetsBearerIndependentOfConfiguredMyItmoOrigin() = runTest {
        val snapshot = testTokens(clock)
        val engine = MockEngine { request ->
            assertEquals("qr.itmo.su", request.url.host)
            assertTrue(request.headers[HttpHeaders.Authorization] == "Bearer ${snapshot.accessToken}", "QR bearer")
            assertEquals("ru", request.headers[HttpHeaders.AcceptLanguage])
            respond("""{"response":{"qr_hex":"001122"}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = MyItmoClient(MyItmoConfiguration.DEV, TokenTestStorage(snapshot), engine, clock)
        try { assertEquals("001122", client.qr.getQrCode().response?.qrHex) }
        finally { client.close(); engine.close() }
    }

    @Test
    fun unauthorizedRefreshesAndRetriesOnceWithoutLeakingBearerToIdentity() = runTest {
        val original = testTokens(clock)
        val rotation = testTokens(clock)
        val storage = TokenTestStorage(original)
        var apiCalls = 0
        var refreshes = 0
        val engine = MockEngine { request ->
            if (request.url.host == "id.itmo.ru") {
                refreshes++
                assertNull(request.headers[HttpHeaders.Authorization])
                assertNull(request.headers[HttpHeaders.AcceptLanguage])
                respond(tokenBody(rotation), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                apiCalls++
                val expected = if (apiCalls == 1) original.accessToken else rotation.accessToken
                assertTrue(request.headers.getAll(HttpHeaders.Authorization) == listOf("Bearer $expected"), "One bearer per attempt")
                assertEquals("ru", request.headers[HttpHeaders.AcceptLanguage])
                if (apiCalls == 1) respondError(HttpStatusCode.Unauthorized)
                else respond("""{"response":{"qr_hex":"001122"}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = MyItmoClient(MyItmoConfiguration.DEFAULT, storage, engine, clock)
        try {
            assertEquals("001122", client.qr.getQrCode().response?.qrHex)
            assertEquals(1, refreshes)
            assertEquals(2, apiCalls)
            assertEquals(1, storage.writes)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun secondUnauthorizedIsAuthEvenWithApiErrorEnvelopeAndRedactedDiagnostics() = runTest {
        val snapshot = testTokens(clock)
        var apiCalls = 0
        var refreshes = 0
        val engine = MockEngine { request ->
            if (request.url.host == "id.itmo.ru") {
                refreshes++
                respond(tokenBody(testTokens(clock)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                apiCalls++
                respond("""{"error_code":5,"error_message":"${snapshot.accessToken}"}""", HttpStatusCode.Unauthorized)
            }
        }
        val client = MyItmoClient(MyItmoConfiguration.DEFAULT, TokenTestStorage(snapshot), engine, clock)
        try {
            val failure = assertFailsWith<MyItmoException.Auth> { client.qr.getQrCode() }
            assertEquals(401, failure.status)
            assertFalse(failure.toString().contains(snapshot.accessToken))
            assertNull(failure.cause)
            assertEquals(1, refreshes)
            assertEquals(2, apiCalls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun expiredRefreshAndTransientIdentityFailureRemainTypedAtAreaBoundary() = runTest {
        for (rejected in listOf(true, false)) {
            val snapshot = testTokens(clock, expired = true)
            val storage = TokenTestStorage(snapshot)
            val engine = MockEngine { request ->
                assertEquals("id.itmo.ru", request.url.host)
                if (rejected) respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest)
                else respond("{}", HttpStatusCode.ServiceUnavailable)
            }
            val client = MyItmoClient(MyItmoConfiguration.DEFAULT, storage, engine, clock)
            try {
                if (rejected) assertFailsWith<MyItmoException.Auth> { client.qr.getQrCode() }
                else assertEquals(503, assertFailsWith<MyItmoException.Http> { client.qr.getQrCode() }.status)
                assertEquals(0, storage.writes)
                assertTrue(storage.snapshot === snapshot)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun tenAreaCallsOnExpiredAccessCauseOneRefresh() = runTest {
        var refreshes = 0
        var apiCalls = 0
        val engine = MockEngine { request ->
            if (request.url.host == "id.itmo.ru") {
                refreshes++
                delay(10)
                respond(tokenBody(testTokens(clock)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                apiCalls++
                respond("""{"response":{"qr_hex":"001122"}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = MyItmoClient(MyItmoConfiguration.DEFAULT, TokenTestStorage(testTokens(clock, expired = true)), engine, clock)
        try {
            List(10) { async { client.qr.getQrCode() } }.awaitAll()
            assertEquals(1, refreshes)
            assertEquals(10, apiCalls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun tenConcurrentUnauthorizedResponsesReuseOneForcedRotation() = runTest {
        val original = testTokens(clock)
        val rotation = testTokens(clock)
        var refreshes = 0
        var rejectedCalls = 0
        var successfulCalls = 0
        val engine = MockEngine { request ->
            if (request.url.host == "id.itmo.ru") {
                refreshes++
                delay(10)
                respond(tokenBody(rotation), headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else if (request.headers[HttpHeaders.Authorization] == "Bearer ${original.accessToken}") {
                rejectedCalls++
                respondError(HttpStatusCode.Unauthorized)
            } else {
                successfulCalls++
                assertTrue(request.headers[HttpHeaders.Authorization] == "Bearer ${rotation.accessToken}", "Rotation reused")
                respond("""{"response":{"qr_hex":"001122"}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = MyItmoClient(MyItmoConfiguration.DEFAULT, TokenTestStorage(original), engine, clock)
        try {
            List(10) { async { client.qr.getQrCode() } }.awaitAll()
            assertEquals(1, refreshes)
            assertEquals(10, rejectedCalls)
            assertEquals(10, successfulCalls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun endedSessionStaysAuthAtAreaBoundaryWithoutMakingRequests() = runTest {
        val expired = testTokens(clock, expired = true)
        clock.instant = expired.refreshExpiresAt
        for (snapshot in listOf(null, expired)) {
            val engine = MockEngine { error("Ended sessions must not make network requests") }
            val storage = TokenTestStorage(snapshot)
            val client = MyItmoClient(MyItmoConfiguration.DEFAULT, storage, engine, clock)
            try {
                assertFailsWith<MyItmoException.Auth> { client.qr.getQrCode() }
                assertTrue(storage.snapshot === snapshot)
                assertEquals(0, storage.writes)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun redirectsDoNotReplayBearerToForeignHost() = runTest {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://foreign.invalid/api"))
        }
        val client = MyItmoClient(MyItmoConfiguration.DEFAULT, TokenTestStorage(testTokens(clock)), engine, clock)
        try {
            assertEquals(302, assertFailsWith<MyItmoException.Http> { client.qr.getQrCode() }.status)
            assertEquals(1, requests)
        } finally { client.close(); engine.close() }
    }
}
