package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.isNetworkFailure
import dev.alllexey.itmoapi.core.noCookieClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock
import kotlin.time.Instant

/** The account's ITMO.ID sessions, as the id.itmo.ru `/activity` page sees and ends them.
 * Managing sessions needs a token of the id.itmo.ru front's public client `profile` ([profileToken]);
 * a MyITMO bearer gets 401. Transient failures are never Auth. Nothing here logs a token, cookie, sid or IP.
 * The caller owns and closes the injected engine; use [dev.alllexey.itmoapi.core.defaultEngine] for the
 * no-cookie/no-redirect policy.
 */
public class ItmoIdSessions(engine: HttpClientEngine, clock: Clock) {
    private val profile = ItmoIdClient(engine, clock, PROFILE)
    private val callback = CallbackUrl(PROFILE.redirectUri, PROFILE.issuer)
    private val client = lazy { noCookieClient(engine) }

    /** A `profile` access token from a live ITMO.ID SSO cookie (`KEYCLOAK_IDENTITY=...`), obtained silently like the
     * id.itmo.ru page does: PKCE authorize with the cookie, no redirects followed, the code checked against state
     * and redirect, then the code exchange. The same session (`sid`) as the cookie. Null when the cookie is blank
     * or dead (ITMO.ID shows its sign-in page instead of redirecting back).
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun profileToken(cookieHeader: String?): String? {
        if (cookieHeader.isNullOrBlank()) return null
        val verifier = Pkce.newVerifier()
        val state = Pkce.newState()
        val url = profile.loginUrl(Pkce.challenge(verifier), state)
        val location = send(HttpMethod.Get, url, cookieHeader = cookieHeader) { response ->
            when (val status = response.status.value) {
                in 200..299 -> null
                in 300..399 -> response.headers[HttpHeaders.Location] ?: throw MyItmoException.Http(status)
                else -> throw MyItmoException.Http(status)
            }
        } ?: return null
        val target = resolveLocation(url, location) ?: throw MyItmoException.Decode()
        if (!callback.isCallback(target)) {
            // An issuer page such as a required action means the cookie alone cannot sign in.
            if (callback.isAllowedPage(target)) return null
            throw MyItmoException.Decode()
        }
        val code = callback.extractCode(target, state) ?: throw MyItmoException.Decode()
        return profile.exchange(code, verifier).accessToken
    }

    /** `GET https://id.itmo.ru/api/profile/sessions`: every active session of the account, in server order.
     * 401 (an expired or non-`profile` bearer) is Auth.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun list(bearer: String): List<ItmoIdSession> {
        require(bearer.isNotBlank()) { "Bearer token is required" }
        val body = send(HttpMethod.Get, SESSIONS, bearer = bearer) { response ->
            requireSuccess(response.status.value)
            response.bodyAsText()
        }
        return try {
            ItmoApiJson.decodeFromString(ListSerializer(ItmoIdSession.serializer()), body)
        } catch (_: IllegalArgumentException) {
            throw MyItmoException.Decode() // includes SerializationException
        }
    }

    /** `DELETE https://id.itmo.ru/api/account/sessions/{sid}` with `Accept: application/json` (without it Keycloak's
     * HTML console answers 405). Ends that session: its refresh tokens and SSO cookies die, other sessions live;
     * a session may end itself. 204 and 404 (already gone) are success; 401 is Auth.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun end(bearer: String, sid: String): Unit {
        require(bearer.isNotBlank()) { "Bearer token is required" }
        require(sid.isNotEmpty() && sid.length <= 64 && sid.all { it.isLetterOrDigit() && it.code < 128 || it == '-' }) { "Invalid session id" }
        send(HttpMethod.Delete, "$ACCOUNT_SESSIONS/$sid", bearer = bearer) { response ->
            if (response.status.value != 404) requireSuccess(response.status.value)
        }
    }

    /** Releases initialized clients, leaving the caller's engine usable. */
    public fun close(): Unit {
        profile.close()
        if (client.isInitialized()) client.value.close()
    }

