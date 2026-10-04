package dev.alllexey.itmoapi.myitmo

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.itmoid.ItmoIdClient
import dev.alllexey.itmoapi.itmoid.TokenManager
import dev.alllexey.itmoapi.itmoid.TokenRefreshGuard
import dev.alllexey.itmoapi.itmoid.TokenStorage
import dev.alllexey.itmoapi.myitmo.election.ElectionApi
import dev.alllexey.itmoapi.myitmo.election.ElectionApiImpl
import dev.alllexey.itmoapi.myitmo.finances.FinancesApi
import dev.alllexey.itmoapi.myitmo.finances.FinancesApiImpl
import dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApi
import dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApiImpl
import dev.alllexey.itmoapi.myitmo.qr.QrApi
import dev.alllexey.itmoapi.myitmo.qr.QrApiImpl
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApi
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApiImpl
import dev.alllexey.itmoapi.myitmo.requests.RequestsApi
import dev.alllexey.itmoapi.myitmo.requests.RequestsApiImpl
import dev.alllexey.itmoapi.myitmo.schedule.ScheduleApi
import dev.alllexey.itmoapi.myitmo.schedule.ScheduleApiImpl
import dev.alllexey.itmoapi.myitmo.sport.SportApi
import dev.alllexey.itmoapi.myitmo.sport.SportApiImpl
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApi
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApiImpl
import dev.alllexey.itmoapi.myitmo.system.SystemApi
import dev.alllexey.itmoapi.myitmo.system.SystemApiImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Clock

/** Immutable authenticated MyITMO assembly with independently lazy areas.
 * Storage, engine and clock are injected; consumers own secure persistence and the engine lifetime.
 * Login exchanges use [identity], then [tokens] to replace the complete snapshot. Logout uses tokens too.
 * [refreshGuard] serializes rotations across consumers sharing an iOS App Group or other storage.
 * Closing releases initialized clients, not the injected engine; finish in-flight calls before closing.
 */
public class MyItmoClient(
    public val configuration: MyItmoConfiguration,
    storage: TokenStorage,
    engine: HttpClientEngine,
    clock: Clock,
    refreshGuard: TokenRefreshGuard = TokenRefreshGuard { it() },
) {
    /** Stateless login/exchange client using the same caller-owned engine and clock. */
    public val identity: ItmoIdClient = ItmoIdClient(engine, clock, configuration.itmoId)

    /** The sole session writer; consumers call forceRefresh for the missing-QR-pass retry. */
    public val tokens: TokenManager = TokenManager(storage, identity, clock, configuration.clockSkew, refreshGuard)

    private val transport = lazy {
        ItmoTransport(HttpClient(engine) {
            expectSuccess = false
            followRedirects = false
            install(ContentNegotiation) { json(ItmoApiJson) }
            install(MyItmoAuth) {
                this.tokens = this@MyItmoClient.tokens
                this.configuration = this@MyItmoClient.configuration
            }
        }, configuration.baseUrl)
    }

    /** Lazily constructs the schedule area without initializing unused areas. */
    public val schedule: ScheduleApi by lazy { ScheduleApiImpl(transport.value) }

    /** Lazily constructs the recordbook area without initializing unused areas. */
    public val recordBook: RecordBookApi by lazy { RecordBookApiImpl(transport.value) }

    /** Lazily constructs the personalities area without initializing unused areas. */
    public val personalities: PersonalitiesApi by lazy { PersonalitiesApiImpl(transport.value) }

    /** Lazily constructs the studyplan area without initializing unused areas. */
    public val studyplan: StudyPlanApi by lazy { StudyPlanApiImpl(transport.value) }

    /** Lazily constructs the qr area without initializing unused areas. */
    public val qr: QrApi by lazy { QrApiImpl(transport.value) }

    /** Lazily constructs the sport area without initializing unused areas. */
    public val sport: SportApi by lazy { SportApiImpl(transport.value) }

    /** Lazily constructs the election area without initializing unused areas. */
    public val election: ElectionApi by lazy { ElectionApiImpl(transport.value) }

    /** Lazily constructs the finances area without initializing unused areas. */
    public val finances: FinancesApi by lazy { FinancesApiImpl(transport.value) }

    /** Lazily constructs the requests area without initializing unused areas. */
    public val requests: RequestsApi by lazy { RequestsApiImpl(transport.value) }

    /** Lazily constructs the system area without initializing unused areas. */
    public val system: SystemApi by lazy { SystemApiImpl(transport.value) }

    /** Releases an initialized HTTP client; an unused shell stays entirely uninitialized. */
    public fun close(): Unit {
        if (transport.isInitialized()) transport.value.client.close()
        identity.close()
    }
}
