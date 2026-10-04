package dev.alllexey.itmoapi.bars.auth

import dev.alllexey.itmoapi.bars.BarsConfiguration
import dev.alllexey.itmoapi.bars.auth.BarsSessionCode.Outcome
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.isNetworkFailure
import dev.alllexey.itmoapi.core.noCookieClient
import dev.alllexey.itmoapi.core.readSetCookies
import dev.alllexey.itmoapi.itmoid.CallbackUrl
import dev.alllexey.itmoapi.itmoid.Pkce
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel

/** Stateless BARS browser login and ADR 0012 cookie replay, without a password flow or shared SSO.
 * BARS exchanges its public-client code itself, so this authorization URL intentionally has no PKCE.
 * Inject [dev.alllexey.itmoapi.core.defaultEngine] for the verified no-cookie/no-cache/native-redirect policy;
 * a custom engine must already meet that precondition. The caller owns and closes the injected engine.
 */
public class BarsLogin(
    engine: HttpClientEngine,
    public val configuration: BarsConfiguration = BarsConfiguration(),
) {
    private val callback = CallbackUrl(configuration.redirectUri, configuration.issuer)
    private val client = lazy { noCookieClient(engine) }

    /** Generates a fresh unpredictable state; the caller retains it until callback validation. */
    public fun newState(): String = Pkce.newState()

    /** Builds the observed BARS OIDC request: response_type=code, scope=openid, client and redirect URI. */
    public fun loginUrl(state: String): String {
        require(state.isNotBlank()) { "Invalid authorization state" }
        return URLBuilder(configuration.issuer.trimEnd('/') + "/protocol/openid-connect/auth").apply {
            parameters.append("response_type", "code")
            parameters.append("scope", "openid")
            parameters.append("client_id", configuration.clientId)
            parameters.append("redirect_uri", configuration.redirectUri)
            parameters.append("state", state)
        }.buildString()
    }

    /** Exact HTTPS callback allow-list, including rejection of malformed, IDN and duplicate queries. */
    public fun isCallback(url: String): Boolean = callback.isCallback(url)

    /** Strict trusted issuer-host pages or the exact callback; intended for consumer WebView gating. */
    public fun isAllowedPage(url: String): Boolean = callback.isAllowedPage(url)

    /** Returns a code only after strict callback/state/query checks.
     * Every present iss must equal the issuer; absence preserves the owner-approved 1.x policy.
     */
    public fun extractCode(url: String, expectedState: String): String? = callback.extractCode(url, expectedState)

    /** One GET with the explicit Cookie header, no redirects, cache, shared cookie jar or body reads.
     * Blank/absent cookies return LOGIN_REQUIRED with status zero without a request.
     * Relative Location resolves against the authorization URL, not against the engine's routed wire URL.
     * Returns every Set-Cookie even for rejected/error outcomes. Network failures never mean session expiry.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun requestCodeWithCookies(state: String, cookieHeader: String?): BarsSessionCode {
        if (cookieHeader.isNullOrBlank()) return BarsSessionCode(Outcome.LOGIN_REQUIRED, null, 0, emptyList())
        val requestUrl = loginUrl(state)
        try {
            // Streaming execution avoids HttpStatement's default whole-body buffering.
            return client.value.prepareGet(requestUrl) { header(HttpHeaders.Cookie, cookieHeader) }.execute { response ->
                try {
                    val status = response.status.value
                    val cookies = readSetCookies(response)
                    if (status in 300..399) {
                        val location = response.headers[HttpHeaders.Location]
                            ?: return@execute BarsSessionCode(Outcome.HTTP_ERROR, null, status, cookies)
                        val target = resolve(requestUrl, location)
                        if (target != null && isCallback(target)) {
                            val code = extractCode(target, state)
                            return@execute BarsSessionCode(if (code == null) Outcome.REJECTED else Outcome.CODE, code, status, cookies)
                        }
                        val outcome = if (target != null && isAllowedPage(target)) Outcome.LOGIN_REQUIRED else Outcome.REJECTED
                        return@execute BarsSessionCode(outcome, null, status, cookies)
                    }
                    BarsSessionCode(if (status in 200..299) Outcome.LOGIN_REQUIRED else Outcome.HTTP_ERROR, null, status, cookies)
                } finally {
                    // Ktor cleanup joins a completed response job. Cancel it first to abort blocked engine body I/O.
                    response.cancel()
                }
            }
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
    }

    private fun resolve(base: String, location: String): String? {
        // Validate absolute redirects before Ktor can normalize away malicious syntax.
        if (location.any { it.code !in 33..126 || it in "\\\"<>^`{|}" }) return null
        if (Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(location)) return location
        if (location.startsWith("//")) return "https:$location"
        val authority = base.substring(0, base.indexOf('/', 8).let { if (it < 0) base.length else it })
        val path = base.substring(authority.length).substringBefore('?')
        return when {
            location.isEmpty() -> base
            location.startsWith('/') -> authority + location
            location.startsWith('?') || location.startsWith('#') -> authority + path + location
            else -> authority + path.substringBeforeLast('/') + "/" + location
        }
    }

    /** Closes an initialized client only, leaving the caller's engine usable. */
    public fun close(): Unit {
        if (client.isInitialized()) client.value.close()
    }
}
