package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.IdValuePair
import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** MyITMO sports operations used by schedule, enrollment and score consumers. */
public interface SportApi {
    /** GET /api/sport/time_slots; Sports lesson intervals. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportTimeSlots(): List<TimeSlot>

    /** GET /api/sport/sign/schedule/filters; Available schedule filter options, not a complete venue catalog. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportFilters(): SportFilters

    /** GET /api/sport/sign/schedule; Lessons available for enrollment in the inclusive date range. Lists produce repeated filters; null and empty lists omit the filter. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSchedule(dateStart: LocalDate, dateEnd: LocalDate, buildingId: Long? = null, sportTypeIds: List<Long>? = null, teacherIsu: List<Long>? = null): List<SportSchedule>

    /** GET /api/sport/personal/score; Points and award history for the selected sports semester. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportScore(semesterId: Long? = null): SportScore

    /** GET /api/sport/personal/have_attempts; Used and remaining enrollment attempts. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportAttempts(): SportAttempts

    /** GET /api/sport/semesters/list; Sports semesters for the history selector. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSemesters(): List<SportSemesterOption>

    /** GET /api/sport/semesters/current; Current sports semester and enrollment control dates. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getCurrentSportSemester(): SportSemester

    /** GET /api/sport/sign/schedule/limits; Limits grouped by raw server identifiers; an empty map means no special restrictions. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSignLimits(): Map<Long, Map<Long, SportSignLimit>>

    /** GET /api/sport/sign/chosen; Selected sections, lesson groups and regular slots. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getChosenSportSections(): List<ChosenSportSection>

    /** POST /api/sport/sign/schedule/lessons; Enroll in lesson IDs; returns successful enrollment IDs. Code 137 carries localized reasons in error_message (daily/weekly limit or overlapping enrollment). */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun signInLessons(lessonIds: List<Long>): List<Long>

    /**
     * POST /api/sport/sign/schedule/lessons with one lesson ID through [signInLessons], classified: booked, full,
     * refused by another rule, or no decision. Every [MyItmoException] becomes a result; see [SportSignResult].
     */
    @Throws(CancellationException::class)
    public suspend fun signInLesson(lessonId: Long): SportSignResult = try {
        signInLessons(listOf(lessonId))
        SportSignResult.Booked
    } catch (failure: MyItmoException) {
        sportSignResultOf(failure)
    }

