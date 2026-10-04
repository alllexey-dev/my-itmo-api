package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.IdValuePair
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Additional MyITMO sports operations, inherited by [SportApi]. */
public interface SportRemainingApi {
    /** GET /api/sport/sport_types; Sports type catalog. Values and identifiers are server-managed. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportTypes(): ResultResponse<List<IdValuePair>>

    /** GET /api/sport/personal/sign_attempts; Separate server enrollment-attempt counter. Its exact meaning is undocumented; use getSportAttempts for UI decisions. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSignAttempts(): ResultResponse<Int>

    /** GET /api/sport/personal/calendar; Personal sports calendar, including past lessons, in the inclusive date range. Dates are sent as ISO yyyy-MM-dd. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getPersonalSportCalendar(dateStart: LocalDate, dateEnd: LocalDate): ResultResponse<List<SportSchedule>>

    /** GET /api/sport/personal/debt; Physical-education debt status and optional points and attempts required to clear it. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportDebt(): ResultResponse<SportDebt>

    /** GET /api/sport/personal/externat; Sports externship application status and optional refusal reason. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportExternat(): ResultResponse<SportExternat>

    /** GET /api/sport/personal/health_level; Assigned medical health group, nested under health_level. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportHealthLevel(): ResultResponse<SportHealthLevelResponse>

    /** GET /api/sport/personal/selections; Available sports selections and their levels and qualification standards. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSelections(): ResultResponse<List<SportSelection>>

    /** GET /api/sport/projects/list; Special sports-credit formats, including externships, with capacities and prerequisites. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportProjects(): ResultResponse<List<SportProject>>

}

internal class SportRemainingApiImpl(private val transport: ItmoTransport) : SportRemainingApi {
    override suspend fun getSportTypes(): ResultResponse<List<IdValuePair>> = transport.execute(
        ResultResponse.serializer(ListSerializer(IdValuePair.serializer())), HttpMethod.Get, "api/sport/sport_types",
    ) {
        url { encodedPath = "/api/sport/sport_types" }
    }

    override suspend fun getSportSignAttempts(): ResultResponse<Int> = transport.execute(
        ResultResponse.serializer(Int.serializer()), HttpMethod.Get, "api/sport/personal/sign_attempts",
    ) {
        url { encodedPath = "/api/sport/personal/sign_attempts" }
    }

    override suspend fun getPersonalSportCalendar(dateStart: LocalDate, dateEnd: LocalDate): ResultResponse<List<SportSchedule>> = transport.execute(
        ResultResponse.serializer(ListSerializer(SportSchedule.serializer())), HttpMethod.Get, "api/sport/personal/calendar",
    ) {
        url { encodedPath = "/api/sport/personal/calendar" }
        parameter("date_start", dateStart.toQueryValue())
        parameter("date_end", dateEnd.toQueryValue())
    }

    override suspend fun getSportDebt(): ResultResponse<SportDebt> = transport.execute(
        ResultResponse.serializer(SportDebt.serializer()), HttpMethod.Get, "api/sport/personal/debt",
    ) {
        url { encodedPath = "/api/sport/personal/debt" }
    }

    override suspend fun getSportExternat(): ResultResponse<SportExternat> = transport.execute(
        ResultResponse.serializer(SportExternat.serializer()), HttpMethod.Get, "api/sport/personal/externat",
    ) {
        url { encodedPath = "/api/sport/personal/externat" }
    }

    override suspend fun getSportHealthLevel(): ResultResponse<SportHealthLevelResponse> = transport.execute(
        ResultResponse.serializer(SportHealthLevelResponse.serializer()), HttpMethod.Get, "api/sport/personal/health_level",
    ) {
        url { encodedPath = "/api/sport/personal/health_level" }
    }

    override suspend fun getSportSelections(): ResultResponse<List<SportSelection>> = transport.execute(
        ResultResponse.serializer(ListSerializer(SportSelection.serializer())), HttpMethod.Get, "api/sport/personal/selections",
    ) {
        url { encodedPath = "/api/sport/personal/selections" }
    }

    override suspend fun getSportProjects(): ResultResponse<List<SportProject>> = transport.execute(
        ResultResponse.serializer(ListSerializer(SportProject.serializer())), HttpMethod.Get, "api/sport/projects/list",
    ) {
        url { encodedPath = "/api/sport/projects/list" }
    }

}
