package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** MyITMO sports operations used by schedule, enrollment and score consumers. */
public interface SportApi : SportRemainingApi {
    /** GET /api/sport/time_slots; Sports lesson intervals. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportTimeSlots(): ResultResponse<List<TimeSlot>>

    /** GET /api/sport/sign/schedule/filters; Available schedule filter options, not a complete venue catalog. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportFilters(): ResultResponse<SportFilters>

    /** GET /api/sport/sign/schedule; Lessons available for enrollment in the inclusive date range. Lists produce repeated filters; null and empty lists omit the filter. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSchedule(dateStart: LocalDate, dateEnd: LocalDate, buildingId: Long? = null, sportTypeIds: List<Long>? = null, teacherIsu: List<Long>? = null): ResultResponse<List<SportSchedule>>

    /** GET /api/sport/personal/score; Points and award history for the selected sports semester. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportScore(semesterId: Long? = null): ResultResponse<SportScore>

    /** GET /api/sport/personal/have_attempts; Used and remaining enrollment attempts. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportAttempts(): ResultResponse<SportAttempts>

    /** GET /api/sport/semesters/list; Sports semesters for the history selector. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSemesters(): ResultResponse<List<SportSemesterOption>>

    /** GET /api/sport/semesters/current; Current sports semester and enrollment control dates. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getCurrentSportSemester(): ResultResponse<SportSemester>

    /** GET /api/sport/sign/schedule/limits; Limits grouped by raw server identifiers; an empty map means no special restrictions. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSignLimits(): ResultResponse<Map<Long, Map<Long, SportSignLimit>>>

    /** GET /api/sport/sign/chosen; Selected sections, lesson groups and regular slots. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getChosenSportSections(): ResultResponse<List<ChosenSportSection>>

    /** POST /api/sport/sign/schedule/lessons; Enroll in lesson IDs; returns successful enrollment IDs. Code 137 carries localized reasons in error_message (daily/weekly limit or overlapping enrollment). */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun signInLessons(lessonIds: List<Long>): ResultResponse<List<Long>>

    /** DELETE /api/sport/sign/schedule/lessons; Withdraw from lesson IDs using DELETE with a JSON body; returns withdrawn IDs. Code 130 carries reasons in error_message, including not enrolled. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun signOutLessons(lessonIds: List<Long>): ResultResponse<List<Long>>

}

internal class SportApiImpl(private val transport: ItmoTransport) : SportApi, SportRemainingApi by SportRemainingApiImpl(transport) {
    override suspend fun getSportTimeSlots(): ResultResponse<List<TimeSlot>> = transport.execute(
        ResultResponse.serializer(ListSerializer(TimeSlot.serializer())), HttpMethod.Get, "api/sport/time_slots",
    ) {
        url { encodedPath = "/api/sport/time_slots" }
    }

    override suspend fun getSportFilters(): ResultResponse<SportFilters> = transport.execute(
        ResultResponse.serializer(SportFilters.serializer()), HttpMethod.Get, "api/sport/sign/schedule/filters",
    ) {
        url { encodedPath = "/api/sport/sign/schedule/filters" }
    }

    override suspend fun getSportSchedule(dateStart: LocalDate, dateEnd: LocalDate, buildingId: Long?, sportTypeIds: List<Long>?, teacherIsu: List<Long>?): ResultResponse<List<SportSchedule>> = transport.execute(
        ResultResponse.serializer(ListSerializer(SportSchedule.serializer())), HttpMethod.Get, "api/sport/sign/schedule",
    ) {
        url { encodedPath = "/api/sport/sign/schedule" }
        parameter("date_start", dateStart.toQueryValue())
        parameter("date_end", dateEnd.toQueryValue())
        if (buildingId != null) parameter("building_id", buildingId)
        sportTypeIds.orEmpty().forEach { parameter("sport_type_id", it) }
        teacherIsu.orEmpty().forEach { parameter("teacher_isu", it) }
    }

    override suspend fun getSportScore(semesterId: Long?): ResultResponse<SportScore> = transport.execute(
        ResultResponse.serializer(SportScore.serializer()), HttpMethod.Get, "api/sport/personal/score",
    ) {
        url { encodedPath = "/api/sport/personal/score" }
        if (semesterId != null) parameter("semester_id", semesterId)
    }

    override suspend fun getSportAttempts(): ResultResponse<SportAttempts> = transport.execute(
        ResultResponse.serializer(SportAttempts.serializer()), HttpMethod.Get, "api/sport/personal/have_attempts",
    ) {
        url { encodedPath = "/api/sport/personal/have_attempts" }
    }

    override suspend fun getSportSemesters(): ResultResponse<List<SportSemesterOption>> = transport.execute(
        ResultResponse.serializer(ListSerializer(SportSemesterOption.serializer())), HttpMethod.Get, "api/sport/semesters/list",
    ) {
        url { encodedPath = "/api/sport/semesters/list" }
    }

    override suspend fun getCurrentSportSemester(): ResultResponse<SportSemester> = transport.execute(
        ResultResponse.serializer(SportSemester.serializer()), HttpMethod.Get, "api/sport/semesters/current",
    ) {
        url { encodedPath = "/api/sport/semesters/current" }
    }

    override suspend fun getSportSignLimits(): ResultResponse<Map<Long, Map<Long, SportSignLimit>>> = transport.execute(
        ResultResponse.serializer(MapSerializer(Long.serializer(), MapSerializer(Long.serializer(), SportSignLimit.serializer()))), HttpMethod.Get, "api/sport/sign/schedule/limits",
    ) {
        url { encodedPath = "/api/sport/sign/schedule/limits" }
    }

    override suspend fun getChosenSportSections(): ResultResponse<List<ChosenSportSection>> = transport.execute(
        ResultResponse.serializer(ListSerializer(ChosenSportSection.serializer())), HttpMethod.Get, "api/sport/sign/chosen",
    ) {
        url { encodedPath = "/api/sport/sign/chosen" }
    }

    override suspend fun signInLessons(lessonIds: List<Long>): ResultResponse<List<Long>> = transport.execute(
        ResultResponse.serializer(ListSerializer(Long.serializer())), HttpMethod.Post, "api/sport/sign/schedule/lessons",
    ) {
        url { encodedPath = "/api/sport/sign/schedule/lessons" }
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(Long.serializer()), lessonIds))
    }

    override suspend fun signOutLessons(lessonIds: List<Long>): ResultResponse<List<Long>> = transport.execute(
        ResultResponse.serializer(ListSerializer(Long.serializer())), HttpMethod.Delete, "api/sport/sign/schedule/lessons",
    ) {
        url { encodedPath = "/api/sport/sign/schedule/lessons" }
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(Long.serializer()), lessonIds))
    }

}
