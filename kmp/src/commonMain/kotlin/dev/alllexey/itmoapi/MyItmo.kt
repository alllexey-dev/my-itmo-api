package dev.alllexey.itmoapi

import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.bars.BarsCodeSupplier
import dev.alllexey.itmoapi.bars.BarsConfiguration
import dev.alllexey.itmoapi.bars.auth.BarsLogin
import dev.alllexey.itmoapi.bars.auth.BarsSessionCode
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.defaultEngine
import dev.alllexey.itmoapi.itmoid.InMemoryTokenStorage
import dev.alllexey.itmoapi.itmoid.PasswordLogin
import dev.alllexey.itmoapi.itmoid.TokenStorage
import dev.alllexey.itmoapi.myitmo.MyItmoClient
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import dev.alllexey.itmoapi.myitmo.election.ElectionApi
import dev.alllexey.itmoapi.myitmo.finances.FinancesApi
import dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApi
import dev.alllexey.itmoapi.myitmo.qr.QrApi
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApi
import dev.alllexey.itmoapi.myitmo.requests.RequestsApi
import dev.alllexey.itmoapi.myitmo.schedule.ScheduleApi
import dev.alllexey.itmoapi.myitmo.sport.SportApi
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApi
import dev.alllexey.itmoapi.myitmo.system.SystemApi
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CancellationException
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.time.Clock

/** The quick way in: one object with every MyITMO area and BARS, created from a login and password,
 * a refresh token or a stored session. It owns its HTTP engine and closes it in [close].
 * Advanced setups (browser login, Keychain storage, shared refresh locks) use [MyItmoClient] directly;
 * [client] exposes the one underneath, including [MyItmoClient.tokens] for logout and forced refresh.
 */
public class MyItmo private constructor(
    /** The underlying client; its areas are the same objects as this facade's. */
    public val client: MyItmoClient,
    private val engine: HttpClientEngine,
    private val ownsEngine: Boolean,
) : AutoCloseable {
    private val passwordLogin = PasswordLogin(engine, client.identity)
    private val barsConfiguration = BarsConfiguration(itmoId = client.configuration.itmoId)
    private val barsLogin = lazy { BarsLogin(engine, barsConfiguration) }
    private val barsClient = lazy {
        BarsClient(engine, barsConfiguration, codeSupplier = BarsCodeSupplier { state -> barsCode(state) })
    }

    public val schedule: ScheduleApi get() = client.schedule
    public val recordBook: RecordBookApi get() = client.recordBook
    public val personalities: PersonalitiesApi get() = client.personalities
    public val studyplan: StudyPlanApi get() = client.studyplan
    public val qr: QrApi get() = client.qr
    public val sport: SportApi get() = client.sport
    public val election: ElectionApi get() = client.election
    public val finances: FinancesApi get() = client.finances
    public val requests: RequestsApi get() = client.requests
    public val system: SystemApi get() = client.system

    /** BARS with a session obtained and renewed from the ITMO.ID sign-in of [login], without another password.
     * After [fromRefreshToken] or [fromStorage] there is no ITMO.ID cookie session: calls throw
     * [MyItmoException.Auth] until you sign in to BARS yourself (`BarsLogin` in a browser, then `bars.login(code)`).
     */
    public val bars: BarsClient get() = barsClient.value

    private suspend fun barsCode(state: String): String? {
        val cookies = passwordLogin.cookieHeader() ?: return null
        val result = barsLogin.value.requestCodeWithCookies(state, cookies)
        passwordLogin.remember(result.setCookies)
        return when (result.outcome) {
            BarsSessionCode.Outcome.CODE -> result.code
            BarsSessionCode.Outcome.HTTP_ERROR -> throw MyItmoException.Http(result.httpCode)
            BarsSessionCode.Outcome.LOGIN_REQUIRED, BarsSessionCode.Outcome.REJECTED -> null
        }
    }

    /** Releases HTTP clients and the engine. Finish in-flight calls first. */
    override fun close() {
        if (barsClient.isInitialized()) barsClient.value.close()
        if (barsLogin.isInitialized()) barsLogin.value.close()
        passwordLogin.close()
        client.close()
        if (ownsEngine) engine.close()
    }

    override fun toString(): String = "MyItmo(redacted)"

    public companion object {
        /** Signs in with an ITMO.ID login and password, as 1.x `MyItmo.auth` did, and keeps the tokens in [storage].
         * The password is sent once to the ITMO.ID form and is not stored. Wrong credentials, a locked
         * account or an extra sign-in step (for example 2FA) throw [MyItmoException.Auth].
         */
        @JvmStatic
        @JvmOverloads
        @Throws(MyItmoException::class, CancellationException::class)
        public suspend fun login(
            username: String,
            password: String,
            storage: TokenStorage = InMemoryTokenStorage(),
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): MyItmo = create(defaultEngine(), true, storage, configuration, Clock.System).signIn(username, password)

        /** Starts from a refresh token (for example from a previous run or another app) and checks it at once;
         * a rejected or expired token throws [MyItmoException.Auth].
         */
        @JvmStatic
        @JvmOverloads
        @Throws(MyItmoException::class, CancellationException::class)
        public suspend fun fromRefreshToken(
            refreshToken: String,
            storage: TokenStorage = InMemoryTokenStorage(),
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): MyItmo = create(defaultEngine(), true, storage, configuration, Clock.System).refreshWith(refreshToken)

        /** Uses the session already in [storage] without a network call; an empty storage makes calls throw
         * [MyItmoException.Auth]. Read `storage.read() != null` first to choose between this and [login].
         */
        @JvmStatic
        @JvmOverloads
        public fun fromStorage(
            storage: TokenStorage,
            configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
        ): MyItmo = create(defaultEngine(), true, storage, configuration, Clock.System)

        internal fun create(
            engine: HttpClientEngine,
            ownsEngine: Boolean,
            storage: TokenStorage,
            configuration: MyItmoConfiguration,
            clock: Clock,
        ): MyItmo = MyItmo(MyItmoClient(configuration, storage, engine, clock), engine, ownsEngine)
    }

    internal suspend fun signIn(username: String, password: String): MyItmo = closingOnFailure {
        client.tokens.replaceTokens(passwordLogin.login(username, password))
    }

    internal suspend fun refreshWith(refreshToken: String): MyItmo = closingOnFailure {
        client.tokens.replaceTokens(client.identity.refresh(refreshToken))
    }

    private suspend fun closingOnFailure(action: suspend () -> Unit): MyItmo {
        try {
            action()
        } catch (failure: Throwable) {
            close()
            throw failure
        }
        return this
    }
}
