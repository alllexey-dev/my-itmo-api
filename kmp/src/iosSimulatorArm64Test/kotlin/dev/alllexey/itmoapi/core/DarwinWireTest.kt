@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.parseServerSetCookieHeader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** SP-15a wire scenarios executed by existing GitHub iOS CI, never by a local simulator. */
class DarwinWireTest {
    private fun wireTest(block: suspend (LoopbackServer, HttpClient) -> Unit): Unit = runBlocking {
        withTimeout(20_000) {
            val server = LoopbackServer()
            val engine = defaultEngine()
            val client = noCookieClient(engine)
            try {
                block(server, client)
            } finally {
                client.close()
                engine.close()
                server.stop()
            }
        }
    }

    private fun assertCookies(response: HttpResponse) {
        val returned = readSetCookies(response)
        assertTrue(returned.none { it.contains("\$x-enc") }, "No artificial encoding marker may be added")
        assertTrue(returned.any { it.contains("Expires=Wed, 21 Oct 2026 07:28:00 GMT") }, "Expires comma/date must remain intact")
        val parsed = returned.map(::parseServerSetCookieHeader)
        assertEquals(3, parsed.size)
        assertTrue(parsed.map { it.name } == listOf("synthetic_session", "synthetic_expires", "synthetic_maxage"),
            "Every cookie must be recovered in response order")
        assertTrue(parsed.map { it.value } == listOf("a", "b", "c"), "Cookie values must be preserved")
        assertEquals("/", parsed[0].path)
        assertEquals("/", parsed[2].path)
        assertEquals(1792567680000L, parsed[1].expires?.timestamp)
        assertEquals("/realms/itmo/", parsed[1].path)
        assertTrue(parsed[0].httpOnly)
        assertEquals("Lax", parsed[0].extensions["SameSite"])
        assertTrue(parsed[2].secure)
        assertEquals(3600, parsed[2].maxAge)
        assertEquals("None", parsed[2].extensions["SameSite"])
    }

    @Test
    fun foldedHeadersRecoverAllThreeCookiesIncludingExpiresComma() = wireTest { server, client ->
        val response = client.get("${server.baseUrl}/cookies")
        assertEquals(200, response.status.value)
        assertCookies(response)
        assertEquals("/cookies", server.takeRequest().path)
    }

    @Test
    fun redirectIsNotFollowedAndItsCookiesAreReadable() = wireTest { server, client ->
        val response = client.get("${server.baseUrl}/redirect")
        assertEquals(302, response.status.value)
        assertEquals("/landing", response.headers[HttpHeaders.Location])
        assertCookies(response)
        client.get("${server.baseUrl}/probe").bodyAsText()
        assertEquals("/redirect", server.takeRequest().path)
        assertEquals("/probe", server.takeRequest().path, "No landing request may intervene")
    }

    @Test
    fun noCookiesPersistOnSameOrNewClientOrSharedAppleStorage() = wireTest { server, client ->
        client.get("${server.baseUrl}/cookies").bodyAsText()
        client.get("${server.baseUrl}/same-client").bodyAsText()
        val secondEngine = defaultEngine()
        val secondClient = noCookieClient(secondEngine)
        try {
            secondClient.get("${server.baseUrl}/new-client").bodyAsText()
        } finally {
            secondClient.close()
            secondEngine.close()
        }
        repeat(3) {
            assertTrue(server.takeRequest().headers["cookie"] == null, "No automatic Cookie may be sent")
        }
        val stored = NSHTTPCookieStorage.sharedHTTPCookieStorage.cookiesForURL(NSURL(string = server.baseUrl)).orEmpty()
        assertTrue(stored.isEmpty(), "Shared Apple storage must not retain cookies")
    }

    @Test
    fun explicitCookieHeaderIsReplayedUnchanged() = wireTest { server, client ->
        val replay = "synthetic_a=1; synthetic_b=2"
        client.get("${server.baseUrl}/replay") { header(HttpHeaders.Cookie, replay) }.bodyAsText()
        assertTrue(server.takeRequest().headers["cookie"] == replay, "Explicit Cookie must reach wire unchanged")
    }

    @Test
    fun cacheableResponsesStillReachServerTwice() = wireTest { server, client ->
        repeat(2) { client.get("${server.baseUrl}/cacheable").bodyAsText() }
        repeat(2) { assertEquals("/cacheable", server.takeRequest().path) }
    }

    @Test
    fun deleteJsonBodyAndAcceptLanguageReachDarwinWire() = wireTest { server, client ->
        val body = """{"deviceId":"synthetic-device"}"""
        client.delete("${server.baseUrl}/device") {
            header(HttpHeaders.AcceptLanguage, "ru")
            setBody(TextContent(body, ContentType.Application.Json))
        }.bodyAsText()
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/device", request.path)
        assertTrue(request.body == body, "DELETE JSON body must arrive intact")
        assertEquals("application/json", request.headers["content-type"])
        assertEquals("ru", request.headers["accept-language"])
    }

    @Test
    fun clientClosureLeavesTheInjectedDarwinEngineUsable() = wireTest { server, client ->
        client.get("${server.baseUrl}/first").bodyAsText()
        val engine = client.engine
        client.close()
        val secondClient = noCookieClient(engine)
        try {
            secondClient.get("${server.baseUrl}/second").bodyAsText()
        } finally {
            secondClient.close()
        }
        assertEquals("/first", server.takeRequest().path)
        assertEquals("/second", server.takeRequest().path)
    }
}
