package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TokenManagerTest {
    private val clock = TokenTestClock()

    @Test
    fun tenConcurrentExpiredAccessCallsRefreshOnceAndWriteOneCompleteSnapshot() = runTest {
        val storage = TokenTestStorage(testTokens(clock, expired = true))
        val rotation = testTokens(clock)
        var refreshes = 0
        val engine = MockEngine {
            refreshes++
            delay(10)
            respond(tokenBody(rotation), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val identity = ItmoIdClient(engine, clock)
        val manager = TokenManager(storage, identity, clock)
        try {
            val results = List(10) { async { manager.validAccessToken() } }.awaitAll()
            assertTrue(results.all { it == rotation.accessToken }, "Every caller receives the rotation")
            assertEquals(1, refreshes)
            assertEquals(1, storage.writes)
            assertTrue(storage.snapshot?.refreshToken == rotation.refreshToken, "Refresh rotation persisted")
            assertTrue(storage.snapshot?.idToken == rotation.idToken, "Identity persisted atomically")
            assertEquals(clock.now() + 3600.seconds, storage.snapshot?.accessExpiresAt)
        } finally { identity.close(); engine.close() }
    }

    @Test
    fun forcedConcurrentCallsOnAStillValidTokenRefreshOnlyOnce() = runTest {
        val storage = TokenTestStorage(testTokens(clock))
        val rotation = testTokens(clock)
        var refreshes = 0
        val engine = MockEngine {
            refreshes++
            delay(10)
            respond(tokenBody(rotation), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val identity = ItmoIdClient(engine, clock)
        val manager = TokenManager(storage, identity, clock)
        try {
            List(10) { async { manager.forceRefresh() } }.awaitAll()
            assertEquals(1, refreshes)
            assertEquals(1, storage.writes)
        } finally { identity.close(); engine.close() }
    }

    @Test
    fun accessAtSkewBoundaryRefreshesAndHealthyAccessDoesNot() = runTest {
        val healthy = testTokens(clock)
        val storage = TokenTestStorage(healthy)
        var refreshes = 0
        val engine = MockEngine {
            refreshes++
            respond(tokenBody(testTokens(clock)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val identity = ItmoIdClient(engine, clock)
        val manager = TokenManager(storage, identity, clock)
        try {
            assertTrue(manager.validAccessToken() == healthy.accessToken, "Healthy access reused")
            assertEquals(0, refreshes)
            clock.instant = healthy.accessExpiresAt - 30.seconds
            manager.validAccessToken()
            assertEquals(1, refreshes)
        } finally { identity.close(); engine.close() }
    }

    @Test
    fun missingAndExpiredRefreshAreAuthAndDoNotCallIdentityOrEraseStorage() = runTest {
        val engine = MockEngine { error("Expired refresh must not reach the network") }
        val identity = ItmoIdClient(engine, clock)
        val storage = TokenTestStorage()
        val manager = TokenManager(storage, identity, clock)
        try {
            assertTrue(manager.isRefreshTokenExpired())
            assertFailsWith<MyItmoException.Auth> { manager.validAccessToken() }
            val expired = testTokens(clock, expired = true)
            clock.instant = expired.refreshExpiresAt
            storage.snapshot = expired
            assertTrue(manager.isRefreshTokenExpired())
            assertFailsWith<MyItmoException.Auth> { manager.forceRefresh() }
            assertSame(expired, storage.snapshot)
            assertEquals(0, storage.writes)
        } finally { identity.close(); engine.close() }
    }

    @Test
    fun recognizedOAuthRejectionIsAuthWithoutSnapshotWrites() = runTest {
        for (status in listOf(400, 401, 403)) {
            val stored = testTokens(clock, expired = true)
            val storage = TokenTestStorage(stored)
            val engine = MockEngine { respond("""{"error":"invalid_grant"}""", HttpStatusCode.fromValue(status)) }
            val identity = ItmoIdClient(engine, clock)
            try {
                val failure = assertFailsWith<MyItmoException.Auth> { TokenManager(storage, identity, clock).validAccessToken() }
                assertFalse(failure.toString().contains(stored.refreshToken))
                assertSame(stored, storage.snapshot)
                assertEquals(0, storage.writes)
            } finally { identity.close(); engine.close() }
        }
    }

    @Test
    fun serverDecodeNetworkAndCancellationLeaveSessionUntouchedAndUnlockManager() = runTest {
        for (kind in listOf("http", "decode", "network", "cancel")) {
            val stored = testTokens(clock, expired = true)
            val storage = TokenTestStorage(stored)
            var calls = 0
            val engine = MockEngine {
                calls++
                if (calls > 1) return@MockEngine respond(tokenBody(testTokens(clock)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
                when (kind) {
                    "http" -> respond("{}", HttpStatusCode.ServiceUnavailable)
                    "decode" -> respond("{}")
                    "network" -> throw IOException("Synthetic engine failure")
                    else -> throw CancellationException("Synthetic cancellation")
                }
            }
            val identity = ItmoIdClient(engine, clock)
            val manager = TokenManager(storage, identity, clock)
            try {
                when (kind) {
                    "http" -> assertFailsWith<MyItmoException.Http> { manager.validAccessToken() }
                    "decode" -> assertFailsWith<MyItmoException.Decode> { manager.validAccessToken() }
                    "network" -> assertFailsWith<MyItmoException.Network> { manager.validAccessToken() }
                    else -> assertFailsWith<CancellationException> { manager.validAccessToken() }
                }
                assertSame(stored, storage.snapshot)
                assertEquals(0, storage.writes)
                manager.validAccessToken()
                assertEquals(1, storage.writes)
            } finally { identity.close(); engine.close() }
        }
    }

    @Test
    fun sharedGuardRereadsStorageAndCoalescesTwoIndependentClientRefreshers() = runTest {
        val storage = TokenTestStorage(testTokens(clock, expired = true))
        val crossProcessLock = Mutex()
        var locked = false
        var lockEntries = 0
        val guard = TokenRefreshGuard { action ->
            crossProcessLock.withLock {
                locked = true
                lockEntries++
                try { action() } finally { locked = false }
            }
        }
        var refreshes = 0
        val engine = MockEngine {
            assertTrue(locked)
            refreshes++
            delay(10)
            respond(tokenBody(testTokens(clock)), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val identity = ItmoIdClient(engine, clock)
        val first = TokenManager(storage, identity, clock, refreshGuard = guard)
        val second = TokenManager(storage, identity, clock, refreshGuard = guard)
        try {
            listOf(async { first.validAccessToken() }, async { second.validAccessToken() }).awaitAll()
            assertEquals(1, refreshes)
            assertEquals(1, storage.writes)
            assertEquals(2, lockEntries)
            assertFalse(locked)
        } finally { identity.close(); engine.close() }
    }

    @Test
    fun loginAndLogoutReplaceAtomicSnapshotThroughManagerAndRemainRedacted() = runTest {
        val engine = MockEngine { error("Storage replacement must not use the network") }
        val identity = ItmoIdClient(engine, clock)
        val storage = TokenTestStorage()
        val manager = TokenManager(storage, identity, clock)
        try {
            val login = testTokens(clock)
            manager.replaceTokens(login)
            assertSame(login, storage.read())
            assertFalse(manager.isRefreshTokenExpired())
            assertEquals("TokenManager(redacted)", manager.toString())
            assertEquals("TokenTestStorage(redacted)", storage.toString())
            manager.replaceTokens(null)
            assertNull(storage.read())
            assertEquals(2, storage.writes)
        } finally { identity.close(); engine.close() }
    }
}
