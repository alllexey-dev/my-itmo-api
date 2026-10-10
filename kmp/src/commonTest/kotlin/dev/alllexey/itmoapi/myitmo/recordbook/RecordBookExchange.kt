package dev.alllexey.itmoapi.myitmo.recordbook

import dev.alllexey.itmoapi.core.ItmoTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Isolated request seam for the recordbook and study-plan endpoints; never prints request headers or bodies. */
internal suspend fun <T> recordBookExchange(
    path: String,
    response: String,
    query: Map<String, List<String>> = emptyMap(),
    status: Int = 200,
    action: suspend (ItmoTransport) -> T,
): T {
    val bearer = "Bearer " + "test-only" // Synthetic sentinel, never a credential.
    var count = 0
    val engine = MockEngine { request ->
        count++
        assertEquals(HttpMethod.Get, request.method)
        assertTrue(request.body is OutgoingContent.NoContent, "GET must not carry a body")
        assertEquals("https", request.url.protocol.name)
        assertEquals("example.invalid", request.url.host)
        assertEquals(path, request.url.encodedPath)
        assertEquals(query, request.url.parameters.entries().associate { it.key to it.value })
        assertEquals("ru", request.headers[HttpHeaders.AcceptLanguage])
        assertTrue(request.headers[HttpHeaders.Authorization] == bearer, "Bearer must reach the endpoint")
        respond(response, HttpStatusCode.fromValue(status), headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }
    val client = HttpClient(engine) {
        expectSuccess = false
        defaultRequest {
            header(HttpHeaders.Authorization, bearer)
            header(HttpHeaders.AcceptLanguage, "ru")
        }
    }
    try {
        // A custom prefix deliberately catches endpoints accidentally resolved relative to the base.
        return action(ItmoTransport(client, Url("https://example.invalid/custom/base/")))
    } finally {
        client.close()
        engine.close()
        assertEquals(1, count, "One exchange must be consumed")
    }
}
