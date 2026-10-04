package dev.alllexey.itmoapi.myitmo

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.createItmoHttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Url
import dev.alllexey.itmoapi.myitmo.schedule.ScheduleApi
import dev.alllexey.itmoapi.myitmo.schedule.ScheduleApiImpl
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApi
import dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApiImpl
import dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApi
import dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApiImpl
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApi
import dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApiImpl
import dev.alllexey.itmoapi.myitmo.qr.QrApi
import dev.alllexey.itmoapi.myitmo.qr.QrApiImpl
import dev.alllexey.itmoapi.myitmo.sport.SportApi
import dev.alllexey.itmoapi.myitmo.sport.SportApiImpl
import dev.alllexey.itmoapi.myitmo.election.ElectionApi
import dev.alllexey.itmoapi.myitmo.election.ElectionApiImpl
import dev.alllexey.itmoapi.myitmo.finances.FinancesApi
import dev.alllexey.itmoapi.myitmo.finances.FinancesApiImpl
import dev.alllexey.itmoapi.myitmo.requests.RequestsApi
import dev.alllexey.itmoapi.myitmo.requests.RequestsApiImpl
import dev.alllexey.itmoapi.myitmo.system.SystemApi
import dev.alllexey.itmoapi.myitmo.system.SystemApiImpl

/** MyITMO client shell with independently lazy areas and an explicitly injected engine.
 * Authentication assembly belongs to ML-04b; no global client or token state is allocated.
 * Closing the shell closes its client, not the caller-owned engine.
 */
public class MyItmoClient(engine: HttpClientEngine, baseUrl: Url = Url("https://my.itmo.ru/")) {
    private val transport = lazy { ItmoTransport(createItmoHttpClient(engine), baseUrl) }

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
    }
}
