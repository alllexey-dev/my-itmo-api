package dev.alllexey.itmoapi.bars.auth

import dev.alllexey.itmoapi.bars.BarsConfiguration
import dev.alllexey.itmoapi.bars.auth.BarsSessionCode.Outcome
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import kotlinx.coroutines.withTimeout
import kotlin.test.*
import kotlin.uuid.Uuid

/** The same eleven legacy cookie scenarios execute with MockEngine, OkHttp and Darwin. */
internal class BarsCookieScenarios(private val factory: suspend () -> CookieExchange) {
    private suspend fun scenario(block: suspend Context.() -> Unit) {
        val exchange = factory()
        val login = BarsLogin(exchange.engine, exchange.configuration)
        try {
            Context(exchange, login).block()
            exchange.assertNoRequest()
        } finally {
            login.close()
            exchange.close()
        }
    }

    private class Context(val exchange: CookieExchange, val login: BarsLogin) {
        val state = Uuid.random().toString()
        val code = Uuid.random().toString()
        val cookie = "synthetic_session=${Uuid.random()}; synthetic_id=${Uuid.random()}"
        val cookies = listOf(
            "synthetic_a=${Uuid.random()}; Path=/; HttpOnly; SameSite=Lax",
            "synthetic_b=${Uuid.random()}; Expires=Wed, 21 Oct 2026 07:28:00 GMT; Path=/auth/realms/itmo/",
        )
        val callback: String get() = login.configuration.redirectUri
        fun validCallback(): String = "$callback?state=$state&code=$code"
        suspend fun request(): BarsSessionCode = login.requestCodeWithCookies(state, cookie)
        suspend fun assertRequest(expectedCookie: String = cookie) {
            val request = exchange.takeRequest()
            val url = Url("https://id.invalid" + request.path)
            assertEquals("GET", request.method)
            assertEquals("/auth/realms/itmo/protocol/openid-connect/auth", url.encodedPath)
            assertTrue(url.parameters["state"] == state, "State must reach the wire")
            assertTrue(url.parameters["redirect_uri"] == callback, "Redirect URI must reach the wire")
            assertTrue(url.parameters["client_id"] == "bars" && url.parameters["scope"] == "openid" &&
                url.parameters["response_type"] == "code", "BARS OIDC parameters must reach the wire")
            assertTrue(request.cookies == listOf(expectedCookie), "One explicit Cookie header must be replayed unchanged")
            assertTrue(request.authorization == null, "No authorization header may be introduced")
        }
        fun assertCookies(result: BarsSessionCode) {
            val returned = result.setCookies.map(::parseServerSetCookieHeader)
            val expected = cookies.map(::parseServerSetCookieHeader)
            assertEquals(2, returned.size)
            assertTrue(returned.map { it.name to it.value } == expected.map { it.name to it.value },
                "All cookie values must be returned in response order")
            assertEquals(1792567680000L, returned[1].expires?.timestamp)
            assertEquals("/auth/realms/itmo/", returned[1].path)
            assertTrue(returned[0].httpOnly)
            assertEquals("Lax", returned[0].extensions["SameSite"])
        }
    }

    suspend fun callbackWithTheSameStateIsACodeAndIsNotFollowed() = scenario {
        exchange.enqueue(CookieReply(302, validCallback()))
        val result = request()
        assertEquals(Outcome.CODE, result.outcome)
        assertTrue(result.code == code, "Validated code must be returned unchanged")
        assertEquals(302, result.httpCode)
        assertRequest()
    }

    suspend fun callbackWithoutAUsableCodeIsRejected() = scenario {
        listOf(
            "$callback?state=other&code=$code", validCallback() + "&error=access_denied",
            validCallback() + "&code=other", validCallback() + "&iss=https%3A%2F%2Fforeign.invalid",
            validCallback() + "&iss=", validCallback() + "&%73tate=$state", validCallback() + "#fragment",
            "$callback?state=$state&code=%GG", "https://user@callback.invalid/rest/login?state=$state&code=$code",
            "https://callback.invalid:8443/rest/login?state=$state&code=$code",
            "https://xn--bars-123.itmo.ru/rest/login?state=$state&code=$code",
        ).forEach { location ->
            exchange.enqueue(CookieReply(302, location))
            val result = request()
            assertEquals(Outcome.REJECTED, result.outcome)
            assertTrue(result.code == null, "Rejected callbacks must not yield a code")
            assertRequest()
        }
    }

    suspend fun itmoIdPageMeansLoginIsRequired() = scenario {
        exchange.enqueue(CookieReply(302, "${login.configuration.issuer}/login-actions/authenticate"))
        assertEquals(Outcome.LOGIN_REQUIRED, request().outcome)
        assertRequest()
    }

    suspend fun relativeLocationIsResolvedFromTheRequestUrl() = scenario {
        exchange.enqueue(CookieReply(302, "/auth/realms/itmo/login-actions/authenticate?client_id=bars"))
        assertEquals(Outcome.LOGIN_REQUIRED, request().outcome)
        assertRequest()
        exchange.enqueue(CookieReply(302, "../login-actions/authenticate"))
        assertEquals(Outcome.LOGIN_REQUIRED, request().outcome)
        assertRequest()
    }

