package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.isNetworkFailure
import dev.alllexey.itmoapi.core.noCookieClient
import dev.alllexey.itmoapi.core.readSetCookies
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The ITMO.ID login form flow of 1.x `AuthHelper.auth`: the browser's public client and PKCE,
 * with the form posted by this library instead of a browser.
 * Credentials go only to a form action on the issuer host and are never stored, logged or put in exceptions.
 * ITMO.ID cookies stay in this object only (no shared jar), so [cookieHeader] can replay the SSO session
 * for another public client such as BARS.
 */
internal class PasswordLogin(engine: HttpClientEngine, private val identity: ItmoIdClient) {
    private val client = lazy { noCookieClient(engine) }
    private val mutex = Mutex()
    private val cookies = linkedMapOf<String, String>()
    private val configuration get() = identity.configuration

    /** Wrong credentials, a locked account or an extra step such as 2FA end without a code: that is [MyItmoException.Auth]. */
    suspend fun login(username: String, password: String): TokenSet {
        require(username.isNotBlank() && password.isNotEmpty()) { "Username and password are required" }
        val verifier = Pkce.newVerifier()
        val state = Pkce.newState()
        val action = safely {
            val page = client.value.get(identity.loginUrl(Pkce.challenge(verifier), state)) { cookieHeader()?.let { header(HttpHeaders.Cookie, it) } }
            remember(page)
            if (page.status.value !in 200..299) throw MyItmoException.Http(page.status.value)
            findLoginAction(page.bodyAsText())
        }
        if (action == null || !onIssuerHost(action)) throw MyItmoException.Decode()
        val location = safely {
            val answer = client.value.post(action) {
                cookieHeader()?.let { header(HttpHeaders.Cookie, it) }
                setBody(FormDataContent(Parameters.build {
                    append("username", username)
                    append("password", password)
                    append("rememberMe", "on")
                }))
            }
            remember(answer)
            if (answer.status.value in 400..599 && answer.status.value != 401) throw MyItmoException.Http(answer.status.value)
            answer.headers[HttpHeaders.Location]
        }
        val code = location?.let { CallbackUrl(configuration.redirectUri, configuration.issuer).extractCode(it, state) }
            ?: throw MyItmoException.Auth(401)
        return identity.exchange(code, verifier)
    }

    /** Cookie header with the ITMO.ID session cookies received so far, or null before a login. */
    suspend fun cookieHeader(): String? = mutex.withLock {
        cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }.ifEmpty { null }
    }

    /** Keeps cookies ITMO.ID set or rotated; an empty value or Max-Age=0 removes a cookie. */
    suspend fun remember(setCookies: List<String>): Unit = mutex.withLock {
        for (line in setCookies) {
            val pair = line.substringBefore(';')
            val name = pair.substringBefore('=').trim()
            val value = pair.substringAfter('=', "").trim()
            if (name.isEmpty()) continue
            if (value.isEmpty() || line.contains("max-age=0", ignoreCase = true)) cookies.remove(name) else cookies[name] = value
        }
    }

    private suspend fun remember(response: HttpResponse) = remember(readSetCookies(response))

    private fun onIssuerHost(url: String): Boolean =
        StrictHttpsUrl.parse(url)?.host.let { it != null && it == StrictHttpsUrl.parse(configuration.issuer)?.host }

    private suspend fun <T> safely(action: suspend () -> T): T = try {
        action()
    } catch (failure: CancellationException) {
        throw failure
    } catch (failure: MyItmoException) {
        throw failure
    } catch (failure: Exception) {
        val seen = mutableSetOf<Throwable>()
        var cause: Throwable? = failure
        while (cause != null && seen.add(cause)) {
            if (isNetworkFailure(cause)) throw MyItmoException.Network(failure)
            cause = cause.cause
        }
        throw MyItmoException.Decode()
    }

    fun close() {
        if (client.isInitialized()) client.value.close()
    }

    override fun toString(): String = "PasswordLogin(redacted)"

    companion object {
        /** The form address as 1.x found it: `"loginAction": "..."` in the page config or `action="..."` in HTML. */
        fun findLoginAction(body: String): String? {
            for (key in listOf("\"loginAction\": \"", "\"loginAction\":\"", "action=\"")) {
                val start = body.indexOf(key).takeIf { it >= 0 }?.plus(key.length) ?: continue
                val end = body.indexOf('"', start).takeIf { it >= 0 } ?: continue
                return body.substring(start, end).replace("&amp;", "&")
            }
            return null
        }
    }
}
