package dev.alllexey.itmoapi.myitmo.requests

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.myItmoAreaExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Instant

class RequestsApiTest {
    @Test
    fun currentUserRequestsKeepNoticeExtensibleStatusAndBothInstants() = runTest {
        val request = myItmoAreaExchange("requests/my", fixture("requests/my.json")) {
            RequestsApiImpl(it).getMyRequests().requireResult()
        }.single()
        assertEquals(RequestSummary(7001, "Synthetic request", "Synthetic notice", 91, "Synthetic status",
            Instant.parse("2026-01-05T06:00:00Z"), Instant.parse("2026-01-06T07:30:00Z")), request)
    }

    @Test
    fun emptyRequestsAndHttp400Api100ArePreserved() = runTest {
        assertTrue(myItmoAreaExchange("requests/my", fixture("requests/empty.json")) {
            RequestsApiImpl(it).getMyRequests().requireResult()
        }.isEmpty())
        val failure = assertFailsWith<MyItmoException.Api> {
            myItmoAreaExchange("requests/my", fixture("requests/error.json"), status = 400) {
                RequestsApiImpl(it).getMyRequests()
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
    }
}
