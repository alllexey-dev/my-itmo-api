package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.dormitory.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDate

/** Blocking view of [DormitoryApi] for Java: each method waits for the result on the calling thread.
 * Dates are `java.time.LocalDate`. Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingDormitory internal constructor(private val api: DormitoryApi) {
    /** Blocking [DormitoryApi.getDormitoryStatus]. */
    @Throws(MyItmoException::class)
    public fun getDormitoryStatus(): DormitoryStatus? = runBlocking { api.getDormitoryStatus() }

    /** Blocking [DormitoryApi.getDormitoryContractPeriods]. */
    @Throws(MyItmoException::class)
    public fun getDormitoryContractPeriods(): List<DormitoryContractPeriod> =
        runBlocking { api.getDormitoryContractPeriods() }

    /** Blocking [DormitoryApi.getDormitoryContracts]. */
    @Throws(MyItmoException::class)
    public fun getDormitoryContracts(dateFrom: LocalDate, dateTo: LocalDate): List<DormitoryContract> =
        runBlocking { api.getDormitoryContracts(dateFrom.toKotlinLocalDate(), dateTo.toKotlinLocalDate()) }

    /** Blocking [DormitoryApi.getDormitoryDocuments]. */
    @Throws(MyItmoException::class)
    public fun getDormitoryDocuments(): List<DormitoryDocument> = runBlocking { api.getDormitoryDocuments() }
}
