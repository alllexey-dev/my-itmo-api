package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.parseServerSetCookieHeader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OkHttpWireTest {
    private val cookies = listOf(
        "synthetic_session=a; Path=/; HttpOnly; SameSite=Lax",
        "synthetic_expires=b; Expires=Wed, 21 Oct 2026 07:28:00 GMT; Path=/realms/itmo/; HttpOnly",
        "synthetic_maxage=c; Max-Age=3600; Path=/; Secure; SameSite=None",
    )

    private fun cookieResponse(): MockResponse = MockResponse().setBody("ok").apply {
        cookies.forEach { addHeader(HttpHeaders.SetCookie, it) }
    }

    private fun wireTest(block: suspend (MockWebServer, HttpClient) -> Unit): Unit = runBlocking {
        withTimeout(15_000) {
            val server = MockWebServer()
            server.start()
            val engine = defaultEngine()
            val client = noCookieClient(engine)
            try {
                block(server, client)
            } finally {
                client.close()
                engine.close()
                server.shutdown()
            }
        }
    }

    @Test
    fun everyCookieAndAttributeIsReturnedWithoutSplittingExpiresComma() = wireTest { server, client ->
        server.enqueue(cookieResponse())
        val returned = readSetCookies(client.get(server.url("/cookies").toString()))
        assertTrue(returned == cookies, "Raw Set-Cookie values must be retained")
        val parsed = returned.map(::parseServerSetCookieHeader)
        assertEquals(3, parsed.size)
        assertEquals(1792567680000L, parsed[1].expires?.timestamp)
        assertEquals("/realms/itmo/", parsed[1].path)
        assertTrue(parsed[0].httpOnly)
        assertEquals("Lax", parsed[0].extensions["SameSite"])
        assertEquals(3600, parsed[2].maxAge)
        assertTrue(parsed[2].secure)
        assertEquals("None", parsed[2].extensions["SameSite"])
    }

    @Test
    fun redirectIsNotFollowedAndCookiesStayReadable() = wireTest { server, client ->
        server.enqueue(cookieResponse().setResponseCode(302).addHeader(HttpHeaders.Location, "/landing"))
        server.enqueue(MockResponse().setBody("unexpected landing"))
        val response = client.get(server.url("/redirect").toString())
        assertEquals(302, response.status.value)
        assertEquals("/landing", response.headers[HttpHeaders.Location])
        assertTrue(readSetCookies(response) == cookies, "Redirect Set-Cookie values must be retained")
        assertEquals("/redirect", server.takeRequest(2, TimeUnit.SECONDS)?.path)
        assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS), "Redirect destination must not be requested")
    }

    @Test
    fun cookiesAreNotPersistedAcrossRequestsOrClients() = wireTest { server, client ->
        server.enqueue(cookieResponse())
        server.enqueue(MockResponse().setBody("ok"))
        server.enqueue(MockResponse().setBody("ok"))
        client.get(server.url("/cookies").toString()).bodyAsText()
        client.get(server.url("/same-client").toString()).bodyAsText()
        val secondEngine = defaultEngine()
        val secondClient = noCookieClient(secondEngine)
        try {
            secondClient.get(server.url("/new-client").toString()).bodyAsText()
        } finally {
            secondClient.close()
            secondEngine.close()
        }
        repeat(3) {
            val request = server.takeRequest(2, TimeUnit.SECONDS)
            assertTrue(request != null, "Expected request must arrive")
            assertTrue(request?.getHeader(HttpHeaders.Cookie) == null, "No automatic Cookie header may be sent")
        }
    }

    @Test
    fun explicitCookieReplayReachesWireUnchanged() = wireTest { server, client ->
        val replay = "synthetic_a=1; synthetic_b=2"
        server.enqueue(MockResponse().setBody("ok"))
        client.get(server.url("/replay").toString()) { header(HttpHeaders.Cookie, replay) }.bodyAsText()
        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertTrue(request?.getHeader(HttpHeaders.Cookie) == replay, "Explicit Cookie must be replayed unchanged")
    }

    @Test
    fun cacheableResponsesAreFetchedAgain() = wireTest { server, client ->
        server.enqueue(MockResponse().setBody("first").addHeader(HttpHeaders.CacheControl, "public, max-age=3600"))
        server.enqueue(MockResponse().setBody("second").addHeader(HttpHeaders.CacheControl, "public, max-age=3600"))
        val url = server.url("/cacheable").toString()
        assertEquals("first", client.get(url).bodyAsText())
        assertEquals("second", client.get(url).bodyAsText())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun deleteJsonBodyAndLanguageReachWire() = wireTest { server, client ->
        val body = """{"deviceId":"synthetic-device"}"""
        server.enqueue(MockResponse().setBody("ok"))
        client.delete(server.url("/device").toString()) {
            header(HttpHeaders.AcceptLanguage, "ru")
            setBody(TextContent(body, ContentType.Application.Json))
        }.bodyAsText()
        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertEquals("DELETE", request?.method)
        assertTrue(request?.body?.readUtf8() == body, "DELETE JSON body must arrive intact")
        assertEquals("ru", request?.getHeader(HttpHeaders.AcceptLanguage))
        assertEquals("application/json", request?.getHeader(HttpHeaders.ContentType))
    }

    @Test
    fun closingClientDoesNotCloseCallerOwnedEngine() = wireTest { server, client ->
        server.enqueue(MockResponse().setBody("first"))
        assertEquals("first", client.get(server.url("/first").toString()).bodyAsText())
        val engine = client.engine
        client.close()
        server.enqueue(MockResponse().setBody("second"))
        val secondClient = noCookieClient(engine)
        try {
            assertEquals("second", secondClient.get(server.url("/second").toString()).bodyAsText())
        } finally {
            secondClient.close()
        }
    }
}
