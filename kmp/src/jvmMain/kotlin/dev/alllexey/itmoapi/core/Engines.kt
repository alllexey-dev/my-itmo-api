package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.statement.HttpResponse
import java.io.IOException

/** Platform engine; callers explicitly inject it into the client they own. */
public actual fun defaultEngine(): HttpClientEngine = OkHttp.create()

internal actual fun noCookieClient(engine: HttpClientEngine): HttpClient =
    TODO("ML-03: verify no-cookie, no-cache and no-redirect OkHttp client")

internal actual fun readSetCookies(response: HttpResponse): List<String> =
    TODO("ML-03: verify every Set-Cookie value on OkHttp")

internal actual fun isNetworkFailure(cause: Throwable): Boolean = cause is IOException
