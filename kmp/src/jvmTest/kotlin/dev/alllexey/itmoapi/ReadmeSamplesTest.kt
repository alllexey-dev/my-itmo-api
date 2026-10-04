package dev.alllexey.itmoapi

// README imports begin
import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.bars.auth.BarsLogin
import dev.alllexey.itmoapi.bars.auth.BarsSessionCode
import dev.alllexey.itmoapi.bars.model.Discipline
import dev.alllexey.itmoapi.bars.model.Term
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.itmoid.CallbackUrl
import dev.alllexey.itmoapi.itmoid.TokenSet
import dev.alllexey.itmoapi.itmoid.TokenStorage
import dev.alllexey.itmoapi.myitmo.MyItmoClient
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import dev.alllexey.itmoapi.myitmo.schedule.Schedule
import io.ktor.client.engine.HttpClientEngine
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
// README imports end
import java.io.File
import dev.alllexey.itmoapi.parity.ParityCompletenessTest
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
    // README storage begin
    class MemoryTokens : TokenStorage {
        private var snapshot: TokenSet? = null
        override suspend fun read(): TokenSet? = snapshot
        override suspend fun write(tokens: TokenSet?) { snapshot = tokens }
        override fun toString(): String = "MemoryTokens(redacted)"
    }
    // README storage end

    // README client begin
    fun createClient(
        storage: TokenStorage,
        engine: HttpClientEngine,
        clock: Clock,
        configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
    ): MyItmoClient = MyItmoClient(configuration, storage, engine, clock)
    // README client end

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
        ).requireResult()
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
        .first { File(it, "README.md").isFile && File(it, "kmp/src/commonMain").isDirectory }

    @Test
    fun readmeContainsTheActualCompiledSampleBlocks() {
        val root = repositoryRoot()
        val source = File(root, "kmp/src/jvmTest/kotlin/dev/alllexey/itmoapi/ReadmeSamplesTest.kt").readText()
        val readme = File(root, "README.md").readText()
        for (name in listOf("imports", "storage", "client", "login", "schedule", "bars", "cookies")) {
            val sample = source.substringAfter("// README $name begin\n").substringBefore("// README $name end")
                .trimIndent().trimEnd()
            assertTrue(readme.contains("```kotlin\n$sample\n```"), "README sample differs: $name")
        }
    }

    @Test
    fun migrationGuideContainsTheFullFreshlyGeneratedMemberMap() {
        ParityCompletenessTest().allLegacyModelsAndMembersAreAccountedFor()
        val root = repositoryRoot()
        val generated = File(root, "kmp/build/parity/member-map.md").readText()
        val migration = File(root, "docs/migration.md").readText()
        val embedded = migration.substringAfter("<!-- Generated member map begin -->\n")
            .substringBefore("\n<!-- Generated member map end -->")
        assertEquals(generated, embedded)
    }

}