    /** DELETE /api/sport/sign/schedule/lessons; Withdraw from lesson IDs using DELETE with a JSON body; returns withdrawn IDs. Code 130 carries reasons in error_message, including not enrolled. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun signOutLessons(lessonIds: List<Long>): List<Long>

    /** GET /api/sport/sport_types; Sports type catalog. Values and identifiers are server-managed. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportTypes(): List<IdValuePair>

    /** GET /api/sport/personal/sign_attempts; Separate server enrollment-attempt counter. Its exact meaning is undocumented; use getSportAttempts for UI decisions. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSignAttempts(): Int

    /** GET /api/sport/personal/calendar; Personal sports calendar, including past lessons, in the inclusive date range. Dates are sent as ISO yyyy-MM-dd. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getPersonalSportCalendar(dateStart: LocalDate, dateEnd: LocalDate): List<SportSchedule>

    /** GET /api/sport/personal/debt; Physical-education debt status and optional points and attempts required to clear it. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportDebt(): SportDebt

    /** GET /api/sport/personal/externat; Sports externship application status and optional refusal reason. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportExternat(): SportExternat

    /** GET /api/sport/personal/health_level; Assigned medical health group, nested under health_level. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportHealthLevel(): SportHealthLevelResponse

    /** GET /api/sport/personal/selections; Available sports selections and their levels and qualification standards. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportSelections(): List<SportSelection>

    /** GET /api/sport/projects/list; Special sports-credit formats, including externships, with capacities and prerequisites. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSportProjects(): List<SportProject>

}

internal class SportApiImpl(private val transport: ItmoTransport) : SportApi {
    override suspend fun getSportTimeSlots(): List<TimeSlot> = transport.result(
        ListSerializer(TimeSlot.serializer()), HttpMethod.Get, "api/sport/time_slots",
    )

    override suspend fun getSportFilters(): SportFilters = transport.result(
        SportFilters.serializer(), HttpMethod.Get, "api/sport/sign/schedule/filters",
    )

    override suspend fun getSportSchedule(dateStart: LocalDate, dateEnd: LocalDate, buildingId: Long?, sportTypeIds: List<Long>?, teacherIsu: List<Long>?): List<SportSchedule> = transport.result(
        ListSerializer(SportSchedule.serializer()), HttpMethod.Get, "api/sport/sign/schedule",
    ) {
        parameter("date_start", dateStart.toQueryValue())
        parameter("date_end", dateEnd.toQueryValue())
        if (buildingId != null) parameter("building_id", buildingId)
        sportTypeIds.orEmpty().forEach { parameter("sport_type_id", it) }
        teacherIsu.orEmpty().forEach { parameter("teacher_isu", it) }
    }

    override suspend fun getSportScore(semesterId: Long?): SportScore = transport.result(
        SportScore.serializer(), HttpMethod.Get, "api/sport/personal/score",
    ) {
        if (semesterId != null) parameter("semester_id", semesterId)
    }

    override suspend fun getSportAttempts(): SportAttempts = transport.result(
        SportAttempts.serializer(), HttpMethod.Get, "api/sport/personal/have_attempts",
    )

    override suspend fun getSportSemesters(): List<SportSemesterOption> = transport.result(
        ListSerializer(SportSemesterOption.serializer()), HttpMethod.Get, "api/sport/semesters/list",
    )

    override suspend fun getCurrentSportSemester(): SportSemester = transport.result(
        SportSemester.serializer(), HttpMethod.Get, "api/sport/semesters/current",
    )

    override suspend fun getSportSignLimits(): Map<Long, Map<Long, SportSignLimit>> = transport.result(
        MapSerializer(Long.serializer(), MapSerializer(Long.serializer(), SportSignLimit.serializer())), HttpMethod.Get, "api/sport/sign/schedule/limits",
    )

    override suspend fun getChosenSportSections(): List<ChosenSportSection> = transport.result(
        ListSerializer(ChosenSportSection.serializer()), HttpMethod.Get, "api/sport/sign/chosen",
    )

    override suspend fun signInLessons(lessonIds: List<Long>): List<Long> = transport.result(
        ListSerializer(Long.serializer()), HttpMethod.Post, "api/sport/sign/schedule/lessons",
    ) {
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(Long.serializer()), lessonIds))
    }

    override suspend fun signOutLessons(lessonIds: List<Long>): List<Long> = transport.result(
        ListSerializer(Long.serializer()), HttpMethod.Delete, "api/sport/sign/schedule/lessons",
    ) {
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(Long.serializer()), lessonIds))
    }

    override suspend fun getSportTypes(): List<IdValuePair> = transport.result(
        ListSerializer(IdValuePair.serializer()), HttpMethod.Get, "api/sport/sport_types",
    )

    override suspend fun getSportSignAttempts(): Int = transport.result(
        Int.serializer(), HttpMethod.Get, "api/sport/personal/sign_attempts",
    )

    override suspend fun getPersonalSportCalendar(dateStart: LocalDate, dateEnd: LocalDate): List<SportSchedule> = transport.result(
        ListSerializer(SportSchedule.serializer()), HttpMethod.Get, "api/sport/personal/calendar",
    ) {
        parameter("date_start", dateStart.toQueryValue())
        parameter("date_end", dateEnd.toQueryValue())
    }

    override suspend fun getSportDebt(): SportDebt = transport.result(
        SportDebt.serializer(), HttpMethod.Get, "api/sport/personal/debt",
    )

    override suspend fun getSportExternat(): SportExternat = transport.result(
        SportExternat.serializer(), HttpMethod.Get, "api/sport/personal/externat",
    )

    override suspend fun getSportHealthLevel(): SportHealthLevelResponse = transport.result(
        SportHealthLevelResponse.serializer(), HttpMethod.Get, "api/sport/personal/health_level",
    )

    override suspend fun getSportSelections(): List<SportSelection> = transport.result(
        ListSerializer(SportSelection.serializer()), HttpMethod.Get, "api/sport/personal/selections",
    )

    override suspend fun getSportProjects(): List<SportProject> = transport.result(
        ListSerializer(SportProject.serializer()), HttpMethod.Get, "api/sport/projects/list",
    )

}
