package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.requests.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [RequestsApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingRequests internal constructor(private val api: RequestsApi) {
    /** Blocking [RequestsApi.getMyRequests]. */
    @Throws(MyItmoException::class)
    public fun getMyRequests(): List<RequestSummary> = runBlocking { api.getMyRequests() }
}
