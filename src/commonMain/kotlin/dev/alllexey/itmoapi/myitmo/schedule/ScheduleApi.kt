package dev.alllexey.itmoapi.myitmo.schedule

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Academic schedules. On the wire these endpoints use the `{code, data, message}` envelope, not `error_code`/`result`. */
public interface ScheduleApi {
    /** GET /api/schedule/schedule/personal for an inclusive date range.
     * date_start and date_end are explicitly formatted as ISO yyyy-MM-dd calendar dates.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getPersonalSchedule(dateStart: LocalDate, dateEnd: LocalDate): List<Schedule>

    /** GET /api/schedule/teacher/{isu} for an inclusive date range: the lessons a teacher gives, in the same day model
     * as the personal schedule (one entry per calendar day, empty days included). Any signed-in user may read it.
     * An ISU that teaches nothing in the range, a student's or an unknown one, answers with days without lessons.
     * Lessons at the same time for several groups come as separate lessons with their own pair_id and group.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTeacherSchedule(isu: Long, dateStart: LocalDate, dateEnd: LocalDate): List<Schedule>

    /** GET /api/schedule/meta/time_slots; returns standard lesson intervals with display order. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTimeSlots(): List<ExtendedTimeSlot>
}

internal class ScheduleApiImpl(private val transport: ItmoTransport) : ScheduleApi {
    override suspend fun getPersonalSchedule(dateStart: LocalDate, dateEnd: LocalDate): List<Schedule> =
        transport.data(
            ListSerializer(Schedule.serializer()),
            HttpMethod.Get,
            "api/schedule/schedule/personal",
        ) {
            parameter("date_start", dateStart.toQueryValue())
            parameter("date_end", dateEnd.toQueryValue())
        }

    override suspend fun getTeacherSchedule(isu: Long, dateStart: LocalDate, dateEnd: LocalDate): List<Schedule> =
        transport.data(
            ListSerializer(Schedule.serializer()),
            HttpMethod.Get,
            "api/schedule/teacher/$isu",
        ) {
            parameter("date_start", dateStart.toQueryValue())
            parameter("date_end", dateEnd.toQueryValue())
        }

    override suspend fun getTimeSlots(): List<ExtendedTimeSlot> = transport.data(
        ListSerializer(ExtendedTimeSlot.serializer()), HttpMethod.Get,
        "api/schedule/meta/time_slots",
    )
}
