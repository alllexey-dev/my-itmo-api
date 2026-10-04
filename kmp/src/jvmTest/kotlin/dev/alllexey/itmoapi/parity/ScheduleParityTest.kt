package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.schedule.*
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.alllexey.itmoapi.myitmo.qr.areaExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.time.LocalDate as JavaDate
import retrofit2.Retrofit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import dev.alllexey.itmoapi.core.toQueryValue

class ScheduleParityTest {
    @Test
    fun `personal wire tree`() {
        assertParity<api.myitmo.model.DataResponse<List<api.myitmo.model.schedule.Schedule>>, DataResponse<List<Schedule>>>(
            "schedule/personal.json",
            DataResponse.serializer(ListSerializer(Schedule.serializer())),
        )
    }

    @Test
    fun `empty wire tree`() {
        assertParity<api.myitmo.model.DataResponse<List<api.myitmo.model.schedule.Schedule>>, DataResponse<List<Schedule>>>(
            "schedule/empty.json",
            DataResponse.serializer(ListSerializer(Schedule.serializer())),
        )
    }

    @Test
    fun `error wire tree`() {
        assertParity<api.myitmo.model.DataResponse<List<api.myitmo.model.schedule.Schedule>>, DataResponse<List<Schedule>>>(
            "schedule/error.json",
            DataResponse.serializer(ListSerializer(Schedule.serializer())),
        )
    }

    @Test
    fun `time-slots wire tree`() {
        assertParity<api.myitmo.model.DataResponse<List<api.myitmo.model.schedule.ExtendedTimeSlot>>, DataResponse<List<ExtendedTimeSlot>>>(
            "schedule/time-slots.json",
            DataResponse.serializer(ListSerializer(ExtendedTimeSlot.serializer())),
        )
    }

    @Test
    fun `date query matches legacy Retrofit request`() = runTest {
        val legacy = Retrofit.Builder().baseUrl("https://example.invalid/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(api.myitmo.MyItmo().gson))
            .build().create(api.myitmo.MyItmoApi::class.java)
            .getPersonalSchedule(JavaDate.of(2026, 1, 5), JavaDate.of(2026, 1, 11)).request().url
        val expected = legacy.queryParameterNames.associateWith { legacy.queryParameterValues(it).map { value -> requireNotNull(value) } }
        assertEquals("date_start=2026-01-05&date_end=2026-01-11", legacy.encodedQuery)
        areaExchange("/api/schedule/schedule/personal", fixture("schedule/empty.json"), expected) {
            ScheduleApiImpl(it).getPersonalSchedule(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11))
        }
    }
    /** Encoding seam only: the future sport port must compare its actual production API call. */
    @Test
    fun `repeated list query encoding matches legacy Retrofit`() = runTest {
        val legacyApi = Retrofit.Builder().baseUrl("https://example.invalid/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(api.myitmo.MyItmo().gson))
            .build().create(api.myitmo.MyItmoApi::class.java)
        for ((types, teachers) in listOf(
            listOf(3L, 1L, 3L) to listOf(200002L, 200001L),
            emptyList<Long>() to emptyList(),
            null to null,
        )) {
            val legacy = legacyApi.getSportSchedule(
                JavaDate.of(2026, 1, 5), JavaDate.of(2026, 1, 11), null, types, teachers,
            ).request().url
            var consumed = false
            val engine = MockEngine { request ->
                consumed = true
                assertEquals(legacy.encodedQuery, request.url.encodedQuery)
                respond("{}")
            }
            val client = HttpClient(engine)
            try {
                client.get("https://example.invalid/api/sport/sign/schedule") {
                    parameter("date_start", LocalDate(2026, 1, 5).toQueryValue())
                    parameter("date_end", LocalDate(2026, 1, 11).toQueryValue())
                    types.orEmpty().forEach { parameter("sport_type_id", it) }
                    teachers.orEmpty().forEach { parameter("teacher_isu", it) }
                }
                assertEquals(true, consumed)
            } finally {
                client.close()
                engine.close()
            }
        }
    }
}
