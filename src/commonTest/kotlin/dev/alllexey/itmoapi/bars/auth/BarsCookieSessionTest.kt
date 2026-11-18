package dev.alllexey.itmoapi.bars.auth

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.commonMockEngine(): CookieExchange = object : CookieExchange {
    private val replies = ArrayDeque<CookieReply>()
    private val requests = ArrayDeque<CookieRequest>()
    private var stopped = false
    override val engine = MockEngine(MockEngineConfig().apply {
        dispatcher = StandardTestDispatcher(testScheduler)
        addHandler { request ->
            if (stopped) throw IOException("Synthetic offline transport")
            assertTrue(replies.isNotEmpty(), "A reply must be scripted")
            val reply = replies.removeFirst()
            requests.addLast(CookieRequest(request.method.value, request.url.encodedPath + "?" + request.url.encodedQuery,
                request.headers.getAll(HttpHeaders.Cookie).orEmpty(), request.headers[HttpHeaders.Authorization]))
            val headers = Headers.build {
                reply.location?.let { append(HttpHeaders.Location, it) }
                reply.setCookies.forEach { append(HttpHeaders.SetCookie, it) }
            }
            if (reply.unreadBody) respond(ByteChannel(), HttpStatusCode.fromValue(reply.status), headers)
            else respond("", HttpStatusCode.fromValue(reply.status), headers)
        }
    })
    override suspend fun enqueue(reply: CookieReply) { replies.addLast(reply) }
    override suspend fun takeRequest(): CookieRequest {
        assertTrue(requests.isNotEmpty(), "Expected authorization request must arrive")
        return requests.removeFirst()
    }
    override suspend fun assertNoRequest() { assertTrue(requests.isEmpty(), "No redirect or extra request may occur") }
    override suspend fun stop() { stopped = true }
    override suspend fun close() { engine.close() }
}

class BarsCookieSessionTest {
    private fun scenario(block: suspend BarsCookieScenarios.() -> Unit) = runTest {
        BarsCookieScenarios { commonMockEngine() }.block()
    }

    @Test fun callbackWithTheSameStateIsACodeAndIsNotFollowed() = scenario { callbackWithTheSameStateIsACodeAndIsNotFollowed() }
    @Test fun callbackWithoutAUsableCodeIsRejected() = scenario { callbackWithoutAUsableCodeIsRejected() }
    @Test fun itmoIdPageMeansLoginIsRequired() = scenario { itmoIdPageMeansLoginIsRequired() }
    @Test fun relativeLocationIsResolvedFromTheRequestUrl() = scenario { relativeLocationIsResolvedFromTheRequestUrl() }
    @Test fun foreignRedirectIsRejectedAndRedirectWithoutLocationIsAnHttpError() = scenario { foreignRedirectIsRejectedAndRedirectWithoutLocationIsAnHttpError() }
    @Test fun loginPageIsLoginRequiredWithoutReadingTheBody() = scenario { loginPageIsLoginRequiredWithoutReadingTheBody() }
    @Test fun serverErrorIsAnHttpError() = scenario { serverErrorIsAnHttpError() }
    @Test fun missingCookiesNeedLoginWithoutARequest() = scenario { missingCookiesNeedLoginWithoutARequest() }
    @Test fun setCookiesAreReturnedInOrderAndNotStoredInTheClientJar() = scenario { setCookiesAreReturnedInOrderAndNotStoredInTheClientJar() }
    @Test fun textFormHidesTheCodeAndCookies() = scenario { textFormHidesTheCodeAndCookies() }
    @Test fun stoppedServerIsANetworkError() = scenario { stoppedServerIsANetworkError() }

    @Test
    fun cancellationAndNonNetworkEngineFailuresRemainRedacted() = runTest {
        listOf(kotlinx.coroutines.CancellationException("Synthetic cancellation"), IllegalStateException("Synthetic plugin failure")).forEach { failure ->
            val engine = MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { throw failure }
            })
            val login = BarsLogin(engine, cookieTestConfiguration())
            try {
                if (failure is kotlinx.coroutines.CancellationException) {
                    assertFailsWith<kotlinx.coroutines.CancellationException> {
                        login.requestCodeWithCookies(login.newState(), "synthetic_cookie=value")
                    }
                } else {
                    assertEquals("Response could not be decoded", assertFailsWith<dev.alllexey.itmoapi.core.MyItmoException.Decode> {
                        login.requestCodeWithCookies(login.newState(), "synthetic_cookie=value")
                    }.message)
                }
            } finally { login.close(); engine.close() }
        }
    }
}
