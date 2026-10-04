package dev.alllexey.itmoapi.myitmo

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.TokenManager
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import kotlinx.coroutines.cancel

/** Host-scoped bearer plugin, installed only by MyItmoClient. Never authenticates ITMO.ID or foreign hosts.
 * HTTPS and the standard port are required; redirects are disabled by the assembled client.
 * Explicit authorization/language headers win. Explicit authorization is never replaced or retried.
 */
internal class MyItmoAuthConfiguration {
    lateinit var tokens: TokenManager
    lateinit var configuration: MyItmoConfiguration
}

internal val MyItmoAuth = createClientPlugin("MyItmoAuth", ::MyItmoAuthConfiguration) {
    val tokens = pluginConfig.tokens
    val configuration = pluginConfig.configuration
    on(Send) { request ->
        val trusted = request.url.protocol == URLProtocol.HTTPS && request.url.build().port == 443 &&
            request.url.user.isNullOrEmpty() && request.url.password.isNullOrEmpty() &&
            request.url.host in setOf(configuration.baseUrl.host, "qr.itmo.su")
        if (!trusted) return@on proceed(request)
        if (!request.headers.contains(HttpHeaders.AcceptLanguage)) {
            request.headers.append(HttpHeaders.AcceptLanguage, configuration.acceptLanguage)
        }
        if (request.headers.contains(HttpHeaders.Authorization)) return@on proceed(request)
        val access = tokens.validAccessToken()
        request.headers.append(HttpHeaders.Authorization, "Bearer $access")
        val first = proceed(request)
        if (first.response.status != HttpStatusCode.Unauthorized) return@on first
        first.cancel()
        request.headers.remove(HttpHeaders.Authorization)
        request.headers.append(HttpHeaders.Authorization, "Bearer ${tokens.refreshObserved(access)}")
        val retried = proceed(request)
        if (retried.response.status == HttpStatusCode.Unauthorized) {
            retried.cancel()
            throw MyItmoException.Auth(401)
        }
        retried
    }
}
