package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.*
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.*
import java.time.LocalDate as JavaDate

class ElectionParityTest {
    @Test
    fun `availability full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.election.ElectionAvailability>, ResultResponse<ElectionAvailability>>(
            "election/availability.json", ResultResponse.serializer(ElectionAvailability.serializer()),
        )
    }

    @Test
    fun `disciplines full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.election.AvailableDiscipline>>, ResultResponse<List<AvailableDiscipline>>>(
            "election/disciplines.json", ResultResponse.serializer(ListSerializer(AvailableDiscipline.serializer())),
        )
    }

    @Test
    fun `validation full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.election.DisciplineSelectionValidation>, ResultResponse<DisciplineSelectionValidation>>(
            "election/validation.json", ResultResponse.serializer(DisciplineSelectionValidation.serializer()),
        )
    }

    @Test
    fun `limits full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<Map<String, api.myitmo.model.election.FlowLimit>>, ResultResponse<Map<String, FlowLimit>>>(
            "election/limits.json", ResultResponse.serializer(MapSerializer(String.serializer(), FlowLimit.serializer())),
        )
    }

    @Test
    fun `chains full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.election.ElectionFlowChain>>, ResultResponse<List<ElectionFlowChain>>>(
            "election/chains.json", ResultResponse.serializer(ListSerializer(ElectionFlowChain.serializer())),
        )
    }

    @Test
    fun `chosen full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<Long>>, ResultResponse<List<Long>>>(
            "election/chosen.json", ResultResponse.serializer(ListSerializer(Long.serializer())),
        )
    }

    @Test
    fun `change-flows full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/change-flows.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `change-disciplines full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/change-disciplines.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `clear full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/clear.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `order-object full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/order-object.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `order-array full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/order-array.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `order-scalar full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/order-scalar.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `empty full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.election.AvailableDiscipline>>, ResultResponse<List<AvailableDiscipline>>>(
            "election/empty.json", ResultResponse.serializer(ListSerializer(AvailableDiscipline.serializer())),
        )
    }

    @Test
    fun `empty-limits full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<Map<String, api.myitmo.model.election.FlowLimit>>, ResultResponse<Map<String, FlowLimit>>>(
            "election/empty-limits.json", ResultResponse.serializer(MapSerializer(String.serializer(), FlowLimit.serializer())),
        )
    }

    @Test
    fun `change-absent full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.election.ChangeResult>, ResultResponse<ChangeResult>>(
            "election/change-absent.json", ResultResponse.serializer(ChangeResult.serializer()),
        )
    }

    @Test
    fun `error full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<com.google.gson.JsonElement>, ResultResponse<JsonElement?>>(
            "election/error.json", ResultResponse.serializer(JsonElement.serializer().nullable),
        )
    }

    @Test
    fun `reviewed dates preserve instants and never normalize unreviewed drift`() {
        val pairs = listOf(
            Triple("election/availability.json", "semesterStart", "2026-01-05T00:00+03:00" to "2026-01-04T21:00:00Z"),
            Triple("election/availability.json", "semesterEnd", "2026-06-30T00:00+03:00" to "2026-06-29T21:00:00Z"),
            Triple("election/availability.json", "dateStart", "2026-01-06T10:00+03:00" to "2026-01-06T07:00:00Z"),
            Triple("election/availability.json", "dateEnd", "2026-01-07T18:30+03:00" to "2026-01-07T15:30:00Z"),
            Triple("requests/my.json", "created_at", "2026-01-05T09:00+03:00" to "2026-01-05T06:00:00Z"),
            Triple("requests/my.json", "updated_at", "2026-01-06T10:30+03:00" to "2026-01-06T07:30:00Z"),
        )
        for ((fixture, key, pair) in pairs) {
            val (old, new) = pair
            assertEquals(java.time.OffsetDateTime.parse(old).toInstant(), java.time.OffsetDateTime.parse(new).toInstant())
            fun tree(name: String, value: String): JsonElement {
                val payload = JsonObject(mapOf(name to JsonPrimitive(value)))
                return JsonObject(mapOf("result" to if (fixture.startsWith("requests/")) JsonArray(listOf(payload)) else payload))
            }
            val legacy = tree(key, old)
            val modern = tree(key, new)
            assertEquals(IntendedDifferences.normalize(fixture, legacy), IntendedDifferences.normalize(fixture, modern))
            val changed = java.time.Instant.parse(new).plusSeconds(1).toString()
            assertNotEquals(IntendedDifferences.normalize(fixture, legacy), IntendedDifferences.normalize(fixture, tree(key, changed)))
            assertNotEquals(IntendedDifferences.normalize("unreviewed.json", legacy), IntendedDifferences.normalize("unreviewed.json", modern))
            assertNotEquals(IntendedDifferences.normalize(fixture, tree("unreviewed", old)), IntendedDifferences.normalize(fixture, tree("unreviewed", new)))
            val unreviewedSpelling = old.replace("+03:00", ":00+03:00")
            assertNotEquals(IntendedDifferences.normalize(fixture, tree(key, unreviewedSpelling)), IntendedDifferences.normalize(fixture, modern))
            val extra = JsonObject(modern.jsonObject + ("unreviewed" to JsonPrimitive("extra")))
            assertNotEquals(IntendedDifferences.normalize(fixture, legacy), IntendedDifferences.normalize(fixture, extra))
        }
    }

    private fun legacyApi(): api.myitmo.MyItmoApi = Retrofit.Builder().baseUrl("https://example.invalid/custom/base/")
        .addConverterFactory(GsonConverterFactory.create(api.myitmo.MyItmo().gson))
        .build().create(api.myitmo.MyItmoApi::class.java)

    @Test
    fun `production getElectionAvailability request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getElectionAvailability().request()
        myItmoAreaExchange("election/students/availability", fixture("election/availability.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).getElectionAvailability()
        }
    }

    @Test
    fun `production getAvailableDisciplines request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getAvailableDisciplines().request()
        myItmoAreaExchange("election/students/available_disciplines", fixture("election/disciplines.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).getAvailableDisciplines()
        }
    }

    @Test
    fun `production validateSelectedDisciplines request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().validateSelectedDisciplines(listOf("00a /+&?", "000b", "00a /+&?")).request()
        myItmoAreaExchange("election/students/group_flow_available_disciplines", fixture("election/validation.json"), method = HttpMethod.Post, body = "[\"00a /+&?\",\"000b\",\"00a /+&?\"]", inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).validateSelectedDisciplines(listOf("00a /+&?", "000b", "00a /+&?"))
        }
    }

    @Test
    fun `production getFlowLimits request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getFlowLimits().request()
        myItmoAreaExchange("election/students/limits/flows", fixture("election/limits.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).getFlowLimits()
        }
    }

    @Test
    fun `production getOrderedFlowChains request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getOrderedFlowChains().request()
        myItmoAreaExchange("election/students/ordered_flow_chains", fixture("election/chains.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).getOrderedFlowChains()
        }
    }

    @Test
    fun `production getChosenFlows request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getChosenFlows().request()
        myItmoAreaExchange("election/students/chosen_flows", fixture("election/chosen.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).getChosenFlows()
        }
    }

    @Test
    fun `production changeSelectedFlows request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().changeSelectedFlows(listOf(101L, 102L, 101L)).request()
        myItmoAreaExchange("election/students/order/change", fixture("election/change-flows.json"), method = HttpMethod.Post, body = "[101,102,101]", inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).changeSelectedFlows(listOf(101L, 102L, 101L))
        }
    }

    @Test
    fun `production clearAllSelectedFlows request matches pinned legacy Retrofit`() = runTest {
        // Retrofit rejects the legacy wildcard response type before constructing a call.
        // Parse its actual annotations through the same pinned RequestFactory, without a converter or network.
        val retrofit = Retrofit.Builder().baseUrl("https://example.invalid/custom/base/").build()
        val legacyMethod = api.myitmo.MyItmoApi::class.java.getMethod("clearAllSelectedFlows")
        val factoryClass = Class.forName("retrofit2.RequestFactory")
        val parse = factoryClass.getDeclaredMethod("parseAnnotations", Retrofit::class.java, Class::class.java, java.lang.reflect.Method::class.java)
        parse.isAccessible = true
        val factory = parse.invoke(null, retrofit, api.myitmo.MyItmoApi::class.java, legacyMethod)
        val create = factoryClass.getDeclaredMethod("create", Any::class.java, Array<Any>::class.java)
        create.isAccessible = true
        val legacy = create.invoke(factory, null, emptyArray<Any>()) as okhttp3.Request
        myItmoAreaExchange("election/students/order/clear", fixture("election/clear.json"), method = HttpMethod.Post, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).clearAllSelectedFlows()
        }
    }

    @Test
    fun `production changeSelectedDisciplines request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().changeSelectedDisciplines(listOf("00a /+&?", "000b")).request()
        myItmoAreaExchange("election/students/order/", fixture("election/change-disciplines.json"), method = HttpMethod.Post, body = "[\"00a /+&?\",\"000b\"]", inspect = { assertLegacyAreaRequest(legacy, it) }) {
            ElectionApiImpl(it).changeSelectedDisciplines(listOf("00a /+&?", "000b"))
        }
    }

}

