package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.bars.model.*
import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BarsClientTest {
    private val initial = "Bearer synthetic-initial"
    private val fresh = "Bearer synthetic-renewed"
    private val configuration = BarsConfiguration(restUrl = Url("https://synthetic.invalid/backend/rest/"))

    private fun TestScope.mockEngine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData): MockEngine =
        MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler(handler)
        })

    private suspend fun storage(header: String? = initial): RuntimeBarsStorage =
        RuntimeBarsStorage().also { it.setAuthorization(header) }

    private fun assertRequest(request: HttpRequestData, path: String, method: HttpMethod = HttpMethod.Get) {
        assertEquals(method, request.method)
        assertEquals("https", request.url.protocol.name)
        assertEquals("synthetic.invalid", request.url.host)
        assertEquals("/backend/rest/$path", request.url.encodedPath)
        assertNull(request.headers[HttpHeaders.Cookie])
    }

    @Test
    fun loginStoresTheAuthorizationHeaderAndRequestsCarryIt() = runTest {
        val storage = storage(null)
        var calls = 0
        val engine = mockEngine { request ->
            if (calls++ == 0) {
                assertRequest(request, "login")
                assertEquals(mapOf("code" to listOf("synthetic&code"), "customRedirectUri" to listOf(configuration.redirectUri)), request.url.parameters.entries().associate { it.key to it.value })
                assertNull(request.headers[HttpHeaders.Authorization])
                respond("", headers = headersOf(HttpHeaders.Authorization, initial))
            } else {
                assertRequest(request, "users/current_user/")
                assertTrue(request.url.parameters.isEmpty())
                assertTrue(request.headers[HttpHeaders.Authorization] == initial)
                respond(fixture("bars/user.json"))
            }
        }
        val client = BarsClient(engine, configuration, storage)
        try {
            client.login("synthetic&code")
            assertTrue(storage.getAuthorization() == initial)
            val user = client.getCurrentUser()
            assertEquals("123456", user.login)
            assertEquals(Term.AUTUMN, user.selectedTermValue)
            assertEquals(2, calls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun unauthorizedWithoutCodeSupplierIsAnException() = runTest {
        val engine = mockEngine { respond("not JSON", HttpStatusCode.Unauthorized) }
        val client = BarsClient(engine, configuration, storage())
        val empty = BarsClient(engine, configuration)
        try {
            assertEquals(401, assertFailsWith<MyItmoException.Auth> { client.getCurrentUser() }.status)
            assertEquals(401, assertFailsWith<MyItmoException.Auth> { empty.getCurrentUser() }.status)
            assertEquals(1, engine.requestHistory.size)
        } finally { client.close(); empty.close(); engine.close() }
    }

    @Test
    fun expiredSessionIsRenewedOnceThroughTheCodeSupplierAndRetried() = runTest {
        val storage = storage()
        val states = mutableListOf<String>()
        var calls = 0
        val engine = mockEngine { request ->
            when (calls++ % 3) {
                0 -> { assertTrue(request.headers[HttpHeaders.Authorization] == storage.getAuthorization()); respond("", HttpStatusCode.Unauthorized) }
                1 -> {
                    assertRequest(request, "login")
                    assertEquals("fresh-code", request.url.parameters["code"])
                    assertNull(request.headers[HttpHeaders.Authorization])
                    respond("", headers = headersOf(HttpHeaders.Authorization, fresh))
                }
                else -> {
                    assertTrue(request.headers[HttpHeaders.Authorization] == fresh)
                    if (calls <= 3) respond(fixture("bars/user.json")) else respond("", HttpStatusCode.Unauthorized)
                }
            }
        }
        val client = BarsClient(engine, configuration, storage, BarsCodeSupplier { states += it; "fresh-code" })
        try {
            assertEquals("123456", client.getCurrentUser().login)
            assertEquals(1, states.size)
            assertTrue(states.single().isNotBlank())
            assertTrue(storage.getAuthorization() == fresh)
            assertEquals(401, assertFailsWith<MyItmoException.Auth> { client.getCurrentUser() }.status)
            assertEquals(2, states.size)
            assertEquals(6, calls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun supplierWithoutCodeLeavesTheSessionUnauthorized() = runTest {
        var supplied = 0
        val engine = mockEngine { error("Must remain off-network") }
        val client = BarsClient(engine, configuration, storage(null), BarsCodeSupplier { supplied++; null })
        try {
            assertEquals(401, assertFailsWith<MyItmoException.Auth> { client.getCurrentUser() }.status)
            assertEquals(1, supplied)
            assertTrue(engine.requestHistory.isEmpty())
        } finally { client.close(); engine.close() }
    }

    @Test
    fun selectPeriodWritesOnlyDifferencesAndVerifiesThem() = runTest {
        val bodies = mutableListOf<String>()
        val responses = ArrayDeque(listOf(
            fixture("bars/user.json"), fixture("bars/user.json"), fixture("bars/setting.json"), fixture("bars/setting.json"),
            fixture("bars/user.json").replace("2026/2027", "2025/2026").replace("\"selected_term\": 1", "\"selected_term\": 0"),
            fixture("bars/user.json"), fixture("bars/setting.json"), fixture("bars/user.json"),
        ))
        val engine = mockEngine { request ->
            if (request.method == HttpMethod.Post) {
                assertRequest(request, "config/personal", HttpMethod.Post)
                assertEquals(ContentType.Application.Json, request.body.contentType)
                bodies += (request.body as TextContent).text
            } else assertRequest(request, "users/current_user/")
            assertTrue(request.url.parameters.isEmpty())
            respond(responses.removeFirst())
        }
        val client = BarsClient(engine, configuration, storage())
        try {
            assertEquals("2026/2027", client.selectPeriod("2026/2027", Term.AUTUMN).selectedYear)
            assertEquals(1, engine.requestHistory.size)
            assertEquals("2025/2026", client.selectPeriod("2025/2026", Term.SPRING).selectedYear)
            assertEquals(listOf("""{"name":"current_year","value":"2025/2026"}""", """{"name":"current_term","value":"0"}"""), bodies)
            assertFailsWith<MyItmoException.Decode> { client.selectPeriod("2026/2027", Term.SPRING) }
            assertFailsWith<IllegalArgumentException> { client.selectPeriod("invalid", Term.SPRING) }
            assertTrue(responses.isEmpty())
        } finally { client.close(); engine.close() }
    }

    @Test
    fun journalPathEncodesIdentifierAndParsesScores() = runTest {
        val engine = mockEngine { request ->
            assertRequest(request, "marks/8/flow/a%2Fb/student")
            assertTrue(request.url.parameters.isEmpty())
            respond(fixture("bars/journal.json"))
        }
        val client = BarsClient(engine, configuration, storage())
        try {
            val journal = client.getStudentJournal(8, "flow", "a/b")
            val marks = journal.students.single().marks
            assertNull(marks.additional!!.checkpointId)
            assertTrue(marks.hasAnyMark())
            assertNull(journal.headers.plan.finalCheckpoint!!.name)
            assertEquals("3/E", marks.activeApprovals.single().gradeCode)
            assertEquals(1770000000123L, marks.regular.single().createdAt)
            val empty = ItmoApiJson.decodeFromString(StudentJournal.serializer(), fixture("bars/empty-journal.json"))
            assertFalse(empty.students.single().marks.hasAnyMark())
            assertNull(empty.students.single().marks.finalMark)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun gradeCodesFollowTheMyItmoFormat() {
        val cases = mapOf("Отл., A" to "5/A", "Хор., C" to "4/C", "Удвл., E" to "3/E", "Неуд., FX" to "2/FX", "Зачет" to "Зачет", "Незачет" to "Незачет", "Удвл." to "Удвл.", " Отл., B " to "5/B")
        cases.forEach { (input, output) -> assertEquals(output, Approval(markString = input).gradeCode) }
        assertNull(Approval().gradeCode)
    }

    @Test
    fun rotatedHeaderReplacesTheStoredSession() = runTest {
        val storage = storage()
        val engine = mockEngine { respond(fixture("bars/user.json"), headers = headersOf(HttpHeaders.Authorization, fresh)) }
        val client = BarsClient(engine, configuration, storage)
        try { client.getCurrentUser(); assertTrue(storage.getAuthorization() == fresh) }
        finally { client.close(); engine.close() }
    }

    @Test
    fun withPeriodRequestRenewalDoesNotDeadlock() = runTest {
        var calls = 0
        var supplied = 0
        val engine = mockEngine { request ->
            when (calls++) {
                0 -> respond(fixture("bars/user.json"))
                1 -> respond("", HttpStatusCode.Unauthorized)
                2 -> { assertRequest(request, "login"); respond("", headers = headersOf(HttpHeaders.Authorization, fresh)) }
                else -> respond(fixture("bars/disciplines.json"))
            }
        }
        val client = BarsClient(engine, configuration, storage(), BarsCodeSupplier { supplied++; "fresh-code" })
        try {
            withTimeout(5000) {
                assertEquals(90, client.withPeriod("2026/2027", Term.AUTUMN) { getDisciplines(true) }.single().id)
            }
            assertEquals(1, supplied)
            assertEquals(4, calls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun noSessionRenewsOnlyOnceEvenWhenTheFirstAuthorizedRequestFails() = runTest {
        var supplied = 0
        val engine = mockEngine { request ->
            if (request.url.encodedPath.endsWith("login")) respond("", headers = headersOf(HttpHeaders.Authorization, fresh))
            else respond("", HttpStatusCode.Unauthorized)
        }
        val client = BarsClient(engine, configuration, storage(null), BarsCodeSupplier { supplied++; "fresh-code" })
        try {
            assertFailsWith<MyItmoException.Auth> { client.getCurrentUser() }
            assertEquals(1, supplied)
            assertEquals(2, engine.requestHistory.size)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun concurrentUnauthorizedRequestsShareOneRenewal() = runTest {
        val bothRejected = CompletableDeferred<Unit>()
        var rejected = 0
        var exchanged = 0
        var retried = 0
        var supplied = 0
        val engine = mockEngine { request ->
            when {
                request.url.encodedPath.endsWith("login") -> {
                    exchanged++
                    respond("", headers = headersOf(HttpHeaders.Authorization, fresh))
                }
                request.headers[HttpHeaders.Authorization] == initial -> {
                    if (++rejected == 2) bothRejected.complete(Unit)
                    bothRejected.await()
                    respond("", HttpStatusCode.Unauthorized)
                }
                else -> {
                    retried++
                    respond(fixture("bars/user.json"))
                }
            }
        }
        val client = BarsClient(engine, configuration, storage(), BarsCodeSupplier { supplied++; "fresh-code" })
        try {
            withTimeout(5000) { awaitAll(async { client.getCurrentUser() }, async { client.getCurrentUser() }) }
            assertEquals(1, supplied)
            assertEquals(2, rejected)
            assertEquals(1, exchanged)
            assertEquals(2, retried)
            assertEquals(5, engine.requestHistory.size)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun periodMutationsAreSerializedButReadRequestsCanRunInsideAction() = runTest {
        val inAction = CompletableDeferred<Unit>()
        val releaseAction = CompletableDeferred<Unit>()
        val secondDone = CompletableDeferred<Unit>()
        val engine = mockEngine { respond(fixture("bars/user.json")) }
        val client = BarsClient(engine, configuration, storage())
        try {
            val first = async {
                client.withPeriod("2026/2027", Term.AUTUMN) {
                    inAction.complete(Unit)
                    getCurrentUser()
                    releaseAction.await()
                }
            }
            inAction.await()
            val second = async { client.selectPeriod("2026/2027", Term.AUTUMN); secondDone.complete(Unit) }
            runCurrent()
            assertFalse(secondDone.isCompleted)
            releaseAction.complete(Unit)
            withTimeout(5000) { first.await(); second.await() }
            assertEquals(3, engine.requestHistory.size)
            assertTrue(secondDone.isCompleted)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun remainingCatalogEndpointsHaveExactQueriesAndBodies() = runTest {
        var calls = 0
        val engine = mockEngine { request ->
            val body = when (calls++) {
                0 -> { assertRequest(request, "config/"); assertTrue(request.url.parameters.isEmpty()); fixture("bars/config.json") }
                1 -> {
                    assertRequest(request, "config/personal", HttpMethod.Post)
                    assertEquals("""{"name":"current_year","value":"2026/2027"}""", (request.body as TextContent).text)
                    fixture("bars/setting.json")
                }
                2 -> { assertRequest(request, "journal/disciplines"); assertEquals(mapOf("withCheckpointPlansOnly" to listOf("false")), request.url.parameters.entries().associate { it.key to it.value }); fixture("bars/disciplines.json") }
                3 -> { assertRequest(request, "journal/disciplines"); assertTrue(request.url.parameters.isEmpty()); fixture("bars/empty-disciplines.json") }
                4 -> { assertRequest(request, "journal/groups-and-flows"); assertEquals(mapOf("disciplineId" to listOf("90")), request.url.parameters.entries().associate { it.key to it.value }); fixture("bars/groups.json") }
                else -> { assertRequest(request, "journal/groups-and-flows"); assertTrue(request.url.parameters.isEmpty()); fixture("bars/empty-groups.json") }
            }
            assertTrue(request.headers[HttpHeaders.Authorization] == initial)
            respond(body)
        }
        val client = BarsClient(engine, configuration, storage())
        try {
            assertNull(client.getConfig()[1].value)
            assertEquals(9, client.setPersonalSetting(Setting(9, "current_year", "2026/2027")).id)
            assertEquals(listOf(8L), client.getDisciplines(false).single().checkpointPlanIds)
            assertTrue(client.getDisciplines().isEmpty())
            assertEquals("a/b", client.getGroupsAndFlows(90).single().identifier)
            assertTrue(client.getGroupsAndFlows().isEmpty())
            assertEquals(6, calls)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun redactionAndFailedRenewalNeverEraseStoredSession() = runTest {
        val sentinel = "synthetic-sensitive-sentinel"
        val storage = storage()
        val engine = mockEngine { request ->
            if (request.url.encodedPath.endsWith("login")) respond(sentinel, HttpStatusCode.BadGateway)
            else respond(sentinel, HttpStatusCode.Unauthorized)
        }
        val client = BarsClient(engine, configuration, storage, BarsCodeSupplier { sentinel })
        try {
            val error = assertFailsWith<MyItmoException.Http> { client.getCurrentUser() }
            assertEquals(502, error.status)
            for (diagnostic in listOf(error.toString(), storage.toString(), client.toString())) {
                assertFalse(sentinel in diagnostic)
                assertFalse(initial in diagnostic)
            }
            assertTrue(storage.getAuthorization() == initial)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun invalidLoginHeadersAndDecodeFailuresHaveRedactedDiagnostics() = runTest {
        val sentinel = "synthetic-sensitive-sentinel"
        for (header in listOf<String?>(null, sentinel, "Bearer short", "Bearer $sentinel\n")) {
            val storage = storage()
            val engine = mockEngine { respond(sentinel, headers = header?.let { headersOf(HttpHeaders.Authorization, it) } ?: Headers.Empty) }
            val client = BarsClient(engine, configuration, storage)
            try {
                val error = assertFailsWith<MyItmoException.Decode> { client.login(sentinel) }
                assertFalse(sentinel in error.toString())
                assertNull(error.cause)
                assertTrue(storage.getAuthorization() == initial)
            } finally { client.close(); engine.close() }
        }
        val engine = mockEngine { respond("""{"id":"$sentinel"}""") }
        val client = BarsClient(engine, configuration, storage())
        try {
            val error = assertFailsWith<MyItmoException.Decode> { client.getCurrentUser() }
            assertFalse(sentinel in error.toString())
            assertNull(error.cause)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun authorizationValidationKeepsLegacyPersistenceSemantics() {
        assertFalse(BarsClient.isValidAuthorization(null))
        assertFalse(BarsClient.isValidAuthorization("Bearer short"))
        assertFalse(BarsClient.isValidAuthorization("bearer synthetic-token"))
        assertTrue(BarsClient.isValidAuthorization("Bearer " + " ".repeat(9)))
        assertTrue(BarsClient.isValidAuthorization("Bearer " + "x".repeat(16377)))
        assertFalse(BarsClient.isValidAuthorization("Bearer " + "x".repeat(16378)))
        assertFalse(BarsClient.isValidAuthorization("Bearer synthetic\rheader"))
        assertFalse(BarsClient.isValidAuthorization("Bearer synthetic\nheader"))
        assertEquals(Term.SPRING, Term.fromWire(0))
        assertEquals(Term.AUTUMN, Term.fromWire(1))
        assertFailsWith<IllegalArgumentException> { Term.fromWire(2) }
        assertEquals("https://bars.itmo.ru/backend/rest/", BarsConfiguration().restUrl.toString())
        assertEquals("bars", BarsConfiguration().clientId)
        assertEquals("https://bars.itmo.ru/rest/login", BarsConfiguration().redirectUri)
    }
}
