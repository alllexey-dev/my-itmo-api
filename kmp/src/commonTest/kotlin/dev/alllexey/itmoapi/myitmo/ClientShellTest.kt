package dev.alllexey.itmoapi.myitmo

import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.itmoid.ItmoIdClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Url
import dev.alllexey.itmoapi.itmoid.TokenTestStorage
import dev.alllexey.itmoapi.itmoid.TokenTestClock
import kotlinx.coroutines.Job
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ClientShellTest {
    @Test
    fun everyAreaIsStableAndDoesNotMakeRequestsWhenConstructed() {
        val engine = MockEngine { error("A shell must not make a request") }
        val client = MyItmoClient(MyItmoConfiguration(baseUrl = Url("https://example.invalid/")), TokenTestStorage(), engine, TokenTestClock())
        try {
            assertSame(client.schedule, client.schedule)
            assertSame(client.recordBook, client.recordBook)
            assertSame(client.personalities, client.personalities)
            assertSame(client.studyplan, client.studyplan)
            assertSame(client.qr, client.qr)
            assertSame(client.sport, client.sport)
            assertSame(client.election, client.election)
            assertSame(client.finances, client.finances)
            assertSame(client.requests, client.requests)
            assertSame(client.system, client.system)
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun identityAndBarsShellsDoNotInitializeTransportJustToClose() {
        val engine = MockEngine { error("A shell must not make a request") }
        val identity = ItmoIdClient(engine, object : Clock { override fun now(): Instant = Instant.parse("2026-10-03T09:00:00Z") })
        val bars = BarsClient(engine)
        try {
            identity.close()
            bars.close()
            assertFalse(identity.transport.isInitialized())
            assertFalse(bars.transport.isInitialized())
            assertTrue(engine.coroutineContext[Job]?.isActive == true)
        } finally {
            engine.close()
        }
    }

    @Test
    fun separateMyItmoClientsOwnIndependentAreasWithoutOwningInjectedEngine() {
        val engine = MockEngine { error("A shell must not make a request") }
        val first = MyItmoClient(MyItmoConfiguration.DEFAULT, TokenTestStorage(), engine, TokenTestClock())
        val second = MyItmoClient(MyItmoConfiguration.DEFAULT, TokenTestStorage(), engine, TokenTestClock())
        try {
            assertTrue(first.schedule !== second.schedule)
            first.close()
            assertTrue(engine.coroutineContext[Job]?.isActive == true)
            assertSame(second.schedule, second.schedule)
        } finally {
            second.close()
            engine.close()
        }
    }
}
