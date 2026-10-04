package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.isNetworkFailure
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/** Stateless ITMO.ID authorization-code exchange and one-shot refresh.
 * Engine, configuration and Clock are caller-owned; no storage, password flow or shared token state exists.
 * Transient failures are never Auth: consumers must not clear a stored session for Http/Decode/Network.
 */
public class ItmoIdClient(
    engine: HttpClientEngine,
    private val clock: Clock,
    public val configuration: ItmoIdConfiguration = ItmoIdConfiguration(),
) {
    internal val transport: Lazy<ItmoTransport> = lazy {
        ItmoTransport(HttpClient(engine) {
            expectSuccess = false
            followRedirects = false
            install(ContentNegotiation) { json(ItmoApiJson) }
        }, Url(configuration.issuer))
    }

    /** Builds the official authorization URL; callers own state/verifier and show the URL in their browser.
     * The observed request uses protocol=oauth2, response_type=code and scope=openid profile.
     */
    public fun loginUrl(challenge: String, state: String): String {
        require(challenge.length == 43 && challenge.all { it.isLetterOrDigit() && it.code < 128 || it == '-' || it == '_' }) {
            "Invalid PKCE challenge"
        }
        require(state.isNotBlank()) { "Invalid authorization state" }
        return URLBuilder(configuration.authorizationEndpoint).apply {
            parameters.append("protocol", "oauth2")
            parameters.append("response_type", "code")
            parameters.append("client_id", configuration.clientId)
            parameters.append("redirect_uri", configuration.redirectUri)
            parameters.append("scope", "openid profile")
            parameters.append("state", state)
            parameters.append("code_challenge_method", "S256")
            parameters.append("code_challenge", challenge)
        }.buildString()
    }

    /** Exchanges a validated callback code and its caller-owned PKCE verifier for one complete token set.
     * Form fields preserve the observed 1.x exchange, including response_type=code.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun exchange(code: String, verifier: String): TokenSet {
        if (code.isBlank()) throw MyItmoException.Decode()
        try { Pkce.challenge(verifier) } catch (_: IllegalArgumentException) { throw MyItmoException.Decode() }
        return requestTokens(Parameters.build {
            append("code", code)
            append("client_id", configuration.clientId)
            append("redirect_uri", configuration.redirectUri)
            append("response_type", "code")
            append("grant_type", "authorization_code")
            append("code_verifier", verifier)
        })
    }

    /** One-shot refresh with no storage mutation or implicit retries.
     * The 1.x wire form intentionally sends scopes=openid profile (plural), not scope.
     * OAuth errors are Auth; transport/server/decode failures leave session decisions to the caller.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun refresh(refreshToken: String): TokenSet {
        if (refreshToken.isBlank()) throw MyItmoException.Decode()
        return requestTokens(Parameters.build {
            append("refresh_token", refreshToken)
            append("scopes", "openid profile")
            append("client_id", configuration.clientId)
            append("grant_type", "refresh_token")
        })
    }

    private suspend fun requestTokens(parameters: Parameters): TokenSet {
        val status: Int
        val body: String
        try {
            val response = transport.value.client.post(configuration.tokenEndpoint) { setBody(FormDataContent(parameters)) }
            status = response.status.value
            body = response.bodyAsText()
        } catch (failure: CancellationException) {
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
        if (status in 500..599) throw MyItmoException.Http(status)
        val tree = try { ItmoApiJson.parseToJsonElement(body) } catch (_: SerializationException) { null }
        val error = ((tree as? JsonObject)?.get("error") as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (!error.isNullOrBlank()) throw MyItmoException.Auth(status)
        if (status !in 200..299) throw MyItmoException.Http(status)
        val wire = try {
            if (tree == null) throw MyItmoException.Decode()
            ItmoApiJson.decodeFromJsonElement(TokenWire.serializer(), tree)
        } catch (_: SerializationException) {
            throw MyItmoException.Decode()
        }
        if (wire.accessToken.isBlank() || wire.refreshToken.isBlank() || wire.idToken.isBlank() || wire.expiresIn < 0 || wire.refreshExpiresIn < 0) {
            throw MyItmoException.Decode()
        }
        val accessLifetime = wire.expiresIn.seconds
        val refreshLifetime = wire.refreshExpiresIn.seconds
        if (!accessLifetime.isFinite() || !refreshLifetime.isFinite()) throw MyItmoException.Decode()
        val issuedAt = clock.now()
        val accessExpiresAt = issuedAt + accessLifetime
        val refreshExpiresAt = issuedAt + refreshLifetime
        if (accessExpiresAt - issuedAt != accessLifetime || refreshExpiresAt - issuedAt != refreshLifetime) throw MyItmoException.Decode()
        return TokenSet(wire.accessToken, accessExpiresAt, wire.refreshToken, refreshExpiresAt, wire.idToken)
    }

    /** Releases an initialized client, without initializing an unused shell or closing the injected engine. */
    public fun close(): Unit {
        if (transport.isInitialized()) transport.value.client.close()
    }
}

/** Observed ITMO.ID token fields: required opaque strings and lifetimes in seconds.
 * Missing/null/blank tokens or missing/negative lifetimes are rejected, never defaulted into partial snapshots.
 * session_state is observed but not part of the five-field Storage snapshot; extra fields are ignored.
 */
@Serializable
internal class TokenWire(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long,
    @SerialName("id_token") val idToken: String,
) {
    override fun toString(): String = "TokenWire(redacted)"
}
