package dev.alllexey.itmoapi.itmoid

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TokenTestClock : Clock {
    var instant = Instant.parse("2026-10-03T09:00:00Z")
    override fun now(): Instant = instant
}

internal fun testTokens(clock: Clock, expired: Boolean = false): TokenSet = TokenSet(
    Uuid.random().toString(), clock.now() + (if (expired) (-1).seconds else 3600.seconds),
    Uuid.random().toString(), clock.now() + 86400.seconds, Uuid.random().toString(),
)

internal class TokenTestStorage(var snapshot: TokenSet? = null) : TokenStorage {
    var writes = 0
    override suspend fun read(): TokenSet? = snapshot
    override suspend fun write(tokens: TokenSet?) { snapshot = tokens; writes++ }
    override fun toString(): String = "TokenTestStorage(redacted)"
}

internal fun tokenBody(tokens: TokenSet): String = """{
    "access_token":"${tokens.accessToken}","refresh_token":"${tokens.refreshToken}",
    "id_token":"${tokens.idToken}","expires_in":3600,"refresh_expires_in":86400
}"""
