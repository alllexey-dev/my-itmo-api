package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.ItmoTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Off-network request seam for production SportApi calls, including DELETE bodies. */
internal suspend fun <T> sportExchange(
    path: String,
    response: String,
    query: Map<String, List<String>> = emptyMap(),
    method: HttpMethod = HttpMethod.Get,
    body: String? = null,
    status: Int = 200,
    inspect: (io.ktor.client.request.HttpRequestData) -> Unit = {},
    action: suspend (ItmoTransport) -> T,
): T {
    var count = 0
    val sentinel = "Bearer " + "test-only"
    val engine = MockEngine { request ->
        count++
        assertEquals(method, request.method)
        assertEquals("https", request.url.protocol.name)
        assertEquals("example.invalid", request.url.host)
        assertEquals("/api/sport/$path", request.url.encodedPath)
        assertEquals(query, request.url.parameters.entries().associate { it.key to it.value })
        assertEquals("ru", request.headers[HttpHeaders.AcceptLanguage])
        assertTrue(request.headers[HttpHeaders.Authorization] == sentinel, "Bearer must reach sport")
        if (body != null) {
            val content = request.body as TextContent
            assertEquals(ContentType.Application.Json, content.contentType)
            assertEquals(body, content.text)
        } else {
            assertTrue(request.body is io.ktor.http.content.OutgoingContent.NoContent)
        }
        inspect(request)
        respond(response, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val client = HttpClient(engine) {
        expectSuccess = false
        defaultRequest {
            header(HttpHeaders.Authorization, sentinel)
            header(HttpHeaders.AcceptLanguage, "ru")
        }
    }
    try {
        return action(ItmoTransport(client, Url("https://example.invalid/custom/base/")))
    } finally {
        client.close()
        engine.close()
        assertEquals(1, count, "Exactly one sport exchange")
    }
}
