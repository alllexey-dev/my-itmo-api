package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.system.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [SystemApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingSystem internal constructor(private val api: SystemApi) {
    /** Blocking [SystemApi.getDashboard]. */
    @Throws(MyItmoException::class)
    public fun getDashboard(): List<DashboardItem> = runBlocking { api.getDashboard() }

    /** Blocking [SystemApi.getMenuItems]. */
    @Throws(MyItmoException::class)
    public fun getMenuItems(): MenuResponse = runBlocking { api.getMenuItems() }

    /** Blocking [SystemApi.getServices]. */
    @Throws(MyItmoException::class)
    public fun getServices(): MenuResponse = runBlocking { api.getServices() }
}
