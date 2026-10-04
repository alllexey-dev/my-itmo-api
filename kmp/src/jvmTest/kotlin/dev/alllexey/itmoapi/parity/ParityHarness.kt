package dev.alllexey.itmoapi.parity

import api.myitmo.MyItmo
import com.google.gson.reflect.TypeToken
import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import java.time.OffsetDateTime
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.assertEquals

/** Central 1.8.2 converter, including Retrofit's strict document-consumption check. */
internal inline fun <reified Legacy, Modern> assertParity(path: String, serializer: KSerializer<Modern>) {
    ParityRegistrations.record(path)
    val gson = MyItmo().gson
    val retrofit = Retrofit.Builder().baseUrl("https://synthetic.invalid/")
        .addConverterFactory(GsonConverterFactory.create(gson)).build()
    val type = object : TypeToken<Legacy>() {}.type
    val converter = retrofit.responseBodyConverter<Legacy>(type, emptyArray())
    val legacy = converter.convert(fixture(path).toResponseBody())
    val oldTree = ItmoApiJson.parseToJsonElement(gson.toJson(legacy, type))
    val modern = ItmoApiJson.decodeFromString(serializer, fixture(path))
    val newTree = ItmoApiJson.encodeToJsonElement(serializer, modern)
    assertEquals(IntendedDifferences.normalize(path, oldTree), IntendedDifferences.normalize(path, newTree), path)
}

/** Reviewed SP-02 row 13 / ADR 0025 Q5: offset spelling changes, never the instant.
 * Exact fixture/field paths prevent an unrelated string or newly added field being excused.
 */
internal object IntendedDifferences {
    private val dates = mapOf(
        "recordbook/record-book.json" to setOf("$.result[0].exam_date"),
        "recordbook/controls.json" to setOf("$.result[0].date"),
    )

    // Reviewed 2026-10-04: SP-02 date-by-instant / ADR 0025 Q5, same Instant for
    // these exact sport fixture paths and old/new spellings; no fallback or shape changes.
    private val sportDates = mapOf(
        "sport/schedule.json" to mapOf(
            "$.result[0].lessons[0].date" to ("2026-10-05T10:00+03:00" to "2026-10-05T07:00:00Z"),
            "$.result[0].lessons[0].date_end" to ("2026-10-05T11:30+03:00" to "2026-10-05T08:30:00Z"),
            "$.result[0].lessons[1].date" to ("2026-10-05T18:00+03:00" to "2026-10-05T15:00:00Z"),
            "$.result[0].lessons[1].date_end" to ("2026-10-05T19:30+03:00" to "2026-10-05T16:30:00Z"),
        ),
        "sport/chosen.json" to mapOf(
            "$.result[0].lesson_groups[0].lessons[0].date_start" to ("2026-01-05T10:00+03:00" to "2026-01-05T07:00:00Z"),
            "$.result[0].lesson_groups[0].lessons[0].date_end" to ("2026-01-05T11:30+03:00" to "2026-01-05T08:30:00Z"),
        ),
        "sport/external-venue.json" to mapOf(
            "$.result[0].lessons[0].date" to ("2026-10-05T10:00+03:00" to "2026-10-05T07:00:00Z"),
            "$.result[0].lessons[0].date_end" to ("2026-10-05T11:30+03:00" to "2026-10-05T08:30:00Z"),
        ),
        "sport/current-semester.json" to mapOf(
            "$.result.date_start" to ("2026-01-05T00:00+03:00" to "2026-01-04T21:00:00Z"),
            "$.result.date_end" to ("2026-06-01T00:00+03:00" to "2026-05-31T21:00:00Z"),
            "$.result.hard_date_end" to ("2026-07-01T00:00+03:00" to "2026-06-30T21:00:00Z"),
            "$.result.choice_start" to ("0001-01-01T00:00+03:00" to "0000-12-31T21:00:00Z"),
            "$.result.bachelor_bound" to ("0001-01-01T00:00+03:00" to "0000-12-31T21:00:00Z"),
            "$.result.ppa1_start" to ("2026-06-02T00:00+03:00" to "2026-06-01T21:00:00Z"),
            "$.result.ppa1_end" to ("2026-06-10T00:00+03:00" to "2026-06-09T21:00:00Z"),
            "$.result.ppa2_start" to ("2026-06-11T00:00+03:00" to "2026-06-10T21:00:00Z"),
            "$.result.ppa2_end" to ("2026-06-20T00:00+03:00" to "2026-06-19T21:00:00Z"),
        ),
        "sport/score.json" to mapOf(
            "$.result.attendances[0].date" to ("2026-01-05T10:00+03:00" to "2026-01-05T07:00:00Z"),
            "$.result.attendances[1].date" to ("2026-01-05T10:00+03:00" to "2026-01-05T07:00:00Z"),
        ),
    )

