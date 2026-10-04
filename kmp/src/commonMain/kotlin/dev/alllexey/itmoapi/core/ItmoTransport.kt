package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Builds an isolated, injectable client without logging or shared token state. */
public fun createItmoHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(ContentNegotiation) { json(ItmoApiJson) }
}

/** Shared area implementation dependency: one configured client and one base URL. */
internal class ItmoTransport(internal val client: HttpClient, private val baseUrl: Url) {
    private fun hasNetworkCause(failure: Throwable): Boolean {
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = failure
        while (current != null && seen.add(current)) {
            if (isNetworkFailure(current)) return true
            current = current.cause
        }
        return false
    }

    suspend fun <T> execute(
        deserializer: DeserializationStrategy<T>,
        method: HttpMethod,
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val status: Int
        val body: String
        try {
            val response = client.request {
                this.method = method
                url {
                    takeFrom(baseUrl)
                    appendPathSegments(path.trimStart('/'))
                }
                configure()
            }
            status = response.status.value
            body = response.bodyAsText()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            if (hasNetworkCause(failure)) throw MyItmoException.Network(failure)
            // Non-I/O engine/plugin failures can also contain request data.
            throw MyItmoException.Decode()
        }
        val tree = try {
            ItmoApiJson.parseToJsonElement(body)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        val envelope = tree as? JsonObject
        val errorCode = (envelope?.get("error_code") as? JsonPrimitive)?.intOrNull
            ?: (envelope?.get("code") as? JsonPrimitive)?.intOrNull
        if (errorCode != null && errorCode != 0) throw MyItmoException.Api(status, errorCode)
        if (status == 401 || status == 403) throw MyItmoException.Auth(status)
        if (status !in 200..299) throw MyItmoException.Http(status)
        if (tree == null) throw MyItmoException.Decode()
        return try {
            ItmoApiJson.decodeFromJsonElement(deserializer, tree)
        } catch (_: SerializationException) {
            throw MyItmoException.Decode()
        } catch (_: IllegalArgumentException) {
            throw MyItmoException.Decode()
        }
    }
}
