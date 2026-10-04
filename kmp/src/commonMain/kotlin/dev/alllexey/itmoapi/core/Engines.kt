package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.statement.HttpResponse

/** Creates a platform engine: OkHttp on JVM/Android, Darwin on Apple.
 * Clients require an explicit engine argument, allowing isolated MockEngine tests.
 */
public expect fun defaultEngine(): HttpClientEngine

/** ML-03 seam for ADR 0012 cookie replay without shared cookies, caches or redirects.
 * Platform implementations intentionally remain TODO until their real-engine tests land.
 */
internal expect fun noCookieClient(engine: HttpClientEngine): HttpClient

/** ML-03 seam returning every Set-Cookie value, including Darwin's folded headers. */
internal expect fun readSetCookies(response: HttpResponse): List<String>

internal expect fun isNetworkFailure(cause: Throwable): Boolean
