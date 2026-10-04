package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** The sole session writer for one client. Access expiry includes [clockSkew]; refresh expiry is absolute.
 * A per-client Mutex and a re-read under [refreshGuard] coalesce successful concurrent refreshes.
 * Recognized OAuth rejection or expired refresh is Auth; other typed failures stay unchanged.
 * All failures leave the stored snapshot untouched.
 * Consumers must not run a legacy refresher against the same storage at the same time.
 */
public class TokenManager(
    private val storage: TokenStorage,
    private val identity: ItmoIdClient,
    private val clock: Clock,
    private val clockSkew: Duration = 30.seconds,
    private val refreshGuard: TokenRefreshGuard = TokenRefreshGuard { it() },
) {
    private val mutex = Mutex()

    init {
        require(clockSkew.isFinite() && !clockSkew.isNegative()) { "Invalid token clock skew" }
    }

    /** Returns a usable access token, refreshing an expired or nearly expired snapshot once. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun validAccessToken(): String = mutex.withLock {
        val tokens = session()
        if (usable(tokens)) tokens.accessToken else guardedRefresh(null).accessToken
    }

    /** Refreshes even a locally valid token, for a missing QR pass or an explicit server rejection.
     * Concurrent callers that observed the same snapshot reuse the first completed rotation.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun forceRefresh(): String {
        val observed = session()
        return refreshObserved(observed.accessToken)
    }

    /** Missing storage and refresh expiry at the current instant both mean the session has ended. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun isRefreshTokenExpired(): Boolean = mutex.withLock {
        storage.read()?.let { it.refreshExpiresAt <= clock.now() } ?: true
    }

    /** Persists an exchanged login snapshot, or logs out, serialized with any in-flight refresh.
     * This is the client's only write entry point; do not mutate storage concurrently from consumers.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun replaceTokens(tokens: TokenSet?) {
        mutex.withLock { refreshGuard.withLock { storage.write(tokens); tokens } }
    }

    internal suspend fun refreshObserved(rejectedAccessToken: String): String = mutex.withLock {
        guardedRefresh(rejectedAccessToken).accessToken
    }

    private suspend fun guardedRefresh(observedAccessToken: String?): TokenSet =
        refreshGuard.withLock {
            val current = session()
            if (usable(current) && (observedAccessToken == null || current.accessToken != observedAccessToken)) {
                current
            } else {
                if (current.refreshExpiresAt <= clock.now()) throw MyItmoException.Auth(401)
                val refreshed = identity.refresh(current.refreshToken)
                storage.write(refreshed)
                refreshed
            }
        } ?: throw MyItmoException.Auth(401)

    private suspend fun session(): TokenSet = storage.read() ?: throw MyItmoException.Auth(401)

    private fun usable(tokens: TokenSet): Boolean = tokens.accessExpiresAt > clock.now() + clockSkew

    override fun toString(): String = "TokenManager(redacted)"
}
