package dev.alllexey.itmoapi

// README imports begin
import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.bars.auth.BarsLogin
import dev.alllexey.itmoapi.bars.auth.BarsSessionCode
import dev.alllexey.itmoapi.bars.model.Discipline
import dev.alllexey.itmoapi.bars.model.StudentJournal
import dev.alllexey.itmoapi.bars.model.Term
import dev.alllexey.itmoapi.core.defaultEngine
import dev.alllexey.itmoapi.itmoid.CallbackUrl
import dev.alllexey.itmoapi.itmoid.FileTokenStorage
import dev.alllexey.itmoapi.itmoid.TokenSet
import dev.alllexey.itmoapi.itmoid.TokenStorage
import dev.alllexey.itmoapi.myitmo.MyItmoClient
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import dev.alllexey.itmoapi.myitmo.schedule.Schedule
import dev.alllexey.itmoapi.myitmo.sport.SportLesson
import dev.alllexey.itmoapi.myitmo.sport.SportSignResult
import io.ktor.client.engine.HttpClientEngine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.io.path.Path
import kotlin.time.Clock
// README imports end
import java.io.File
import dev.alllexey.itmoapi.bars.BarsConfiguration
import dev.alllexey.itmoapi.bars.RuntimeBarsStorage
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import dev.alllexey.itmoapi.itmoid.Pkce
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReadmeSamplesTest {
    // README file begin
    suspend fun openSession(username: String, password: String): MyItmo =
        MyItmo.loginOrResume(username, password, FileTokenStorage(Path("itmo-tokens.json")))
    // README file end

    // README relogin begin
    suspend fun signInAgain(old: MyItmo, username: String, password: String, storage: TokenStorage): MyItmo {
        old.close()
        return MyItmo.login(username, password, storage)
    }
    // README relogin end

    // README sportfind begin
    suspend fun freeLessons(itmo: MyItmo, from: LocalDate, to: LocalDate): List<SportLesson> =
        itmo.sport.getSportSchedule(from, to)
            .flatMap { it.lessons.orEmpty() }
            .filter { it.canSignIn.canSignIn && it.available > 0 && !it.signed }
    // README sportfind end

    // README sportcancel begin
    suspend fun cancel(itmo: MyItmo, lessonId: Long): Boolean =
        lessonId in itmo.sport.signOutLessons(listOf(lessonId))
    // README sportcancel end

    // README barsjournal begin
    suspend fun readJournals(bars: BarsClient): List<StudentJournal> {
        val groups = bars.getGroupsAndFlows()
        return bars.getDisciplines(withCheckpointPlansOnly = true)
            .flatMap { it.checkpointPlanIds }.distinct()
            .mapNotNull { plan ->
                val group = groups.firstOrNull { plan in it.checkpointPlanIds } ?: return@mapNotNull null
                bars.getStudentJournal(plan, group.type, group.identifier)
            }
    }
    // README barsjournal end

    // README session begin
    suspend fun signOut(itmo: MyItmo) {
        if (itmo.hasSession()) itmo.logout()
    }
    // README session end

    // README errors begin
    suspend fun weekOrNull(itmo: MyItmo, monday: LocalDate): List<Schedule>? = try {
        itmo.schedule.getPersonalSchedule(monday, monday.plus(6, DateTimeUnit.DAY))
    } catch (error: MyItmoException.Auth) {
        null // сессия закончилась: нужен новый вход
    } catch (error: MyItmoException.Api) {
        null // My ITMO отказал: error.errorCode
    } catch (error: MyItmoException) {
        null // Network, Http, Decode, Empty: можно повторить позже
    }
    // README errors end

    // README sport begin
    suspend fun book(itmo: MyItmo, lessonId: Long): String = when (val result = itmo.sport.signInLesson(lessonId)) {
        SportSignResult.Booked -> "записан"
        SportSignResult.Full -> "мест нет, можно попробовать позже"
        is SportSignResult.Rejected -> "отказ: код ${result.refusal.errorCode}"
        is SportSignResult.RetryLater -> "решения нет, повторить позже"
    }
    // README sport end

    // README storage begin
    class MemoryTokens : TokenStorage {
        private var snapshot: TokenSet? = null
        override suspend fun read(): TokenSet? = snapshot
        override suspend fun write(tokens: TokenSet?) { snapshot = tokens }
        override fun toString(): String = "MemoryTokens(redacted)"
    }
    // README storage end

    // README client begin
    fun createClient(storage: TokenStorage): MyItmoClient =
        MyItmoClient(MyItmoConfiguration.DEFAULT, storage, defaultEngine(), Clock.System)
    // README client end

    private fun createClient(
        storage: TokenStorage,
        engine: HttpClientEngine,
        clock: Clock,
        configuration: MyItmoConfiguration,
    ): MyItmoClient = MyItmoClient(configuration, storage, engine, clock)

    // README login begin
    suspend fun completeLogin(
        client: MyItmoClient,
        callbackUrl: String,
        expectedState: String,
        verifier: String,
    ): Boolean {
        val configuration = client.configuration.itmoId
        val callback = CallbackUrl(configuration.redirectUri, configuration.issuer)
        val code = callback.extractCode(callbackUrl, expectedState) ?: return false
        client.tokens.replaceTokens(client.identity.exchange(code, verifier))
        return true
    }
    // README login end

    // README schedule begin
    suspend fun readSchedule(client: MyItmoClient): List<Schedule> =
        client.schedule.getPersonalSchedule(
            LocalDate(2026, 1, 5), LocalDate(2026, 1, 11),
        )
    // README schedule end

    // README bars begin
    suspend fun readBarsDisciplines(bars: BarsClient): List<Discipline> =
        bars.withPeriod("2026/2027", Term.AUTUMN) {
            getDisciplines(withCheckpointPlansOnly = true)
        }
    // README bars end

    // README cookies begin
    suspend fun replayBarsLogin(
        login: BarsLogin,
        bars: BarsClient,
        state: String,
        cookieHeader: String?,
        saveCookies: suspend (List<String>) -> Unit,
    ): BarsSessionCode.Outcome {
        val result = login.requestCodeWithCookies(state, cookieHeader)
        saveCookies(result.setCookies)
        when (result.outcome) {
            BarsSessionCode.Outcome.CODE -> bars.login(requireNotNull(result.code))
            BarsSessionCode.Outcome.LOGIN_REQUIRED -> Unit
            BarsSessionCode.Outcome.REJECTED -> Unit
            BarsSessionCode.Outcome.HTTP_ERROR -> Unit
        }
        return result.outcome
    }
    // README cookies end

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
    private val identity = ItmoIdConfiguration(
        issuer = "https://issuer.invalid/realm",
        redirectUri = "https://callback.invalid/myitmo",
    )
    private val configuration = MyItmoConfiguration(Url("https://issuer.invalid"), identity)
    private val barsConfiguration = BarsConfiguration(
        restUrl = Url("https://issuer.invalid/backend/rest/"),
        redirectUri = "https://callback.invalid/bars",
        itmoId = identity,
    )
    private val verifier = "a".repeat(43)
    private val tokenBody = """{"access_token":"synthetic-access","refresh_token":"synthetic-refresh","id_token":"synthetic-id","expires_in":300,"refresh_expires_in":3600}"""

    @Test
    fun loginAndScheduleSamplesUseAtomicStorageAndValidatedCallback() = runTest {
        val storage = MemoryTokens()
        val engine = MockEngine { request ->
            assertEquals("issuer.invalid", request.url.host)
            if (request.url.encodedPath.endsWith("/token")) respond(tokenBody) else {
                assertEquals("/api/schedule/schedule/personal", request.url.encodedPath)
                assertEquals("2026-01-05", request.url.parameters["date_start"])
                assertEquals("2026-01-11", request.url.parameters["date_end"])
                assertEquals("ru", request.headers[HttpHeaders.AcceptLanguage])
                assertTrue(request.headers[HttpHeaders.Authorization] != null)
                respond(fixture("schedule/empty.json"))
            }
        }
        val client = createClient(storage, engine, clock, configuration)
        try {
            val state = Pkce.newState()
            val url = Url(client.identity.loginUrl(Pkce.challenge(verifier), state))
            assertEquals("issuer.invalid", url.host)
            assertEquals("S256", url.parameters["code_challenge_method"])
            assertFalse(completeLogin(client, "https://callback.invalid/myitmo?code=synthetic&state=wrong", state, verifier))
            assertTrue(engine.requestHistory.isEmpty())
            assertTrue(completeLogin(client, "https://callback.invalid/myitmo?code=synthetic&state=$state", state, verifier))
            assertTrue(storage.read() != null)
            assertEquals(emptyList(), readSchedule(client))
            client.tokens.replaceTokens(null)
            assertEquals(null, storage.read())
        } finally { client.close(); engine.close() }
    }

    @Test
    fun barsSamplesReplayCallerCookiesAndReadTheSelectedPeriod() = runTest {
        val engine = MockEngine { request ->
            assertEquals("issuer.invalid", request.url.host)
            when {
                request.url.encodedPath.endsWith("/auth") -> {
                    assertTrue(request.headers[HttpHeaders.Cookie] != null)
                    respond("", HttpStatusCode.Found, headersOf(
                        HttpHeaders.Location to listOf("https://callback.invalid/bars?code=synthetic&state=${request.url.parameters["state"]}"),
                        HttpHeaders.SetCookie to listOf("synthetic=rotated; Secure", "synthetic-second=value; Secure"),
                    ))
                }
                request.url.encodedPath.endsWith("/login") -> respond("", headers = headersOf(HttpHeaders.Authorization, "Bearer synthetic-session"))
                request.url.encodedPath.endsWith("current_user/") -> respond(fixture("bars/user.json"))
                else -> {
                    assertEquals("/backend/rest/journal/disciplines", request.url.encodedPath)
                    assertEquals("true", request.url.parameters["withCheckpointPlansOnly"])
                    assertTrue(request.headers[HttpHeaders.Authorization] != null)
                    respond(fixture("bars/disciplines.json"))
                }
            }
        }
        val login = BarsLogin(engine, barsConfiguration)
        val bars = BarsClient(engine, barsConfiguration, RuntimeBarsStorage())
        try {
            var cookies = emptyList<String>()
            val state = login.newState()
            assertEquals(BarsSessionCode.Outcome.CODE, replayBarsLogin(login, bars, state, "synthetic=caller") { cookies = it })
            assertEquals(2, cookies.size)
            assertTrue(bars.hasSession())
            assertTrue(readBarsDisciplines(bars).isNotEmpty())
        } finally { login.close(); bars.close(); engine.close() }
    }

    @Test
    fun replaySampleDistinguishesAllNonCodeOutcomesWithoutClearingBarsSession() = runTest {
        for ((status, location, outcome) in listOf(
            Triple(HttpStatusCode.OK, "", BarsSessionCode.Outcome.LOGIN_REQUIRED),
            Triple(HttpStatusCode.Found, "https://callback.invalid/foreign", BarsSessionCode.Outcome.REJECTED),
            Triple(HttpStatusCode.ServiceUnavailable, "", BarsSessionCode.Outcome.HTTP_ERROR),
        )) {
            val engine = MockEngine { respond("", status, headersOf(HttpHeaders.Location, location)) }
            val storage = RuntimeBarsStorage().also { it.setAuthorization("Bearer synthetic-existing") }
            val login = BarsLogin(engine, barsConfiguration)
            val bars = BarsClient(engine, barsConfiguration, storage)
            try {
                assertEquals(outcome, replayBarsLogin(login, bars, login.newState(), "synthetic=caller") {})
                assertEquals(1, engine.requestHistory.size)
                assertTrue(bars.hasSession())
            } finally { login.close(); bars.close(); engine.close() }
        }
    }

    @Test
    fun refreshServerFailurePreservesSnapshotAndCancellationIsNotAuthentication() = runTest {
        val storage = MemoryTokens()
        val snapshot = TokenSet("synthetic-access", clock.now(), "synthetic-refresh", Instant.parse("2027-01-01T00:00:00Z"), "synthetic-id")
        val engine = MockEngine { respond("", HttpStatusCode.ServiceUnavailable) }
        val client = createClient(storage, engine, clock, configuration)
        try {
            client.tokens.replaceTokens(snapshot)
            assertEquals(503, assertFailsWith<MyItmoException.Http> { client.tokens.validAccessToken() }.status)
            assertSame(snapshot, storage.read())
        } finally { client.close(); engine.close() }
        val cancelledEngine = MockEngine { throw CancellationException("Synthetic cancellation") }
        val cancelled = createClient(storage, cancelledEngine, clock, configuration)
        try {
            assertFailsWith<CancellationException> { cancelled.tokens.forceRefresh() }
            assertSame(snapshot, storage.read())
        } finally { cancelled.close(); cancelledEngine.close() }
    }

    private fun repositoryRoot(): File = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .first { File(it, "README.md").isFile && File(it, "src/commonMain").isDirectory }

    private fun documentation(root: File): Map<String, String> =
        (listOf(File(root, "README.md")) + File(root, "docs").listFiles { file -> file.extension == "md" }.orEmpty().sorted())
            .associate { it.relativeTo(root).invariantSeparatorsPath to it.readText() }

    @Test
    fun documentationContainsTheActualCompiledSampleBlocks() {
        val root = repositoryRoot()
        val text = documentation(root).values.joinToString("\n")
        val tests = "src/jvmTest"
        val samples = listOf(
            Triple("kotlin", "$tests/kotlin/dev/alllexey/itmoapi/ReadmeSamplesTest.kt",
                listOf("file", "relogin", "sportfind", "sportcancel", "barsjournal", "session", "errors", "sport", "storage", "client", "login",
                    "schedule", "bars", "cookies")),
            Triple("kotlin", "$tests/kotlin/dev/alllexey/itmoapi/readme/QuickStart.kt", listOf("quickstart")),
            Triple("kotlin", "$tests/kotlin/dev/alllexey/itmoapi/readme/Android.kt", listOf("android")),
            Triple("java", "$tests/java/dev/alllexey/itmoapi/readme/Main.java", listOf("java")),
            Triple("java", "$tests/java/dev/alllexey/itmoapi/readme/Extras.java", listOf("javasession", "javaerrors", "javasport", "javadates", "javacall")),
        )
        for ((language, path, names) in samples) {
            val source = File(root, path).readText()
            for (name in names) {
                val sample = source.substringAfter("// README $name begin\n").substringBefore("// README $name end")
                    .trimIndent().trimEnd()
                assertTrue(text.contains("```$language\n$sample\n```"), "Documentation sample differs: $name")
            }
        }
    }

    /** docs/api.md is the method reference: every public call of every area and of BARS must be listed there. */
    @Test
    fun theReferenceListsEveryPublicCall() {
        val reference = documentation(repositoryRoot()).getValue("docs/api.md")
        val areas = mapOf(
            "schedule" to dev.alllexey.itmoapi.myitmo.schedule.ScheduleApi::class.java,
            "recordBook" to dev.alllexey.itmoapi.myitmo.recordbook.RecordBookApi::class.java,
            "personalities" to dev.alllexey.itmoapi.myitmo.personalities.PersonalitiesApi::class.java,
            "studyPlan" to dev.alllexey.itmoapi.myitmo.studyplan.StudyPlanApi::class.java,
            "qr" to dev.alllexey.itmoapi.myitmo.qr.QrApi::class.java,
            "sport" to dev.alllexey.itmoapi.myitmo.sport.SportApi::class.java,
            "election" to dev.alllexey.itmoapi.myitmo.election.ElectionApi::class.java,
            "finances" to dev.alllexey.itmoapi.myitmo.finances.FinancesApi::class.java,
            "dormitory" to dev.alllexey.itmoapi.myitmo.dormitory.DormitoryApi::class.java,
            "requests" to dev.alllexey.itmoapi.myitmo.requests.RequestsApi::class.java,
            "system" to dev.alllexey.itmoapi.myitmo.system.SystemApi::class.java,
            "bars" to BarsClient::class.java,
        )
        val missing = areas.flatMap { (area, type) ->
            type.declaredMethods
                .filter { java.lang.reflect.Modifier.isPublic(it.modifiers) && !it.isSynthetic && '$' !in it.name }
                .filter { it.parameterTypes.lastOrNull() == kotlin.coroutines.Continuation::class.java }
                .map { "$area.${it.name}" }
                .filter { "`$it" !in reference }
        }
        assertEquals(emptyList(), missing.sorted(), "Calls missing from docs/api.md")
    }
}