    // Coordinator review 2026-10-04: SP-02 date-by-instant / ADR 0025 Q5.
    // Only these six exact fixture/path/old/new spellings; no shape or fallback changes.
    private val electionRequestDates = mapOf(
        "election/availability.json" to mapOf(
            "$.result.semesterStart" to ("2026-01-05T00:00+03:00" to "2026-01-04T21:00:00Z"),
            "$.result.semesterEnd" to ("2026-06-30T00:00+03:00" to "2026-06-29T21:00:00Z"),
            "$.result.dateStart" to ("2026-01-06T10:00+03:00" to "2026-01-06T07:00:00Z"),
            "$.result.dateEnd" to ("2026-01-07T18:30+03:00" to "2026-01-07T15:30:00Z"),
        ),
        "requests/my.json" to mapOf(
            "$.result[0].created_at" to ("2026-01-05T09:00+03:00" to "2026-01-05T06:00:00Z"),
            "$.result[0].updated_at" to ("2026-01-06T10:30+03:00" to "2026-01-06T07:30:00Z"),
        ),
    )

    // Owner review 2026-10-04: "Да, разрешить эти конкретные fallback-расхождения".
    // ADR 0025 Q6 client fallbacks; only these absent wire paths get these exact values.
    private val fallbacks = mapOf(
        "studyplan/study-plan.json" to mapOf(
            "$.result.structure[0].rules" to JsonArray(emptyList()),
            "$.result.structure[0].children[0].rules" to JsonArray(emptyList()),
            "$.result.structure[0].children[0].children[1].rules" to JsonArray(emptyList()),
            "$.result.structure[0].children[0].children[1].children" to JsonArray(emptyList()),
            "$.result.structure[0].contents" to JsonObject(emptyMap()),
            "$.result.structure[0].children[0].contents" to JsonObject(emptyMap()),
            "$.result.structure[0].children[0].children[1].contents" to JsonObject(emptyMap()),
        ),
        "studyplan/absence.json" to mapOf(
            "$.result.semesters" to JsonArray(emptyList()),
            "$.result.structure[0].rules" to JsonArray(emptyList()),
            "$.result.structure[0].children" to JsonArray(emptyList()),
            "$.result.structure[0].contents" to JsonObject(emptyMap()),
            "$.result.planInfo" to ItmoApiJson.parseToJsonElement(
                """{"directionCode":"","directionName":"","levelQualification":"","planType":"","programName":"","startYear":0}""",
            ),
        ),
    )

    fun normalize(fixture: String, tree: JsonElement): JsonElement {
        fun visit(value: JsonElement, path: String): JsonElement = when {
            (sportDates[fixture]?.get(path) ?: electionRequestDates[fixture]?.get(path))?.let { (old, new) ->
                value is JsonPrimitive && value.isString && (value.content == old || value.content == new)
            } == true -> JsonPrimitive(OffsetDateTime.parse(value.jsonPrimitive.content).toInstant().toString())
            path in dates[fixture].orEmpty() -> JsonPrimitive(OffsetDateTime.parse(value.jsonPrimitive.content).toInstant().toString())
            value is JsonObject -> {
                val members = value.mapValues { (key, child) -> visit(child, "$path.$key") }.toMutableMap()
                fallbacks[fixture].orEmpty().forEach { (wirePath, fallback) ->
                    val parent = wirePath.substringBeforeLast('.')
                    val key = wirePath.substringAfterLast('.')
                    if (parent == path && key !in members) members[key] = fallback
                }
                JsonObject(members)
            }
            value is JsonArray -> JsonArray(value.mapIndexed { index, child -> visit(child, "$path[$index]") })
            else -> value
        }
        return visit(tree, "$")
    }
}
