package dev.alllexey.itmoapi

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.itmoid.InMemoryTokenStorage
import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import dev.alllexey.itmoapi.itmoid.PasswordLogin
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MyItmoTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-12T09:00:00Z")
    }
    private val identity = ItmoIdConfiguration("https://issuer.invalid/realm", "synthetic-client", "https://callback.invalid/login")
    private val configuration = MyItmoConfiguration(Url("https://api.invalid"), identity)
    private val username = "synthetic-user"
    private val password = Uuid.random().toString()
    private val tokens = fixture("itmoid/token-success.json")
        .replace("__ACCESS__", "synthetic-access").replace("__REFRESH__", "synthetic-refresh").replace("__ID__", "synthetic-id")

    /** Scripted ITMO.ID: login page, form post, token exchange; MyITMO and BARS answer after it. */
    private class Itmo(val formAnswer: MockRequestHandleScope.(state: String) -> HttpResponseData) {
        var state: String? = null
        var barsState: String? = null
        val seen = mutableListOf<HttpRequestData>()
    }

    private fun form(request: HttpRequestData): Map<String, String> =
        parseQueryString((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).let { query ->
            query.names().associateWith { query[it].orEmpty() }
        }

    private fun engine(itmo: Itmo, page: String = fixture("itmoid/login-page.html")) = MockEngine { request ->
        itmo.seen += request
        val path = request.url.encodedPath
        when {
            path == "/realm/protocol/openid-connect/auth" && request.url.parameters["client_id"] == "bars" -> {
                itmo.barsState = request.url.parameters["state"]
                respond("", HttpStatusCode.Found, headersOf(
                    HttpHeaders.Location to listOf("https://bars.itmo.ru/rest/login?code=synthetic-bars-code&state=${itmo.barsState}"),
                    HttpHeaders.SetCookie to listOf("KEYCLOAK_IDENTITY=synthetic-rotated; Path=/realm/; Secure"),
                ))
            }
            path == "/realm/protocol/openid-connect/auth" -> {
                itmo.state = request.url.parameters["state"]
                respond(page, headers = headersOf(HttpHeaders.SetCookie, listOf(
                    "AUTH_SESSION_ID=synthetic-auth-session; Path=/realm/; Secure; HttpOnly",
                    "KC_RESTART=synthetic-restart; Path=/realm/; Secure; HttpOnly",
                )))
            }
            path == "/realm/login-actions/authenticate" -> itmo.formAnswer(this, requireNotNull(itmo.state))
            path == "/realm/protocol/openid-connect/token" -> respond(tokens)
            path == "/api/schedule/schedule/personal" -> respond(fixture("schedule/empty.json"))
            path == "/backend/rest/login" -> respond("", headers = headersOf(HttpHeaders.Authorization, "Bearer synthetic-bars-session"))
            path == "/backend/rest/journal/disciplines" -> respond(fixture("bars/disciplines.json"))
            else -> error("Unexpected request")
        }
    }

    private val signedIn: MockRequestHandleScope.(String) -> HttpResponseData = { state ->
        respond("", HttpStatusCode.Found, headersOf(
            HttpHeaders.Location to listOf("https://callback.invalid/login?state=$state&session_state=synthetic&iss=https%3A%2F%2Fissuer.invalid%2Frealm&code=synthetic-code"),
            HttpHeaders.SetCookie to listOf(
                "KEYCLOAK_IDENTITY=synthetic-identity; Path=/realm/; Secure; HttpOnly",
                "KC_RESTART=; Max-Age=0; Path=/realm/; Secure; HttpOnly",
            ),
        ))
    }

    @Test
    fun passwordLoginPostsTheFormOnceExchangesTheCodeAndCallsMyItmo() = runTest {
        val itmo = Itmo(signedIn)
        val engine = engine(itmo)
        val storage = InMemoryTokenStorage()
        MyItmo.create(engine, false, storage, configuration, clock).signIn(username, password).use { client ->
            val page = itmo.seen[0]
            assertEquals(HttpMethod.Get, page.method)
            assertEquals("synthetic-client", page.url.parameters["client_id"])
            assertEquals("S256", page.url.parameters["code_challenge_method"])
            assertNull(page.headers[HttpHeaders.Cookie])

            val post = itmo.seen[1]
            assertEquals(HttpMethod.Post, post.method)
            assertEquals("synthetic-session", post.url.parameters["session_code"])
            assertEquals("synthetic-tab", post.url.parameters["tab_id"])
            assertEquals("AUTH_SESSION_ID=synthetic-auth-session; KC_RESTART=synthetic-restart", post.headers[HttpHeaders.Cookie])
            assertEquals(mapOf("username" to username, "password" to password, "rememberMe" to "on"), form(post))

            val exchange = form(itmo.seen[2])
            assertEquals("synthetic-code", exchange["code"])
            assertEquals("authorization_code", exchange["grant_type"])
            assertEquals(43, exchange["code_verifier"]?.length)
            assertEquals("synthetic-refresh", storage.read()?.refreshToken)

            assertEquals(emptyList(), client.schedule.getPersonalSchedule(LocalDate(2026, 10, 12), LocalDate(2026, 10, 18)).requireResult())
            assertEquals("Bearer synthetic-access", itmo.seen.last().headers[HttpHeaders.Authorization])
            assertFalse(itmo.seen.drop(2).any { password in (it.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty() })
        }
    }

    @Test
    fun barsSignsInWithTheItmoIdCookiesOfThePasswordLogin() = runTest {
        val itmo = Itmo(signedIn)
        val engine = engine(itmo)
        MyItmo.create(engine, false, InMemoryTokenStorage(), configuration, clock).signIn(username, password).use { client ->
            assertTrue(client.bars.getDisciplines().isNotEmpty())
            val barsAuth = itmo.seen.first { it.url.parameters["client_id"] == "bars" }
            // KC_RESTART was expired by the form answer; the SSO identity cookie is replayed.
            assertEquals("AUTH_SESSION_ID=synthetic-auth-session; KEYCLOAK_IDENTITY=synthetic-identity", barsAuth.headers[HttpHeaders.Cookie])
            assertEquals("synthetic-bars-code", itmo.seen.first { it.url.encodedPath == "/backend/rest/login" }.url.parameters["code"])
            assertEquals("Bearer synthetic-bars-session", itmo.seen.last().headers[HttpHeaders.Authorization])
            assertTrue(client.bars.hasSession())
        }
    }

    @Test
    fun wrongPasswordIsAuthAndStoresNothing() = runTest {
        val itmo = Itmo { respond(fixture("itmoid/login-form.html")) }
        val storage = InMemoryTokenStorage()
        val failure = assertFailsWith<MyItmoException.Auth> {
            MyItmo.create(engine(itmo), false, storage, configuration, clock).signIn(username, password)
        }
        assertFalse(password in failure.toString())
        assertEquals(2, itmo.seen.size)
        assertNull(storage.read())
    }

    @Test
    fun theHtmlFormVariantIsFoundAndUnescaped() {
        assertEquals(
            "https://issuer.invalid/realm/login-actions/authenticate?session_code=synthetic-session&execution=00000000-0000-4000-8000-000000000002&client_id=synthetic-client&tab_id=synthetic-tab",
            PasswordLogin.findLoginAction(fixture("itmoid/login-form.html")),
        )
        assertNull(PasswordLogin.findLoginAction("<html></html>"))
    }

    @Test
    fun aFormActionOutsideTheIssuerNeverReceivesTheCredentials() = runTest {
        val itmo = Itmo(signedIn)
        val page = fixture("itmoid/login-page.html").replace("https://issuer.invalid/realm/login-actions", "https://foreign.invalid/realm/login-actions")
        assertFailsWith<MyItmoException.Decode> {
            MyItmo.create(engine(itmo, page), false, InMemoryTokenStorage(), configuration, clock).signIn(username, password)
        }
        assertEquals(1, itmo.seen.size)
    }

    @Test
    fun aCallbackWithAnotherStateIsRejected() = runTest {
        val itmo = Itmo { respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://callback.invalid/login?state=other&code=synthetic-code")) }
        assertFailsWith<MyItmoException.Auth> {
            MyItmo.create(engine(itmo), false, InMemoryTokenStorage(), configuration, clock).signIn(username, password)
        }
        assertEquals(2, itmo.seen.size)
    }

    @Test
    fun aServerErrorOnTheFormIsHttpNotAuth() = runTest {
        val itmo = Itmo { respond("", HttpStatusCode.ServiceUnavailable) }
        assertEquals(503, assertFailsWith<MyItmoException.Http> {
            MyItmo.create(engine(itmo), false, InMemoryTokenStorage(), configuration, clock).signIn(username, password)
        }.status)
    }

    @Test
    fun refreshTokenIsCheckedAtOnceAndStored() = runTest {
        val seen = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request -> seen += request; respond(tokens) }
        val storage = InMemoryTokenStorage()
        MyItmo.create(engine, false, storage, configuration, clock).refreshWith("synthetic-old-refresh").use {
            assertEquals("synthetic-old-refresh", form(seen.single())["refresh_token"])
            assertEquals("synthetic-refresh", storage.read()?.refreshToken)
        }
        val rejecting = MockEngine { respond(fixture("itmoid/oauth-error.json"), HttpStatusCode.BadRequest) }
        assertFailsWith<MyItmoException.Auth> {
            MyItmo.create(rejecting, false, InMemoryTokenStorage(), configuration, clock).refreshWith("synthetic-expired")
        }
    }

    @Test
    fun withoutAPasswordLoginBarsNeedsItsOwnSignIn() = runTest {
        val engine = MockEngine { error("No request expected") }
        MyItmo.create(engine, false, InMemoryTokenStorage(), configuration, clock).use { client ->
            assertFailsWith<MyItmoException.Auth> { client.bars.getDisciplines() }
            assertFailsWith<MyItmoException.Auth> { client.schedule.getTimeSlots() }
            assertEquals("MyItmo(redacted)", client.toString())
        }
    }
}
