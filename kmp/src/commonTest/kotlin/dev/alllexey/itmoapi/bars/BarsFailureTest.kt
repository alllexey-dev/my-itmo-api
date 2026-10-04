package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BarsFailureTest {
    private val configuration = BarsConfiguration(restUrl = Url("https://synthetic.invalid/backend/rest/"))
    private val header = "Bearer synthetic-existing-session"

    @Test
    fun forbiddenAndServerErrorsDoNotRenewOrEraseTheSession() = runTest {
        for (status in listOf(HttpStatusCode.Forbidden, HttpStatusCode.BadGateway)) {
            var supplied = 0
            val storage = RuntimeBarsStorage().also { it.setAuthorization(header) }
            val engine = MockEngine { respond("synthetic-sensitive-body", status) }
            val client = BarsClient(engine, configuration, storage, BarsCodeSupplier { supplied++; "synthetic-code" })
            try {
                val error = if (status == HttpStatusCode.Forbidden) {
                    assertFailsWith<MyItmoException.Auth> { client.getCurrentUser() }
                } else assertFailsWith<MyItmoException.Http> { client.getCurrentUser() }
                assertEquals(0, supplied)
                assertFalse("synthetic-sensitive-body" in error.toString())
                assertTrue(storage.getAuthorization() == header)
            } finally { client.close(); engine.close() }
        }
    }

    @Test
    fun loginNeverFollowsRedirectsAndInvalidRotationNeverReplacesTheSession() = runTest {
        val storage = RuntimeBarsStorage().also { it.setAuthorization(header) }
        var calls = 0
        val engine = MockEngine {
            if (calls++ == 0) respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, "https://untrusted.invalid/"))
            else respond(fixture("bars/user.json"), headers = headersOf(HttpHeaders.Authorization, "invalid-synthetic-header"))
        }
        val client = BarsClient(engine, configuration, storage)
        try {
            assertEquals(302, assertFailsWith<MyItmoException.Http> { client.login("synthetic-code") }.status)
            assertEquals(1, calls)
            client.getCurrentUser()
            assertTrue(storage.getAuthorization() == header)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun engineAndParserFailuresDoNotExposeRequestOrResponseData() = runTest {
        val sentinel = "synthetic-sensitive-engine-sentinel"
        val storage = RuntimeBarsStorage().also { it.setAuthorization(header) }
        val engine = MockEngine { throw IllegalStateException(sentinel) }
        val client = BarsClient(engine, configuration, storage)
        try {
            val error = assertFailsWith<MyItmoException.Decode> { client.login(sentinel) }
            assertNull(error.cause)
            assertFalse(sentinel in error.toString())
            assertTrue(storage.getAuthorization() == header)
        } finally { client.close(); engine.close() }
    }

    @Test
    fun cancellationDuringRenewalPropagatesAndReleasesTheSessionMutex() = runTest {
        val storage = RuntimeBarsStorage().also { it.setAuthorization(header) }
        val cancelled = CancellationException("Synthetic cancellation")
        val engine = MockEngine { respond("", HttpStatusCode.Unauthorized) }
        val client = BarsClient(engine, configuration, storage, BarsCodeSupplier { throw cancelled })
        try {
            assertSame(cancelled, assertFailsWith<CancellationException> { client.getCurrentUser() })
            assertTrue(storage.getAuthorization() == header)
            assertTrue(client.hasSession())
            client.logout()
            assertFalse(client.hasSession())
        } finally { client.close(); engine.close() }
    }
}
