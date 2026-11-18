package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import dev.alllexey.itmoapi.itmoid.TokenSet
import dev.alllexey.itmoapi.itmoid.TokenTestStorage
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class BlockingMyItmoTest {
    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-10-12T09:00:00Z")
    }
    private val configuration = MyItmoConfiguration(
        Url("https://api.invalid"),
        ItmoIdConfiguration("https://issuer.invalid/realm", "synthetic-client", "https://callback.invalid/login"),
    )
    private val snapshot = TokenSet(
        "synthetic-access", Instant.parse("2026-10-12T09:30:00Z"),
        "synthetic-refresh", Instant.parse("2026-11-11T09:00:00Z"), "synthetic-id",
    )

    @Test
    fun callsWaitForThePayloadAndThrowTheSameTypedErrors() {
        val queries = mutableListOf<String>()
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/schedule/schedule/personal" -> {
                    queries += request.url.encodedQuery
                    respond(fixture("schedule/empty.json"), headers = json)
                }
                "/api/dormitory/status/full" -> respond(fixture("dormitory/status-empty.json"), headers = json)
                "/api/dormitory/documents" -> respond(fixture("dormitory/error.json"), HttpStatusCode.BadRequest, json)
                else -> error("Unexpected request")
            }
        }
        BlockingMyItmo.of(MyItmo.create(engine, true, TokenTestStorage(snapshot), configuration, clock)).use { itmo ->
            assertTrue(itmo.hasSession())
            assertEquals(emptyList(), itmo.schedule.getPersonalSchedule(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)))
            assertEquals(listOf("date_start=2026-10-12&date_end=2026-10-18"), queries)
            assertNull(itmo.dormitory.getDormitoryStatus())
            assertEquals(400, assertFailsWith<MyItmoException.Api> { itmo.dormitory.getDormitoryDocuments() }.status)
            assertEquals("BlockingMyItmo(redacted)", itmo.toString())
        }
    }
}
