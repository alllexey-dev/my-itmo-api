package dev.alllexey.itmoapi.myitmo.qr

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
import io.ktor.http.headersOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Isolated shared seam for the three ML-05a areas; never prints request headers or bodies. */
internal suspend fun <T> areaExchange(
    path: String,
    response: String,
    query: Map<String, List<String>> = emptyMap(),
    host: String = "example.invalid",
    status: Int = 200,
    action: suspend (ItmoTransport) -> T,
): T {
    val bearer = "Bearer " + "test-only" // Synthetic sentinel, never a credential.
    var count = 0
    val engine = MockEngine { request ->
        count++
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("https", request.url.protocol.name)
        assertEquals(host, request.url.host)
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
        // A path prefix deliberately tests that MyITMO endpoint paths are rooted, while QR is absolute.
        return action(ItmoTransport(client, Url("https://example.invalid/custom/base/")))
    } finally {
        client.close()
        engine.close()
        assertEquals(1, count, "One exchange must be consumed")
    }
}
