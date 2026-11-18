package dev.alllexey.itmoapi.bars.auth

import dev.alllexey.itmoapi.bars.BarsConfiguration
import dev.alllexey.itmoapi.core.defaultEngine
import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import io.ktor.http.Url
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Fail-closed probe before any real-engine scenario: no official URL or credential is configured. */
class CookieRoutingSafetyTest {
    @Test
    fun clientInstallationUsesTheRoutingReceiverAndOnlyLoopbackSeesTheRequest(): Unit = runBlocking {
        val server = MockWebServer().apply { start() }
        val engine = CookieRoutingEngine(defaultEngine(), Url("http://127.0.0.1:${server.port}/"))
        val login = BarsLogin(engine, BarsConfiguration(redirectUri = "https://callback.invalid/rest/login",
            itmoId = ItmoIdConfiguration(issuer = "https://issuer.invalid/auth/realms/itmo")))
        try {
            server.enqueue(MockResponse().setResponseCode(204))
            assertEquals(BarsSessionCode.Outcome.LOGIN_REQUIRED,
                withTimeout(5_000) { login.requestCodeWithCookies("routing-probe", "synthetic_probe=value") }.outcome)
            val request = server.takeRequest(2, TimeUnit.SECONDS)
            assertTrue(request != null, "Only the loopback server must receive the request")
            assertTrue(request.getHeader("Host") == "127.0.0.1:${server.port}", "Wire destination must be loopback")
            assertTrue(request.getHeader("Cookie") == "synthetic_probe=value", "Synthetic Cookie must arrive intact")
            assertEquals("GET", request.method)
            assertEquals(1, server.requestCount)
        } finally {
            login.close()
            engine.close()
            server.shutdown()
        }
    }

    @Test
    fun nonLoopbackWireDestinationsAreRejectedBeforeAnyRequest() {
        val delegate = MockEngine { error("No delegate request is allowed") }
        try {
            listOf("http://foreign.invalid/", "https://127.0.0.1/", "http://localhost/").forEach {
                assertFailsWith<IllegalArgumentException> { CookieRoutingEngine(delegate, Url(it)) }
            }
            assertTrue(delegate.requestHistory.isEmpty(), "Unsafe wire destinations must never reach an engine")
        } finally { delegate.close() }
    }

    @Test
    fun defaultOfficialLogicalUrlIsRejectedBeforeTheDelegate(): Unit = runBlocking {
        // A MockEngine delegate makes this negative test incapable of network access even if its guard regresses.
        val delegate = MockEngine { error("No delegate request is allowed") }
        val engine = CookieRoutingEngine(delegate, Url("http://127.0.0.1:1/"))
        val login = BarsLogin(engine)
        try {
            assertFailsWith<AssertionError> { login.requestCodeWithCookies("routing-probe", "synthetic_probe=value") }
            assertTrue(delegate.requestHistory.isEmpty(), "Official logical URLs must fail before the delegate")
        } finally { login.close(); engine.close() }
    }

}
