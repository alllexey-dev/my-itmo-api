package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.sport.*
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.*
import java.time.OffsetDateTime
import java.time.LocalDate as JavaDate

class SportParityTest {
    @Test
    fun `time-slots full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.TimeSlot>>, ResultResponse<List<TimeSlot>>>(
            "sport/time-slots.json", ResultResponse.serializer(ListSerializer(TimeSlot.serializer())),
        )
    }

    @Test
    fun `filters full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportFilters>, ResultResponse<SportFilters>>(
            "sport/filters.json", ResultResponse.serializer(SportFilters.serializer()),
        )
    }

    @Test
    fun `schedule full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportSchedule>>, ResultResponse<List<SportSchedule>>>(
            "sport/schedule.json", ResultResponse.serializer(ListSerializer(SportSchedule.serializer())),
        )
    }

    @Test
    fun `external-venue full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportSchedule>>, ResultResponse<List<SportSchedule>>>(
            "sport/external-venue.json", ResultResponse.serializer(ListSerializer(SportSchedule.serializer())),
        )
    }

    @Test
    fun `empty full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportSchedule>>, ResultResponse<List<SportSchedule>>>(
            "sport/empty.json", ResultResponse.serializer(ListSerializer(SportSchedule.serializer())),
        )
    }

    @Test
    fun `score full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportScore>, ResultResponse<SportScore>>(
            "sport/score.json", ResultResponse.serializer(SportScore.serializer()),
        )
    }

    @Test
    fun `score-empty full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportScore>, ResultResponse<SportScore>>(
            "sport/score-empty.json", ResultResponse.serializer(SportScore.serializer()),
        )
    }

    @Test
    fun `attempts full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportAttempts>, ResultResponse<SportAttempts>>(
            "sport/attempts.json", ResultResponse.serializer(SportAttempts.serializer()),
        )
    }

    @Test
    fun `semesters full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportSemesterOption>>, ResultResponse<List<SportSemesterOption>>>(
            "sport/semesters.json", ResultResponse.serializer(ListSerializer(SportSemesterOption.serializer())),
        )
    }

    @Test
    fun `current-semester full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportSemester>, ResultResponse<SportSemester>>(
            "sport/current-semester.json", ResultResponse.serializer(SportSemester.serializer()),
        )
    }

    @Test
    fun `limits full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<java.util.HashMap<Long, java.util.HashMap<Long, api.myitmo.model.sport.SportSignLimit>>>, ResultResponse<Map<Long, Map<Long, SportSignLimit>>>>(
            "sport/limits.json", ResultResponse.serializer(MapSerializer(Long.serializer(), MapSerializer(Long.serializer(), SportSignLimit.serializer()))),
        )
    }

    @Test
    fun `limits-empty full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<java.util.HashMap<Long, java.util.HashMap<Long, api.myitmo.model.sport.SportSignLimit>>>, ResultResponse<Map<Long, Map<Long, SportSignLimit>>>>(
            "sport/limits-empty.json", ResultResponse.serializer(MapSerializer(Long.serializer(), MapSerializer(Long.serializer(), SportSignLimit.serializer()))),
        )
    }

    @Test
    fun `chosen full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.ChosenSportSection>>, ResultResponse<List<ChosenSportSection>>>(
            "sport/chosen.json", ResultResponse.serializer(ListSerializer(ChosenSportSection.serializer())),
        )
    }

    @Test
    fun `lessons full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<Long>>, ResultResponse<List<Long>>>(
            "sport/lessons.json", ResultResponse.serializer(ListSerializer(Long.serializer())),
        )
    }

    @Test
    fun `sign-in-error full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<Long>>, ResultResponse<List<Long>>>(
            "sport/sign-in-error.json", ResultResponse.serializer(ListSerializer(Long.serializer())),
        )
    }

    @Test
    fun `sign-out-error full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<Long>>, ResultResponse<List<Long>>>(
            "sport/sign-out-error.json", ResultResponse.serializer(ListSerializer(Long.serializer())),
        )
    }

    @Test
    fun `reviewed sport dates preserve instants but never hide value or shape drift`() {
        val old = ItmoApiJson.parseToJsonElement("""{"result":{"date_start":"2026-01-05T00:00+03:00"}}""")
        val modern = ItmoApiJson.parseToJsonElement("""{"result":{"date_start":"2026-01-04T21:00:00Z"}}""")
        assertEquals(IntendedDifferences.normalize("sport/current-semester.json", old), IntendedDifferences.normalize("sport/current-semester.json", modern))
        val changed = ItmoApiJson.parseToJsonElement("""{"result":{"date_start":"2026-01-04T22:00:00Z"}}""")
        assertNotEquals(IntendedDifferences.normalize("sport/current-semester.json", old), IntendedDifferences.normalize("sport/current-semester.json", changed))
        assertNotEquals(IntendedDifferences.normalize("sport/unreviewed.json", old), IntendedDifferences.normalize("sport/unreviewed.json", modern))
        val unregisteredPath = ItmoApiJson.parseToJsonElement("""{"result":{"unreviewed_date":"2026-01-05T00:00+03:00"}}""")
        assertEquals(unregisteredPath, IntendedDifferences.normalize("sport/current-semester.json", unregisteredPath))
        val unreviewedSpelling = ItmoApiJson.parseToJsonElement("""{"result":{"date_start":"2026-01-05T00:00:00+03:00"}}""")
        assertNotEquals(IntendedDifferences.normalize("sport/current-semester.json", unreviewedSpelling), IntendedDifferences.normalize("sport/current-semester.json", modern))
        val extra = JsonObject(modern.jsonObject + ("unreviewed" to JsonPrimitive("2026-01-05T00:00+03:00")))
        assertNotEquals(IntendedDifferences.normalize("sport/current-semester.json", old), IntendedDifferences.normalize("sport/current-semester.json", extra))
        val yearBoundary = ItmoApiJson.decodeFromString(ResultResponse.serializer(SportSemester.serializer()), fixture("sport/current-semester.json")).requireResult()
        assertEquals(OffsetDateTime.parse("0001-01-01T00:00+03:00").toInstant().toString(), yearBoundary.choiceStart.toString())
    }

    private fun legacyApi(): api.myitmo.MyItmoApi = Retrofit.Builder().baseUrl("https://example.invalid/custom/base/")
        .addConverterFactory(GsonConverterFactory.create(api.myitmo.MyItmo().gson))
        .build().create(api.myitmo.MyItmoApi::class.java)

    @Test
    fun `production schedule request matches legacy dates building and repeated filters`() = runTest {
        val api = legacyApi()
        for ((types, teachers) in listOf(
            listOf(3L, 1L, 3L) to listOf(200002L, 200001L),
            emptyList<Long>() to emptyList(),
            null to null,
        )) {
            for (building in listOf(null, -1L, 0L, 335L)) {
                val legacy = api.getSportSchedule(JavaDate.of(2026, 1, 5), JavaDate.of(2026, 1, 11), building, types, teachers).request()
                val query = legacy.url.queryParameterNames.associateWith { key -> legacy.url.queryParameterValues(key).map { requireNotNull(it) } }
                sportExchange("sign/schedule", fixture("sport/empty.json"), query, inspect = { modern ->
                    assertEquals(legacy.method, modern.method.value)
                    assertEquals(legacy.url.scheme, modern.url.protocol.name)
                    assertEquals(legacy.url.host, modern.url.host)
                    assertEquals(legacy.url.encodedPath, modern.url.encodedPath)
                    assertEquals(legacy.url.encodedQuery, modern.url.encodedQuery)
                }) {
                    SportApiImpl(it).getSportSchedule(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11), building, types, teachers)
                }
            }
        }
    }

    @Test
    fun `production enrollment and withdrawal bodies match legacy Retrofit`() = runTest {
        val api = legacyApi()
        for (ids in listOf(listOf(500001L, 500003L), emptyList())) {
            for (method in listOf(HttpMethod.Post, HttpMethod.Delete)) {
                val legacy = (if (method == HttpMethod.Post) api.signInLessons(ids) else api.signOutLessons(ids)).request()
                val buffer = okio.Buffer()
                requireNotNull(legacy.body).writeTo(buffer)
                sportExchange("sign/schedule/lessons", fixture("sport/empty.json"), method = method, body = buffer.readUtf8(), inspect = { modern ->
                    assertEquals(legacy.method, modern.method.value)
                    assertEquals(legacy.url.encodedPath, modern.url.encodedPath)
                }) {
                    if (method == HttpMethod.Post) SportApiImpl(it).signInLessons(ids)
                    else SportApiImpl(it).signOutLessons(ids)
                }
            }
        }
    }

    @Test
    fun `remaining sport fixtures preserve their full wire trees`() {
        for (path in listOf("sport/sport-types.json", "sport/sport-types-empty.json")) {
            assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.IdValuePair>>, ResultResponse<List<IdValuePair>>>(
                path, ResultResponse.serializer(ListSerializer(IdValuePair.serializer())),
            )
        }
        for (path in listOf("sport/sign-attempts.json", "sport/sign-attempts-zero.json", "sport/remaining-error.json")) {
            assertParity<api.myitmo.model.ResultResponse<Int>, ResultResponse<Int>>(
                path, ResultResponse.serializer(Int.serializer()),
            )
        }
        for (path in listOf("sport/calendar.json", "sport/calendar-empty.json")) {
            assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportSchedule>>, ResultResponse<List<SportSchedule>>>(
                path, ResultResponse.serializer(ListSerializer(SportSchedule.serializer())),
            )
        }
        for (path in listOf("sport/debt.json", "sport/debt-none.json")) {
            assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportDebt>, ResultResponse<SportDebt>>(
                path, ResultResponse.serializer(SportDebt.serializer()),
            )
        }
        for (path in listOf("sport/externat.json", "sport/externat-declined.json", "sport/externat-none.json")) {
            assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportExternat>, ResultResponse<SportExternat>>(
                path, ResultResponse.serializer(SportExternat.serializer()),
            )
        }
        for (path in listOf("sport/health-level.json")) {
            assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.sport.SportHealthLevelResponse>, ResultResponse<SportHealthLevelResponse>>(
                path, ResultResponse.serializer(SportHealthLevelResponse.serializer()),
            )
        }
        for (path in listOf("sport/selections.json", "sport/selections-empty.json")) {
            assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportSelection>>, ResultResponse<List<SportSelection>>>(
                path, ResultResponse.serializer(ListSerializer(SportSelection.serializer())),
            )
        }
        for (path in listOf("sport/projects.json", "sport/projects-empty.json")) {
            assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.sport.SportProject>>, ResultResponse<List<SportProject>>>(
                path, ResultResponse.serializer(ListSerializer(SportProject.serializer())),
            )
        }
    }

    @Test
    fun `all remaining production requests match legacy Retrofit`() = runTest {
        val legacy = legacyApi()
        val cases: List<Pair<retrofit2.Call<*>, suspend (SportApi) -> Unit>> = listOf(
            legacy.getSportTypes() to { it.getSportTypes() },
            legacy.getSportSignAttempts() to { it.getSportSignAttempts() },
            legacy.getPersonalSportCalendar(JavaDate.of(2026, 1, 5), JavaDate.of(2026, 1, 11)) to {
                it.getPersonalSportCalendar(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11))
            },
            legacy.getSportDebt() to { it.getSportDebt() },
            legacy.getSportExternat() to { it.getSportExternat() },
            legacy.getSportHealthLevel() to { it.getSportHealthLevel() },
            legacy.getSportSelections() to { it.getSportSelections() },
            legacy.getSportProjects() to { it.getSportProjects() },
        )
        val responses = listOf("sport-types", "sign-attempts", "calendar", "debt", "externat", "health-level", "selections", "projects")
        for ((index, case) in cases.withIndex()) {
            val request = case.first.request()
            val query = request.url.queryParameterNames.associateWith { key -> request.url.queryParameterValues(key).map { requireNotNull(it) } }
            sportExchange(request.url.encodedPath.removePrefix("/api/sport/"), fixture("sport/${responses[index]}.json"), query, inspect = { modern ->
                assertEquals(request.method, modern.method.value)
                assertEquals(request.url.scheme, modern.url.protocol.name)
                assertEquals(request.url.host, modern.url.host)
                assertEquals(request.url.encodedPath, modern.url.encodedPath)
                assertEquals(request.url.encodedQuery.orEmpty(), modern.url.encodedQuery)
            }) {
                case.second(SportApiImpl(it))
            }
        }
    }
}
