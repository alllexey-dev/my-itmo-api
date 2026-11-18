package dev.alllexey.itmoapi.bars.auth

import dev.alllexey.itmoapi.core.defaultEngine
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSHTTPCookieStorage
import platform.Foundation.NSURL
import kotlin.test.*

/** Actual Darwin cookie-login wire cases, run only by the existing macOS CI simulator job. */
class BarsCookieDarwinTest {
    private fun scenario(block: suspend BarsCookieScenarios.() -> Unit): Unit = runBlocking {
        withTimeout(20_000) { BarsCookieScenarios { DarwinExchange() }.block() }
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

private class DarwinExchange : CookieExchange {
    private val server = BarsLoginLoopbackServer()
    override val engine = CookieRoutingEngine(defaultEngine(), Url(server.baseUrl))
    override suspend fun enqueue(reply: CookieReply) { server.enqueue(reply) }
    override suspend fun takeRequest(): CookieRequest = server.takeRequest()
    override suspend fun assertNoRequest() { server.assertNoRequest() }
    override suspend fun assertNoStoredCookies() {
        val stored = NSHTTPCookieStorage.sharedHTTPCookieStorage.cookiesForURL(NSURL(string = server.baseUrl)).orEmpty()
        assertTrue(stored.isEmpty(), "No replay cookies may enter shared Apple storage")
    }
    override suspend fun stop() { server.stop() }
    override suspend fun close() { engine.close(); server.stop() }
}
