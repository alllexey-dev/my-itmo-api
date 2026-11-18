package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import okhttp3.CookieJar
import java.io.IOException

/** Creates an isolated OkHttp engine without cookies, response caching or native redirects.
 * Callers explicitly inject and own this engine; closing a client does not close it.
 */
public actual fun defaultEngine(): HttpClientEngine = OkHttp.create {
    config {
        followRedirects(false)
        followSslRedirects(false)
        cookieJar(CookieJar.NO_COOKIES)
        cache(null)
    }
}

/** Disables Ktor redirects without replacing, mutating or taking ownership of [engine].
 * Use [defaultEngine] for the verified platform policy. A custom engine must already satisfy
 * the no-cookie/no-cache/no-native-redirect precondition; a prebuilt engine cannot be sanitized here.
 * No HttpCookies or HttpCache plugin is installed; explicit caller Cookie headers pass unchanged.
 */
internal actual fun noCookieClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    followRedirects = false
    expectSuccess = false
}

/** Returns every raw header value, retaining cookie attributes and the Expires comma. */
internal actual fun readSetCookies(response: HttpResponse): List<String> =
    response.headers.getAll(HttpHeaders.SetCookie).orEmpty()

internal actual fun isNetworkFailure(cause: Throwable): Boolean = cause is IOException
