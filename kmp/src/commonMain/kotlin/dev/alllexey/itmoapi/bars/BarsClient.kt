package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.bars.model.*
import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import dev.alllexey.itmoapi.core.isNetworkFailure
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlin.uuid.Uuid

/** BARS client with caller-owned engine, storage and optional one-shot session renewal.
 * The complete authorization response header is the session, lasting about 30 minutes.
 * Period selection changes the user's server setting, shared with the BARS web client.
 * No password flow, shared cookies or global token state is used.
 * Use core.defaultEngine for the verified ADR 0012 policy. An injected engine must already
 * disable native cookies, caches and redirects: a prebuilt engine cannot be sanitized here.
 * Ktor redirects are disabled and no cookie-storage or cache plugin is installed.
 */
public class BarsClient(
    engine: HttpClientEngine,
    public val configuration: BarsConfiguration = BarsConfiguration(),
    public val storage: BarsStorage = RuntimeBarsStorage(),
    public val codeSupplier: BarsCodeSupplier? = null,
) {
    internal val transport: Lazy<ItmoTransport> = lazy {
        ItmoTransport(HttpClient(engine) {
            expectSuccess = false
            followRedirects = false
            install(ContentNegotiation) { json(ItmoApiJson) }
        }, configuration.restUrl)
    }
    private val sessionMutex = Mutex()
    private val periodMutex = Mutex()

    /** Compatibility with the injectable client shell's custom base URL. */
    public constructor(engine: HttpClientEngine, baseUrl: Url) : this(engine, BarsConfiguration(restUrl = baseUrl))

    /** Exchanges an OIDC code once and persists the raw, validated response authorization header. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun login(code: String): Unit = sessionMutex.withLock { loginUnlocked(code) }

    /** Whether caller storage contains a session; no network probe is performed. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun hasSession(): Boolean = storage.getAuthorization() != null

    /** Explicitly clears the local session, never as a side effect of network/server errors. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun logout(): Unit = sessionMutex.withLock { storage.setAuthorization(null) }

    /** Current user and server-selected year/season. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getCurrentUser(): User = execute(User.serializer(), HttpMethod.Get, "users/current_user/")

    /** Global name/value settings, including current_year and current_term. Values may be null. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getConfig(): List<Setting> = execute(ListSerializer(Setting.serializer()), HttpMethod.Get, "config/")

    /** Saves a personal setting; id is excluded from the submitted body. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun setPersonalSetting(setting: Setting): Setting = periodMutex.withLock {
        setPersonalSettingUnlocked(setting)
    }

    /** Disciplines of the selected period; true restricts the list to disciplines with plans. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDisciplines(withCheckpointPlansOnly: Boolean? = null): List<Discipline> =
        execute(ListSerializer(Discipline.serializer()), HttpMethod.Get, "journal/disciplines") {
            withCheckpointPlansOnly?.let { url.parameters.append("withCheckpointPlansOnly", it.toString()) }
        }

    /** Groups/flows of the selected period, optionally filtered by a BARS discipline id. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getGroupsAndFlows(disciplineId: Long? = null): List<GroupOrFlow> =
        execute(ListSerializer(GroupOrFlow.serializer()), HttpMethod.Get, "journal/groups-and-flows") {
            disciplineId?.let { url.parameters.append("disciplineId", it.toString()) }
        }

    /** Own journal; /student restricts rows to the current learner. A wrong period may return 401.
     * Type and identifier are opaque path segments and are encoded independently.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getStudentJournal(checkpointPlanId: Long, type: String, identifier: String): StudentJournal =
        execute(StudentJournal.serializer(), HttpMethod.Get, "marks") {
            url.appendPathSegments(checkpointPlanId.toString(), type, identifier, "student", encodeSlash = true)
        }

    /** Selects yyyy/yyyy and season, writes only changed settings and verifies the resulting period. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun selectPeriod(year: String, term: Term): User = periodMutex.withLock {
        selectPeriodUnlocked(year, term)
    }

    /** Selects the period and excludes other period mutations through this client until action returns.
     * Read requests and renewal remain usable inside action. Do not nest period-changing operations.
     * Other clients and the web UI can still change the server setting.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun <T> withPeriod(year: String, term: Term, action: suspend BarsClient.() -> T): T =
        periodMutex.withLock {
            selectPeriodUnlocked(year, term)
            action()
        }

    private suspend fun selectPeriodUnlocked(year: String, term: Term): User {
        require(year.matches(Regex("[0-9]{4}/[0-9]{4}"))) { "Year must look like 2025/2026" }
        val user = getCurrentUser()
        if (user.selectedYear == year && user.selectedTerm == term.wireValue) return user
        if (user.selectedYear != year) setPersonalSettingUnlocked(Setting(name = "current_year", value = year))
        if (user.selectedTerm != term.wireValue) {
            setPersonalSettingUnlocked(Setting(name = "current_term", value = term.wireValue.toString()))
        }
        return getCurrentUser().also {
            if (it.selectedYear != year || it.selectedTerm != term.wireValue) throw MyItmoException.Decode()
        }
    }

    private suspend fun setPersonalSettingUnlocked(setting: Setting): Setting =
        execute(Setting.serializer(), HttpMethod.Post, "config/personal") {
            contentType(ContentType.Application.Json)
            // The id is response-only, including when the caller supplies a previously read Setting.
            setBody(kotlinx.serialization.json.buildJsonObject {
                put("name", kotlinx.serialization.json.JsonPrimitive(setting.name))
                setting.value?.let { put("value", kotlinx.serialization.json.JsonPrimitive(it)) }
            })
        }

    private suspend fun loginUnlocked(code: String) {
        if (code.isBlank()) throw MyItmoException.Decode()
        val response = perform(HttpMethod.Get, "login", null) {
            url.parameters.append("code", code)
            url.parameters.append("customRedirectUri", configuration.redirectUri)
        }
        requireSuccess(response.status.value)
        val authorization = response.headers[HttpHeaders.Authorization]
        if (!isValidAuthorization(authorization)) throw MyItmoException.Decode()
        storage.setAuthorization(authorization)
    }

    private suspend fun renew(rejected: String?): Boolean = sessionMutex.withLock {
        val current = storage.getAuthorization()
        if (current != null && current != rejected) return@withLock true
        val supplier = codeSupplier ?: return@withLock false
        val code = supplier.obtainCode(Uuid.random().toString()) ?: return@withLock false
        loginUnlocked(code)
        true
    }

    private suspend fun <T> execute(
        deserializer: DeserializationStrategy<T>,
        method: HttpMethod,
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        var authorization = storage.getAuthorization()
        val renewedBeforeRequest = authorization == null
        if (renewedBeforeRequest) {
            if (!renew(null)) throw MyItmoException.Auth(401)
            authorization = storage.getAuthorization()
        }
        var response = perform(method, path, authorization, configure)
        if (response.status.value == 401 && !renewedBeforeRequest && renew(authorization)) {
            authorization = storage.getAuthorization()
            response = perform(method, path, authorization, configure)
        }
        requireSuccess(response.status.value)
        val rotated = response.headers[HttpHeaders.Authorization]
        if (isValidAuthorization(rotated)) sessionMutex.withLock {
            // A delayed response cannot replace a session renewed by another request.
            if (storage.getAuthorization() == authorization) storage.setAuthorization(rotated)
        }
        val body = readBody(response)
        return try {
            ItmoApiJson.decodeFromString(deserializer, body)
        } catch (_: SerializationException) {
            throw MyItmoException.Decode()
        } catch (_: IllegalArgumentException) {
            throw MyItmoException.Decode()
        }
    }

    private suspend fun perform(
        method: HttpMethod,
        path: String,
        authorization: String?,
        configure: HttpRequestBuilder.() -> Unit,
    ): HttpResponse = safely {
        transport.value.client.request {
            this.method = method
            url {
                takeFrom(configuration.restUrl)
                appendPathSegments(path)
            }
            if (authorization != null) headers.append(HttpHeaders.Authorization, authorization)
            configure()
        }
    }

    private suspend fun readBody(response: HttpResponse): String = safely { response.bodyAsText() }

    private suspend fun <T> safely(action: suspend () -> T): T = try {
        action()
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

    private fun requireSuccess(status: Int) {
        if (status == 401 || status == 403) throw MyItmoException.Auth(status)
        if (status !in 200..299) throw MyItmoException.Http(status)
    }

    /** Releases the initialized client without forcing initialization or closing the caller's engine. */
    public fun close(): Unit {
        if (transport.isInitialized()) transport.value.client.close()
    }

    public companion object {
        /** Exact 1.x persisted-header validation: Bearer prefix, 16..16384 characters, no CR/LF. */
        public fun isValidAuthorization(value: String?): Boolean = value != null && value.startsWith("Bearer ") &&
            value.length in 16..16384 && '\n' !in value && '\r' !in value
    }
}
