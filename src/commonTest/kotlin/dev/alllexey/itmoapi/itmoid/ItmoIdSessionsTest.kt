package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ItmoIdSessionsTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-22T09:00:00Z")
    }
    private val cookie = "KEYCLOAK_IDENTITY=${Uuid.random()}"
    private val bearer = Uuid.random().toString()
    private val sid = "00000000-0000-4000-8000-0000000000a1"

    private fun sessions(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): Pair<ItmoIdSessions, MockEngine> {
        val engine = MockEngine(handler)
        return ItmoIdSessions(engine, clock) to engine
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun MockRequestHandleScope.redirect(location: String) =
        respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, location))

    private fun form(request: HttpRequestData): Map<String, String> =
        parseQueryString((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).entries().associate { it.key to it.value.single() }

    @Test
    fun profileTokenAuthorizesProfileClientWithCookieAndExchangesCheckedCode() = runTest {
        val access = Uuid.random().toString()
        val code = Uuid.random().toString()
        var challenge = ""
        var requests = 0
        val (client, engine) = sessions { request ->
            requests++
            when (request.url.encodedPath) {
                "/auth/realms/itmo/protocol/openid-connect/auth" -> {
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("id.itmo.ru", request.url.host)
                    assertTrue(request.headers[HttpHeaders.Cookie] == cookie, "Caller cookie sent")
                    assertNull(request.headers[HttpHeaders.Authorization])
                    val query = request.url.parameters
                    assertEquals("profile", query["client_id"])
                    assertEquals("https://id.itmo.ru/login/callback", query["redirect_uri"])
                    assertEquals("openid profile", query["scope"])
                    assertEquals("S256", query["code_challenge_method"])
                    challenge = query["code_challenge"]!!
                    redirect("https://id.itmo.ru/login/callback?state=${query["state"]}&session_state=x&code=$code")
                }
                "/auth/realms/itmo/protocol/openid-connect/token" -> {
                    val form = form(request)
                    assertEquals("profile", form["client_id"])
                    assertEquals("https://id.itmo.ru/login/callback", form["redirect_uri"])
                    assertTrue(form["code"] == code, "Callback code exchanged")
                    assertEquals(challenge, Pkce.challenge(form.getValue("code_verifier")))
                    json(fixture("itmoid/token-success.json").replace("__ACCESS__", access).replace("__REFRESH__", "r").replace("__ID__", "i"))
                }
                else -> error("Unexpected request")
            }
        }
        try {
            assertTrue(client.profileToken(cookie) == access, "Profile access token")
            assertEquals(2, requests)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun deadOrBlankCookieIsNullNotAuth() = runTest {
        for (answer in listOf<MockRequestHandleScope.() -> HttpResponseData>(
            { respond("<html>sign in</html>", HttpStatusCode.OK) },
            { redirect("https://id.itmo.ru/auth/realms/itmo/login-actions/required-action?execution=x") },
        )) {
            val (client, engine) = sessions { answer() }
            try { assertNull(client.profileToken(cookie)) } finally { client.close(); engine.close() }
        }
        val (client, engine) = sessions { error("Blank cookies must not issue a request") }
        try {
            assertNull(client.profileToken(null))
            assertNull(client.profileToken(" "))
        } finally { client.close(); engine.close() }
    }

    @Test
    fun foreignRedirectOrUncheckedCodeIsDecodeAndNeverExchanged() = runTest {
        val code = Uuid.random().toString()
        for (location in listOf(
            "https://untrusted.invalid/login/callback?state=x&code=$code",
            "https://id.itmo.ru/login/callback?state=wrong&code=$code",
            "https://id.itmo.ru/login/callback?error=access_denied",
            "https://my.itmo.ru/login/callback?code=$code",
        )) {
            var requests = 0
            val (client, engine) = sessions { requests++; redirect(location) }
            try {
                assertFailsWith<MyItmoException.Decode> { client.profileToken(cookie) }
                assertEquals(1, requests)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun authorizeFailuresAreTransient() = runTest {
        val (client, engine) = sessions { respond("", HttpStatusCode.ServiceUnavailable) }
        try { assertEquals(503, assertFailsWith<MyItmoException.Http> { client.profileToken(cookie) }.status) }
        finally { client.close(); engine.close() }
        val original = IOException("Synthetic connectivity failure")
        val (offline, offlineEngine) = sessions { throw original }
        try {
            val failure = assertFailsWith<MyItmoException.Network> { offline.profileToken(cookie) }
            assertTrue(generateSequence(failure.cause) { it.cause }.any { it === original }, "Original IOException preserved")
        } finally { offline.close(); offlineEngine.close() }
    }

    @Test
    fun listSendsBearerAndDecodesBothBrowserShapes() = runTest {
        val (client, engine) = sessions { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("https://id.itmo.ru/api/profile/sessions", request.url.toString())
            assertTrue(request.headers[HttpHeaders.Authorization] == "Bearer $bearer", "Bearer sent")
            assertEquals("application/json", request.headers[HttpHeaders.Accept])
            assertNull(request.headers[HttpHeaders.Cookie])
            json(fixture("itmoid/sessions.json"))
        }
        try {
            val (agent, phone, bare) = client.list(bearer)
            assertEquals(sid, agent.sid)
            assertEquals(Instant.fromEpochSeconds(1790000000), agent.started)
            assertEquals(Instant.fromEpochSeconds(1790003600), agent.lastAccess)
            assertEquals(Instant.fromEpochSeconds(1797776000), agent.expires)
            assertEquals("Linux", agent.os)
            assertEquals("ITMO Widgets agent bot/1.0", agent.browser)
            assertEquals("192.0.2.10", agent.ipAddress)
            assertEquals(false, agent.mobile)
            assertEquals("Chrome Mobile WebView/141.0.0", phone.browser)
            assertEquals(true, phone.mobile)
            assertEquals("", bare.os)
            assertEquals("", bare.browser)
            assertEquals("", bare.ipAddress)
            assertEquals("ItmoIdSession(redacted)", agent.toString())
        } finally { client.close(); engine.close() }
    }

    @Test
    fun listMaps401ToAuthAndOtherFailuresToHttpOrDecode() = runTest {
        for ((status, body) in listOf(401 to "{}", 500 to "{}", 200 to "<html></html>", 200 to "[{}]")) {
            val (client, engine) = sessions { json(body, HttpStatusCode.fromValue(status)) }
            try {
                val failure = assertFailsWith<MyItmoException> { client.list(bearer) }
                when (status) {
                    401 -> assertEquals(401, (failure as MyItmoException.Auth).status)
                    500 -> assertEquals(500, (failure as MyItmoException.Http).status)
                    else -> assertTrue(failure is MyItmoException.Decode, "Decode")
                }
                assertTrue(bearer !in failure.toString(), "Diagnostics redacted")
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun endDeletesWithJsonAcceptAndTreatsGoneAsSuccess() = runTest {
        for (status in listOf(HttpStatusCode.NoContent, HttpStatusCode.NotFound)) {
            var requests = 0
            val (client, engine) = sessions { request ->
                requests++
                assertEquals(HttpMethod.Delete, request.method)
                assertEquals("https://id.itmo.ru/api/account/sessions/$sid", request.url.toString())
                assertEquals("application/json", request.headers[HttpHeaders.Accept])
                assertTrue(request.headers[HttpHeaders.Authorization] == "Bearer $bearer", "Bearer sent")
                respond("", status)
            }
            try {
                client.end(bearer, sid)
                assertEquals(1, requests)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun endFailuresAndInvalidSessionIds() = runTest {
        for (status in listOf(401, 405, 503)) {
            val (client, engine) = sessions { respond("", HttpStatusCode.fromValue(status)) }
            try {
                val failure = assertFailsWith<MyItmoException> { client.end(bearer, sid) }
                assertTrue(if (status == 401) failure is MyItmoException.Auth else failure is MyItmoException.Http, "Status mapping")
                assertTrue(sid !in failure.toString(), "Diagnostics redacted")
            } finally { client.close(); engine.close() }
        }
        val base64url = "pt6fMi91_abc-DEF0123456Q"
        val (sending, sendingEngine) = sessions { request ->
            assertEquals("https://id.itmo.ru/api/account/sessions/$base64url", request.url.toString())
            respond("", HttpStatusCode.NoContent)
        }
        try {
            sending.end(bearer, base64url)
        } finally { sending.close(); sendingEngine.close() }
        val (client, engine) = sessions { error("Invalid input must not issue a request") }
        try {
            for (bad in listOf("", "../profile", "a/b", "x?y", "a".repeat(65))) {
                val failure = assertFailsWith<IllegalArgumentException> { client.end(bearer, bad) }
                assertEquals("Invalid session id", failure.message)
            }
            assertFailsWith<IllegalArgumentException> { client.end("", sid) }
            assertFailsWith<IllegalArgumentException> { client.list(" ") }
        } finally { client.close(); engine.close() }
    }
}
