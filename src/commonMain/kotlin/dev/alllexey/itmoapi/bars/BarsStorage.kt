package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Stores the complete raw authorization header. BARS has no refresh token. */
public interface BarsStorage {
    /** Full Bearer header, or null when no session exists. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getAuthorization(): String?

    /** Persists the header; null explicitly removes the session. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun setAuthorization(authorization: String?)
}

/** Caller-local in-memory session storage; diagnostics never reveal its header. */
public class RuntimeBarsStorage : BarsStorage {
    private val mutex = Mutex()
    private var authorization: String? = null

    @Throws(MyItmoException::class, CancellationException::class)
    override suspend fun getAuthorization(): String? = mutex.withLock { authorization }

    @Throws(MyItmoException::class, CancellationException::class)
    override suspend fun setAuthorization(authorization: String?): Unit = mutex.withLock {
        this.authorization = authorization
    }

    override fun toString(): String = "RuntimeBarsStorage(redacted)"
}
