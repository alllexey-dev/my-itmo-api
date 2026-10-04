package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ItmoIdClientTest {
    private val config = ItmoIdConfiguration("https://example.invalid/realm", "synthetic-client", "https://callback.invalid/login")

    private class FixedClock : Clock {
        val instant = Instant.parse("2026-10-03T09:00:00.123Z")
        var reads = 0
        override fun now(): Instant = instant.also { reads++ }
    }

    private class Markers {
        val access = Uuid.random().toString()
        val refresh = Uuid.random().toString()
        val id = Uuid.random().toString()
        val code = Uuid.random().toString()
        val verifier = Pkce.newVerifier()
        fun body(): String = fixture("itmoid/token-success.json")
            .replace("__ACCESS__", access).replace("__REFRESH__", refresh).replace("__ID__", id)
    }

    private fun engine(body: String, status: Int = 200): MockEngine = MockEngine {
        respond(body, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }

    @Test
    fun configurationDefaultsMatchObservedMyItmoRealmAndClient() {
        val defaults = ItmoIdConfiguration()
        assertEquals("https://id.itmo.ru/auth/realms/itmo", defaults.issuer)
        assertEquals("student-personal-cabinet", defaults.clientId)
        assertEquals("https://my.itmo.ru/login/callback", defaults.redirectUri)
    }

    @Test
    fun configurationRejectsUnsafeIssuerRedirectAndBlankClientWithoutEchoingThem() {
        for (url in listOf("http://example.invalid/realm", "https://caller@example.invalid/realm", "https://example.invalid:8443/realm", "https://xn--example.invalid/realm", "https://example.invalid/realm?unused=one")) {
            val failure = assertFailsWith<IllegalArgumentException> { ItmoIdConfiguration(issuer = url) }
            assertEquals("Invalid ITMO.ID issuer", failure.message)
            assertFailsWith<IllegalArgumentException> { ItmoIdConfiguration(redirectUri = url) }
        }
        assertFailsWith<IllegalArgumentException> { ItmoIdConfiguration(clientId = "") }
    }

    @Test
    fun tokenSnapshotAndWireTypeNeverRenderSecretsAndRejectBlankSnapshotTokens() {
        val markers = Markers()
        val instant = FixedClock().instant
        assertEquals("TokenSet(redacted)", TokenSet(markers.access, instant, markers.refresh, instant, markers.id).toString())
        assertEquals("TokenWire(redacted)", TokenWire(markers.access, 300, markers.refresh, 3600, markers.id).toString())
        val failure = assertFailsWith<IllegalArgumentException> { TokenSet(markers.access, instant, "", instant, markers.id) }
        assertEquals("Incomplete token set", failure.message)
    }

    @Test
    fun authorizationUrlUsesConfiguredRealmClientRedirectStateAndS256() {
        val markers = Markers()
        val clock = FixedClock()
        val engine = engine("{}")
        val client = ItmoIdClient(engine, clock, config)
        try {
            val state = Pkce.newState()
            val challenge = Pkce.challenge(markers.verifier)
            val url = Url(client.loginUrl(challenge, state))
            assertEquals("/realm/protocol/openid-connect/auth", url.encodedPath)
            assertEquals("example.invalid", url.host)
            assertTrue(url.parameters.entries().associate { it.key to it.value.single() } == mapOf(
                "protocol" to "oauth2", "response_type" to "code", "client_id" to config.clientId,
                "redirect_uri" to config.redirectUri, "scope" to "openid profile", "state" to state,
                "code_challenge_method" to "S256", "code_challenge" to challenge,
            ), "Authorization parameters")
            assertEquals(0, clock.reads)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun exchangePreservesObservedFormAndComputesBothExpiriesFromOneClockRead() = runTest {
        val markers = Markers()
        val clock = FixedClock()
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/realm/protocol/openid-connect/token", request.url.encodedPath)
            assertEquals("example.invalid", request.url.host)
            assertTrue(request.body.contentType?.match(ContentType.Application.FormUrlEncoded) == true, "Form content type")
            assertTrue(request.headers[HttpHeaders.Authorization] == null, "No shared bearer state")
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertTrue(parseQueryString(body).entries().associate { it.key to it.value.single() } == mapOf(
                "code" to markers.code, "client_id" to config.clientId, "redirect_uri" to config.redirectUri,
                "response_type" to "code", "grant_type" to "authorization_code", "code_verifier" to markers.verifier,
            ), "Exchange form is preserved")
            respond(markers.body(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = ItmoIdClient(engine, clock, config)
        try {
            val result = client.exchange(markers.code, markers.verifier)
            assertTrue(result.accessToken == markers.access && result.refreshToken == markers.refresh && result.idToken == markers.id, "Complete token snapshot")
            assertEquals(clock.instant + 300.seconds, result.accessExpiresAt)
            assertEquals(clock.instant + 3600.seconds, result.refreshExpiresAt)
            assertEquals(1, clock.reads)
            assertEquals("TokenSet(redacted)", result.toString())
        } finally { client.close(); engine.close() }
    }

    @Test
    fun oneShotRefreshUsesPluralScopesAndHasNoImplicitRetryOrStorage() = runTest {
        val markers = Markers()
        val clock = FixedClock()
        val initialRefresh = Uuid.random().toString()
        var requests = 0
        val engine = MockEngine { request ->
            requests++
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertTrue(parseQueryString(body).entries().associate { it.key to it.value.single() } == mapOf(
                "refresh_token" to initialRefresh, "scopes" to "openid profile",
                "client_id" to config.clientId, "grant_type" to "refresh_token",
            ), "Refresh form retains scopes")
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/realm/protocol/openid-connect/token", request.url.encodedPath)
            respond(markers.body(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = ItmoIdClient(engine, clock, config)
        try {
            val result = client.refresh(initialRefresh)
            assertTrue(result.refreshToken == markers.refresh, "Refresh result retained")
            assertEquals(clock.instant + 300.seconds, result.accessExpiresAt)
            assertEquals(1, requests)
            assertEquals(1, clock.reads)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun oauthInvalidGrant400MapsAuthWithoutDescriptionOrTokens() = runTest {
        val markers = Markers()
        val clock = FixedClock()
        val engine = engine(fixture("itmoid/oauth-error.json").replace("Synthetic failure", markers.refresh), 400)
        val client = ItmoIdClient(engine, clock, config)
        try {
            val failure = assertFailsWith<MyItmoException.Auth> { client.refresh(markers.refresh) }
            assertEquals(400, failure.status)
            assertTrue(markers.refresh !in failure.message.orEmpty() && markers.refresh !in failure.toString(), "Auth diagnostics redacted")
            assertNull(failure.cause)
            assertEquals(0, clock.reads)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun oauthErrorAtHttp200StillRejectsPartialTokens() = runTest {
        val markers = Markers()
        val engine = engine(fixture("itmoid/oauth-error.json"))
        val client = ItmoIdClient(engine, FixedClock(), config)
        try {
            val failure = assertFailsWith<MyItmoException.Auth> { client.exchange(markers.code, markers.verifier) }
            assertEquals(200, failure.status)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun server503IncludingOAuthShapedBodyIsTransientHttpNeverAuth() = runTest {
        for (body in listOf("<html>unavailable</html>", fixture("itmoid/oauth-error.json"))) {
            val engine = engine(body, 503)
            val clock = FixedClock()
            val client = ItmoIdClient(engine, clock, config)
            try {
                val failure = assertFailsWith<MyItmoException.Http> { client.refresh(Uuid.random().toString()) }
                assertEquals(503, failure.status)
                assertEquals(0, clock.reads)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun nonJson200MapsDecodeWithoutBodyOrParserCause() = runTest {
        val marker = Uuid.random().toString()
        val engine = engine("<html>$marker</html>")
        val client = ItmoIdClient(engine, FixedClock(), config)
        try {
            val failure = assertFailsWith<MyItmoException.Decode> { client.refresh(marker) }
            assertTrue(marker !in failure.toString() && marker !in failure.message.orEmpty(), "Decode diagnostics redacted")
            assertNull(failure.cause)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun nonJsonOrUnknownError400And401MapHttpNotAuth() = runTest {
        for (status in listOf(400, 401)) {
            for (body in listOf("<html>unavailable</html>", "{}", """{"error":null}""", """{"error":123}""", """{"error":{}}""")) {
                val engine = engine(body, status)
                val client = ItmoIdClient(engine, FixedClock(), config)
                try {
                    assertEquals(status, assertFailsWith<MyItmoException.Http> { client.refresh(Uuid.random().toString()) }.status)
                } finally { client.close(); engine.close() }
            }
        }
    }

    @Test
    fun missingNullAndBlankTokenFieldsNeverProducePartialSnapshots() = runTest {
        val markers = Markers()
        val source = ItmoApiJson.parseToJsonElement(markers.body()) as JsonObject
        for (field in listOf("access_token", "refresh_token", "id_token", "expires_in", "refresh_expires_in")) {
            for (replacement in listOf(null, JsonNull, JsonPrimitive(""))) {
                val body = JsonObject(source.toMutableMap().apply { if (replacement == null) remove(field) else put(field, replacement) }).toString()
                val engine = engine(body)
                val clock = FixedClock()
                val client = ItmoIdClient(engine, clock, config)
                try {
                    val failure = assertFailsWith<MyItmoException.Decode> { client.refresh(markers.refresh) }
                    assertNull(failure.cause)
                    assertEquals(0, clock.reads)
                } finally { client.close(); engine.close() }
            }
        }
    }

    @Test
    fun negativeAndOverflowingLifetimesAreRejectedBeforeClockRead() = runTest {
        val markers = Markers()
        val source = ItmoApiJson.parseToJsonElement(markers.body()) as JsonObject
        for (field in listOf("expires_in", "refresh_expires_in")) {
            for (value in listOf(-1L, Long.MAX_VALUE)) {
                val engine = engine(JsonObject(source + (field to JsonPrimitive(value))).toString())
                val clock = FixedClock()
                val client = ItmoIdClient(engine, clock, config)
                try {
                    assertFailsWith<MyItmoException.Decode> { client.refresh(markers.refresh) }
                    assertEquals(0, clock.reads)
                } finally { client.close(); engine.close() }
            }
        }
    }

    @Test
    fun instantSaturationCannotProduceInvalidExpirySnapshots() = runTest {
        val markers = Markers()
        val engine = engine(markers.body())
        val client = ItmoIdClient(engine, object : Clock {
            override fun now(): Instant = Instant.fromEpochSeconds(Long.MAX_VALUE)
        }, config)
        try { assertFailsWith<MyItmoException.Decode> { client.refresh(markers.refresh) } }
        finally { client.close(); engine.close() }
    }

    @Test
    fun ioFailurePreservesCauseChainAndNeverMapsAuth() = runTest {
        val original = IOException("Synthetic connectivity failure")
        val engine = MockEngine { throw IllegalStateException("Synthetic engine failure", original) }
        val client = ItmoIdClient(engine, FixedClock(), config)
        try {
            val failure = assertFailsWith<MyItmoException.Network> { client.refresh(Uuid.random().toString()) }
            assertTrue(generateSequence(failure.cause) { it.cause }.any { it === original }, "Original IOException preserved")
        } finally { client.close(); engine.close() }
    }

    @Test
    fun cancellationPropagatesWithoutAuthenticationMapping() = runTest {
        val engine = MockEngine { throw CancellationException("Cancelled") }
        val client = ItmoIdClient(engine, FixedClock(), config)
        try { assertFailsWith<CancellationException> { client.refresh(Uuid.random().toString()) } }
        finally { client.close(); engine.close() }
    }

    @Test
    fun redirectCannotLeakFormTokensToAnotherRequest() = runTest {
        var requests = 0
        val engine = MockEngine {
            requests++
            respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://untrusted.invalid/"))
        }
        val client = ItmoIdClient(engine, FixedClock(), config)
        try {
            assertEquals(302, assertFailsWith<MyItmoException.Http> { client.refresh(Uuid.random().toString()) }.status)
            assertEquals(1, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun blankInputsAndInvalidVerifierFailWithoutNetworkOrClockRead() = runTest {
        val clock = FixedClock()
        val engine = MockEngine { error("Invalid inputs must not issue a request") }
        val client = ItmoIdClient(engine, clock, config)
        try {
            assertFailsWith<MyItmoException.Decode> { client.refresh("") }
            assertFailsWith<MyItmoException.Decode> { client.exchange("", Pkce.newVerifier()) }
            assertFailsWith<MyItmoException.Decode> { client.exchange(Uuid.random().toString(), "invalid") }
            assertEquals(0, clock.reads)
        } finally { client.close(); engine.close() }
    }
}
