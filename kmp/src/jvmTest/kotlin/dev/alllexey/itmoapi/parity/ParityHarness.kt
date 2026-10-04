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
