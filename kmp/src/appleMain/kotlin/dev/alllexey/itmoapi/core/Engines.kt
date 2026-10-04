@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.alllexey.itmoapi.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.statement.HttpResponse
import io.ktor.http.renderSetCookieHeader
import io.ktor.http.setCookie
import kotlinx.io.IOException
import platform.Foundation.NSHTTPCookieAcceptPolicy
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData

/** Creates an isolated Darwin engine without shared cookies or a URL cache.
 * Ktor's own session delegate refuses native redirects; no preconfigured session bypasses it.
 * Callers explicitly inject and own this engine; closing a client does not close it.
 */
public actual fun defaultEngine(): HttpClientEngine = Darwin.create {
    configureSession {
        HTTPCookieStorage = null
        HTTPShouldSetCookies = false
        HTTPCookieAcceptPolicy = NSHTTPCookieAcceptPolicy.NSHTTPCookieAcceptPolicyNever
        URLCache = null
        requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData
    }
}

/** Disables Ktor redirects without replacing, mutating or taking ownership of [engine].
 * Use [defaultEngine] for the verified platform policy. A custom engine must already satisfy
 * the no-cookie/no-cache/no-native-redirect precondition; a prebuilt session cannot be sanitized here.
 * No HttpCookies or HttpCache plugin is installed; explicit caller Cookie headers pass unchanged.
 */
internal actual fun noCookieClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    followRedirects = false
    expectSuccess = false
}

/** Darwin may fold several cookies into one header. Ktor's SP-15a-verified public parser
 * recovers each cookie, including the Expires comma, flags and SameSite extensions.
 * Rendering preserves values/attribute semantics, normalizes spelling/order, and adds no $x-enc marker.
 */
internal actual fun readSetCookies(response: HttpResponse): List<String> = response.setCookie().map { cookie ->
    renderSetCookieHeader(
        name = cookie.name,
        value = cookie.value,
        encoding = cookie.encoding,
        maxAge = cookie.maxAge,
        expires = cookie.expires,
        domain = cookie.domain,
        path = cookie.path,
        secure = cookie.secure,
        httpOnly = cookie.httpOnly,
        extensions = cookie.extensions,
        includeEncoding = false,
    )
}

internal actual fun isNetworkFailure(cause: Throwable): Boolean = cause is IOException
