package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.statement.HttpResponse

/** Creates a platform engine: OkHttp on JVM/Android, Darwin on Apple.
 * Clients require an explicit engine argument, allowing isolated MockEngine tests.
 */
public expect fun defaultEngine(): HttpClientEngine

/** Client for caller-owned ITMO.ID cookie replay: no shared cookie storage, caching or redirects. */
internal expect fun noCookieClient(engine: HttpClientEngine): HttpClient

/** Returns every Set-Cookie value, including Darwin's folded headers. */
internal expect fun readSetCookies(response: HttpResponse): List<String>

internal expect fun isNetworkFailure(cause: Throwable): Boolean