/** Full request comparison shared only by the four ML-07 areas; no network execution. */
internal fun assertLegacyAreaRequest(legacy: okhttp3.Request, modern: HttpRequestData) {
    assertEquals(legacy.method, modern.method.value)
    assertEquals(legacy.url.scheme, modern.url.protocol.name)
    assertEquals(legacy.url.host, modern.url.host)
    assertEquals(legacy.url.encodedPath, modern.url.encodedPath)
    assertEquals(legacy.url.encodedQuery.orEmpty(), modern.url.encodedQuery)
    val query = legacy.url.queryParameterNames.associateWith { key -> legacy.url.queryParameterValues(key).map { requireNotNull(it) } }
    assertEquals(query, modern.url.parameters.entries().associate { it.key to it.value })
    val body = legacy.body
    if (body == null || body.contentLength() == 0L) {
        assertTrue(modern.body is io.ktor.http.content.OutgoingContent.NoContent)
    } else {
        val buffer = okio.Buffer()
        body.writeTo(buffer)
        val content = modern.body as TextContent
        assertEquals(ContentType.Application.Json, content.contentType)
        assertEquals("application/json; charset=UTF-8", body.contentType().toString())
        // Gson HTML-escapes ampersands; compare every JSON value, not irrelevant string spelling.
        assertEquals(ItmoApiJson.parseToJsonElement(buffer.readUtf8()), ItmoApiJson.parseToJsonElement(content.text))
    }
}
