package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.CancellationException

/** Caller-owned OIDC code acquisition. Null means interactive authentication is required. */
public fun interface BarsCodeSupplier {
    /** Obtains a code for the supplied callback state; the caller validates its callback. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun obtainCode(state: String): String?
}
