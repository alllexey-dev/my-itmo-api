package dev.alllexey.itmoapi.bars.auth

import dev.alllexey.itmoapi.core.defaultEngine
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.*

class BarsCookieOkHttpTest {
    private fun scenario(block: suspend BarsCookieScenarios.() -> Unit): Unit = runBlocking {
        withTimeout(20_000) { BarsCookieScenarios { OkHttpExchange() }.block() }
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
}

private class OkHttpExchange : CookieExchange {
    private val server = MockWebServer().apply { start() }
    private var stopped = false
    override val engine = CookieRoutingEngine(defaultEngine(), Url("http://127.0.0.1:${server.port}/"))

    override suspend fun enqueue(reply: CookieReply) {
        server.enqueue(MockResponse().setResponseCode(reply.status).apply {
            reply.location?.let { addHeader(HttpHeaders.Location, it) }
            reply.setCookies.forEach { addHeader(HttpHeaders.SetCookie, it) }
            if (reply.unreadBody) {
                setBody("x".repeat(1024 * 1024))
                throttleBody(1, 30, TimeUnit.SECONDS)
            }
        })
    }

    override suspend fun takeRequest(): CookieRequest {
        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertTrue(request != null, "Expected authorization request must arrive")
        return CookieRequest(request.method.orEmpty(), request.path.orEmpty(), request.headers.values(HttpHeaders.Cookie),
            request.getHeader(HttpHeaders.Authorization))
    }
    override suspend fun assertNoRequest() {
        assertTrue(server.takeRequest(150, TimeUnit.MILLISECONDS) == null, "No redirect or extra request may occur")
    }
    override suspend fun stop() { if (!stopped) { stopped = true; server.shutdown() } }
    override suspend fun close() { engine.close(); stop() }
}
