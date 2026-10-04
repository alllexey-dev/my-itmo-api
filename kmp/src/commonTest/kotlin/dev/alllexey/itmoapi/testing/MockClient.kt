package dev.alllexey.itmoapi.testing

import dev.alllexey.itmoapi.core.createItmoHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A scripted MockEngine exchange; diagnostics intentionally do not print headers or payloads. */
internal class ExpectedRequest(val method: HttpMethod, val path: String) {
    private val query = linkedMapOf<String, List<String>>()
    private val headers = linkedMapOf<String, String>()
    private var body: String? = null
    internal var response: String = "{}"
    internal var status: HttpStatusCode = HttpStatusCode.OK

    fun query(name: String, vararg values: String) {
        query[name] = values.toList()
    }

    fun header(name: String, value: String) {
        headers[name] = value
    }

    fun body(value: String) {
        body = value
    }

    fun respond(body: String, status: Int = 200) {
        response = body
        this.status = HttpStatusCode.fromValue(status)
    }

    internal fun assertMatches(request: HttpRequestData) {
        assertEquals(method, request.method, "HTTP method")
        assertEquals(path, request.url.encodedPath, "HTTP path")
        assertTrue(query == request.url.parameters.entries().associate { it.key to it.value }, "Query parameters do not match expectation")
        headers.forEach { (name, value) ->
            assertTrue(request.headers[name] == value, "Header does not match expectation")
        }
        body?.let { expected ->
            val content = request.body as? OutgoingContent.ByteArrayContent
            assertTrue(content != null && content.bytes().decodeToString() == expected, "Body does not match expectation")
        }
    }
}

internal class MockClientBuilder {
    internal val expectations = mutableListOf<ExpectedRequest>()

    fun expect(method: HttpMethod, path: String, configure: ExpectedRequest.() -> Unit = {}) {
        expectations += ExpectedRequest(method, path).apply(configure)
    }
}

internal class MockClient(expectations: List<ExpectedRequest>) {
    private var next = 0
    private val requests = expectations.toList()
    val engine: MockEngine = MockEngine { request ->
        assertTrue(next < requests.size, "Unexpected request")
        val expected = requests[next++]
        expected.assertMatches(request)
        respond(expected.response, expected.status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
    }
    val client: HttpClient = createItmoHttpClient(engine)

    fun assertComplete() {
        assertEquals(requests.size, next, "Every scripted exchange must be consumed")
    }

    fun close() {
        client.close()
        engine.close()
    }
}

/** Method/path/query/header/body assertions shared by all later area cards. */
internal fun mockClient(configure: MockClientBuilder.() -> Unit): MockClient =
    MockClient(MockClientBuilder().apply(configure).expectations)