    suspend fun foreignRedirectIsRejectedAndRedirectWithoutLocationIsAnHttpError() = scenario {
        listOf("https://foreign.invalid/", "//foreign.invalid/", "https://issuer.invalid@foreign.invalid/", "http://issuer.invalid/").forEach {
            exchange.enqueue(CookieReply(302, it, cookies))
            val result = request()
            assertEquals(Outcome.REJECTED, result.outcome)
            assertCookies(result)
            assertRequest()
        }
        exchange.enqueue(CookieReply(302, setCookies = cookies))
        val result = request()
        assertEquals(Outcome.HTTP_ERROR, result.outcome)
        assertEquals(302, result.httpCode)
        assertCookies(result)
        assertRequest()
    }

    suspend fun loginPageIsLoginRequiredWithoutReadingTheBody() = scenario {
        exchange.enqueue(CookieReply(200, unreadBody = true))
        val result = withTimeout(5_000) { request() }
        assertEquals(Outcome.LOGIN_REQUIRED, result.outcome)
        assertEquals(200, result.httpCode)
        assertRequest()
    }

    suspend fun serverErrorIsAnHttpError() = scenario {
        listOf(400, 401, 403, 503).forEach { status ->
            exchange.enqueue(CookieReply(status, setCookies = cookies, unreadBody = true))
            val result = withTimeout(5_000) { request() }
            assertEquals(Outcome.HTTP_ERROR, result.outcome)
            assertEquals(status, result.httpCode)
            assertTrue(result.code == null)
            assertCookies(result)
            assertRequest()
        }
    }

    suspend fun missingCookiesNeedLoginWithoutARequest() = scenario {
        listOf("", "  ", null).forEach {
            val result = login.requestCodeWithCookies(state, it)
            assertEquals(Outcome.LOGIN_REQUIRED, result.outcome)
            assertEquals(0, result.httpCode)
            assertTrue(result.code == null && result.setCookies.isEmpty())
        }
    }

    suspend fun setCookiesAreReturnedInOrderAndNotStoredInTheClientJar() = scenario {
        exchange.enqueue(CookieReply(302, validCallback(), cookies))
        assertCookies(request())
        assertRequest()
        exchange.enqueue(CookieReply(200))
        // A second explicit replay is unchanged: response cookies must never be added automatically.
        assertEquals(Outcome.LOGIN_REQUIRED, request().outcome)
        assertRequest()
        exchange.assertNoStoredCookies()
    }

    suspend fun textFormHidesTheCodeAndCookies() = scenario {
        exchange.enqueue(CookieReply(302, validCallback(), cookies))
        val result = request()
        assertEquals(Outcome.CODE, result.outcome)
        val text = result.toString()
        assertTrue(!text.contains(code) && !text.contains(cookie) && cookies.none { text.contains(it) }, "Diagnostics must be redacted")
        assertEquals("BarsSessionCode(outcome=CODE, httpCode=302)", text)
        assertRequest()
    }

    suspend fun stoppedServerIsANetworkError() = scenario {
        exchange.stop()
        val error = assertFailsWith<MyItmoException.Network> { request() }
        assertTrue(error.cause != null)
        assertEquals("Network request failed", error.message)
        assertTrue(!error.toString().contains(code) && !error.toString().contains(cookie), "Network diagnostics must be redacted")
    }
}

internal fun cookieTestConfiguration(): BarsConfiguration = BarsConfiguration(
    redirectUri = "https://callback.invalid/rest/login",
    itmoId = ItmoIdConfiguration(issuer = "https://issuer.invalid/auth/realms/itmo"),
)

internal interface CookieExchange {
    val engine: HttpClientEngine
    val configuration: BarsConfiguration get() = cookieTestConfiguration()
    suspend fun enqueue(reply: CookieReply)
    suspend fun takeRequest(): CookieRequest
    suspend fun assertNoRequest()
    suspend fun assertNoStoredCookies() {}
    suspend fun stop()
    suspend fun close()
}

internal class CookieReply(val status: Int, val location: String? = null, val setCookies: List<String> = emptyList(), val unreadBody: Boolean = false) {
    override fun toString(): String = "CookieReply(status=$status, contents=[redacted])"
}

internal class CookieRequest(val method: String, val path: String, val cookies: List<String>, val authorization: String?) {
    override fun toString(): String = "CookieRequest(method=$method, contents=[redacted])"
}

/** Engine routing is test-only: strict logical HTTPS URLs stay intact while wire traffic is loopback HTTP. */
@OptIn(io.ktor.utils.io.InternalAPI::class)
internal class CookieRoutingEngine(private val delegate: HttpClientEngine, private val wireBase: Url) : HttpClientEngine by delegate {
    init {
        require(wireBase.protocol == URLProtocol.HTTP && wireBase.host == "127.0.0.1" && wireBase.port > 0) {
            "Test wire destination must be loopback HTTP"
        }
    }

    override fun install(client: HttpClient) { super<HttpClientEngine>.install(client) }

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        assertEquals("https", data.url.protocol.name)
        assertEquals("issuer.invalid", data.url.host)
        val routed = URLBuilder(data.url).apply { protocol = wireBase.protocol; host = wireBase.host; port = wireBase.port }.build()
        return delegate.execute(HttpRequestData(routed, data.method, data.headers, data.body, data.executionContext, data.attributes))
    }
}
