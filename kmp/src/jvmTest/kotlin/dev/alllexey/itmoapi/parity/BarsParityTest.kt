package dev.alllexey.itmoapi.parity

import api.bars.BarsApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.alllexey.itmoapi.bars.*
import dev.alllexey.itmoapi.bars.model.*
import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** BARS uses bare Gson, not MyITMO's configured Gson. All fixture fields are compared, with no exclusions. */
class BarsParityTest {
    private val baseUrl = "https://synthetic.invalid/backend/rest/"
    private fun legacy(): BarsApi = Retrofit.Builder().baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(Gson())).build().create(BarsApi::class.java)

    private inline fun <reified Legacy, Modern> barsParity(path: String, serializer: KSerializer<Modern>) {
        ParityRegistrations.record(path)
        val gson = Gson()
        val retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
        val type = object : TypeToken<Legacy>() {}.type
        val old = retrofit.responseBodyConverter<Legacy>(type, emptyArray()).convert(fixture(path).toResponseBody())
        val oldTree = ItmoApiJson.parseToJsonElement(gson.toJson(old, type))
        val modern = ItmoApiJson.decodeFromString(serializer, fixture(path))
        assertEquals(oldTree, ItmoApiJson.encodeToJsonElement(serializer, modern), path)
    }

    @Test
    fun userWireTree() = barsParity<api.bars.model.User, User>("bars/user.json", User.serializer())

    @Test
    fun configWireTree() = barsParity<List<api.bars.model.Setting>, List<Setting>>("bars/config.json", ListSerializer(Setting.serializer()))

    @Test
    fun settingWireTree() = barsParity<api.bars.model.Setting, Setting>("bars/setting.json", Setting.serializer())

    @Test
    fun disciplinesWireTree() = barsParity<List<api.bars.model.Discipline>, List<Discipline>>("bars/disciplines.json", ListSerializer(Discipline.serializer()))

    @Test
    fun groupsWireTree() = barsParity<List<api.bars.model.GroupOrFlow>, List<GroupOrFlow>>("bars/groups.json", ListSerializer(GroupOrFlow.serializer()))

    @Test
    fun journalWireTree() = barsParity<api.bars.model.StudentJournal, StudentJournal>("bars/journal.json", StudentJournal.serializer())

    @Test
    fun emptyJournalWireTree() = barsParity<api.bars.model.StudentJournal, StudentJournal>("bars/empty-journal.json", StudentJournal.serializer())

    @Test
    fun emptyDisciplinesWireTree() = barsParity<List<api.bars.model.Discipline>, List<Discipline>>("bars/empty-disciplines.json", ListSerializer(Discipline.serializer()))

    @Test
    fun emptyGroupsWireTree() = barsParity<List<api.bars.model.GroupOrFlow>, List<GroupOrFlow>>("bars/empty-groups.json", ListSerializer(GroupOrFlow.serializer()))

    private suspend fun requestParity(old: Request, body: String, login: Boolean = false, action: suspend BarsClient.() -> Unit) {
        val header = "Bearer synthetic-parity-header"
        val engine = MockEngine { request ->
            assertEquals(old.method, request.method.value)
            assertEquals(old.url.scheme, request.url.protocol.name)
            assertEquals(old.url.host, request.url.host)
            assertEquals(old.url.encodedPath, request.url.encodedPath)
            val queries = old.url.queryParameterNames.associateWith { old.url.queryParameterValues(it) }
            assertEquals(queries, request.url.parameters.entries().associate { it.key to it.value })
            assertTrue(if (login) request.headers[HttpHeaders.Authorization] == null else request.headers[HttpHeaders.Authorization] == header)
            old.body?.let {
                val buffer = Buffer()
                it.writeTo(buffer)
                assertEquals(ItmoApiJson.parseToJsonElement(buffer.readUtf8()), ItmoApiJson.parseToJsonElement((request.body as TextContent).text))
            }
            respond(body, headers = headersOf(HttpHeaders.Authorization, header))
        }
        val storage = RuntimeBarsStorage().also { if (!login) it.setAuthorization(header) }
        val client = BarsClient(engine, BarsConfiguration(restUrl = Url(baseUrl)), storage)
        try { client.action(); assertEquals(1, engine.requestHistory.size) }
        finally { client.close(); engine.close() }
    }

    @Test
    fun endpointRequestsMatchActualLegacyRetrofitConstruction() = runTest {
        val legacyApi = legacy()
        requestParity(legacyApi.login("synthetic&code", "https://bars.itmo.ru/rest/login").request(), "", login = true) { login("synthetic&code") }
        requestParity(legacyApi.currentUser.request(), fixture("bars/user.json")) { getCurrentUser() }
        requestParity(legacyApi.config.request(), fixture("bars/config.json")) { getConfig() }
        requestParity(legacyApi.setPersonalSetting(api.bars.model.Setting("current_year", "2026/2027")).request(), fixture("bars/setting.json")) {
            setPersonalSetting(Setting(name = "current_year", value = "2026/2027"))
        }
        for (filter in listOf(null, false, true)) {
            requestParity(legacyApi.getDisciplines(filter).request(), fixture("bars/disciplines.json")) { getDisciplines(filter) }
        }
        for (id in listOf(null, 90L)) {
            requestParity(legacyApi.getGroupsAndFlows(id).request(), fixture("bars/groups.json")) { getGroupsAndFlows(id) }
        }
        requestParity(legacyApi.getStudentJournal(8, "flow", "a/b &é").request(), fixture("bars/journal.json")) { getStudentJournal(8, "flow", "a/b &é") }
    }

    @Test
    fun authorizationValidationMatchesPersistedLegacyHeaders() {
        for (value in listOf(null, "", "Bearer short", "Bearer " + " ".repeat(9), "Bearer " + "x".repeat(16377), "Bearer " + "x".repeat(16378), "Bearer synthetic\nheader", "Bearer synthetic\rheader", "bearer synthetic-header")) {
            assertEquals(api.bars.Bars.isValidAuthorization(value), BarsClient.isValidAuthorization(value))
        }
    }
}
