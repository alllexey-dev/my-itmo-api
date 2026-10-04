package dev.alllexey.itmoapi.myitmo.schedule

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.DataResponse
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Personal academic schedule; both endpoints use DataResponse rather than ResultResponse. */
public interface ScheduleApi {
    /** GET /api/schedule/schedule/personal for an inclusive date range.
     * date_start and date_end are explicitly formatted as ISO yyyy-MM-dd calendar dates.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getPersonalSchedule(dateStart: LocalDate, dateEnd: LocalDate): DataResponse<List<Schedule>>

    /** GET /api/schedule/meta/time_slots; returns standard lesson intervals with display order. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTimeSlots(): DataResponse<List<ExtendedTimeSlot>>
}

internal class ScheduleApiImpl(private val transport: ItmoTransport) : ScheduleApi {
    override suspend fun getPersonalSchedule(dateStart: LocalDate, dateEnd: LocalDate): DataResponse<List<Schedule>> =
        transport.execute(
            DataResponse.serializer(ListSerializer(Schedule.serializer())),
            HttpMethod.Get,
            "api/schedule/schedule/personal",
        ) {
            url { encodedPath = "/api/schedule/schedule/personal" }
            parameter("date_start", dateStart.toQueryValue())
            parameter("date_end", dateEnd.toQueryValue())
        }

    override suspend fun getTimeSlots(): DataResponse<List<ExtendedTimeSlot>> = transport.execute(
        DataResponse.serializer(ListSerializer(ExtendedTimeSlot.serializer())), HttpMethod.Get,
        "api/schedule/meta/time_slots",
    ) { url { encodedPath = "/api/schedule/meta/time_slots" } }
}
