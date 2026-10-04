package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.CancellationException

/** Consumer-owned persistent session. Each operation reads or replaces all five values atomically.
 * Implementations use Android Keystore, iOS Keychain or a Backend database; null means no session.
 * Never log snapshots. After client construction, route login/logout writes through TokenManager.
 */
public interface TokenStorage {
    /** Returns one coherent snapshot, never values assembled from separate rotations. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun read(): TokenSet?

    /** Atomically replaces the complete snapshot, or removes it when [tokens] is null. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun write(tokens: TokenSet?)
}

/** Consumer seam for a cross-process refresh lock, for example an iOS App Group file lock.
 * Run [action] exactly once while exclusively locked; release in finally, including cancellation.
 * All clients sharing storage must share this guard. The action re-reads storage after acquisition.
 * The default manager uses only its own in-process Mutex; no global lock or token state exists.
 */
public fun interface TokenRefreshGuard {
    /** Serializes the read/refresh/write transaction and returns its complete snapshot. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun withLock(action: suspend () -> TokenSet?): TokenSet?
}
