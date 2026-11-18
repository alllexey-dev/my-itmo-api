package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.finances.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDate

/** Blocking view of [FinancesApi] for Java: each method waits for the result on the calling thread.
 * Dates are `java.time.LocalDate`. Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingFinances internal constructor(private val api: FinancesApi) {
    /** Blocking [FinancesApi.getScholarshipTotals]. */
    @JvmOverloads
    @Throws(MyItmoException::class)
    public fun getScholarshipTotals(dateFrom: LocalDate? = null, dateTo: LocalDate? = null): List<ScholarshipTotal> =
        runBlocking { api.getScholarshipTotals(dateFrom?.toKotlinLocalDate(), dateTo?.toKotlinLocalDate()) }

    /** Blocking [FinancesApi.getTuitionAvailability]. */
    @Throws(MyItmoException::class)
    public fun getTuitionAvailability(): Boolean = runBlocking { api.getTuitionAvailability() }

    /** Blocking [FinancesApi.getTuitionContracts]. */
    @Throws(MyItmoException::class)
    public fun getTuitionContracts(): List<TuitionContract> = runBlocking { api.getTuitionContracts() }

    /** Blocking [FinancesApi.getTuitionPayments]. */
    @Throws(MyItmoException::class)
    public fun getTuitionPayments(): List<TuitionPaymentContract> = runBlocking { api.getTuitionPayments() }
}