    private fun requireSuccess(status: Int) {
        if (status == 401) throw MyItmoException.Auth(status)
        if (status !in 200..299) throw MyItmoException.Http(status)
    }

    /** One request without redirects, cookie jar or cache; the response is cancelled after [read]. */
    private suspend fun <T> send(
        method: HttpMethod,
        url: String,
        bearer: String? = null,
        cookieHeader: String? = null,
        read: suspend (HttpResponse) -> T,
    ): T = try {
        client.value.prepareRequest(url) {
            this.method = method
            bearer?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            }
            cookieHeader?.let { header(HttpHeaders.Cookie, it) }
        }.execute { response ->
            try { read(response) } finally { response.cancel() }
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

    override fun toString(): String = "ItmoIdSessions(redacted)"

    private companion object {
        /** The id.itmo.ru front's public client: PKCE S256, scope `openid profile`. */
        val PROFILE: ItmoIdConfiguration = ItmoIdConfiguration(clientId = "profile", redirectUri = "https://id.itmo.ru/login/callback")
        private const val SESSIONS = "https://id.itmo.ru/api/profile/sessions"
        private const val ACCOUNT_SESSIONS = "https://id.itmo.ru/api/account/sessions"
    }
}

/** One row of id.itmo.ru `/activity` (`GET /api/profile/sessions`), a Keycloak user session.
 * Observed and not modelled: `osVersion`, `device`, `clients[] {clientName, offlineAccess}`.
 * Holds the session id and IP address: never log it; [toString] is redacted.
 */
@Serializable
public class ItmoIdSession(
    /** Session id (`id` on the wire), equal to the `sid` claim of the session's tokens ([TokenSet.sessionId]). */
    @SerialName("id") public val sid: String,
    /** Password sign-in that created the session; epoch seconds on the wire. */
    @Serializable(with = EpochSecondsSerializer::class) public val started: Instant,
    /** Last sign-in, refresh or silent authorize in the session; epoch seconds on the wire. */
    @Serializable(with = EpochSecondsSerializer::class) public val lastAccess: Instant,
    /** End of the session, `started` + 90 days whatever the cookie says; epoch seconds on the wire. */
    @Serializable(with = EpochSecondsSerializer::class) public val expires: Instant,
    /** Operating system parsed from the sign-in User-Agent, such as `Linux`, `Mac OS X` or `Other`. */
    public val os: String = "",
    /** Browser parsed from the sign-in User-Agent, such as `Chrome/141.0.0` or `ITMO Widgets agent bot/1.0`;
     * `Other/Unknown` for an unknown agent. A `{family, version}` object on the wire becomes `family/version`.
     */
    @Serializable(with = BrowserSerializer::class) public val browser: String = "",
    /** Client IP address as ITMO.ID saw it. */
    public val ipAddress: String = "",
    /** Whether the sign-in User-Agent is a mobile one. */
    public val mobile: Boolean = false,
) {
    init {
        require(sid.isNotBlank()) { "Session id is required" }
    }

    override fun toString(): String = "ItmoIdSession(redacted)"
}

internal object EpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EpochSeconds", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochSeconds(decoder.decodeLong())
    override fun serialize(encoder: Encoder, value: Instant): Unit = encoder.encodeLong(value.epochSeconds)
}

/** Accepts both shapes seen for `browser`: a `family/version` string or a `{family, version}` object. */
internal object BrowserSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ItmoIdBrowser", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        if (element is JsonPrimitive) return element.contentOrNull.orEmpty()
        val fields = (element as? JsonObject) ?: throw SerializationException("Unexpected browser shape")
        return listOf("family", "version").mapNotNull { (fields[it] as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotEmpty) }.joinToString("/")
    }

    override fun serialize(encoder: Encoder, value: String): Unit = encoder.encodeString(value)
}
