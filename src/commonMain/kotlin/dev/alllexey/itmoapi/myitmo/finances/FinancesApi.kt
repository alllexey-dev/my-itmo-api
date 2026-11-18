package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Typed MyITMO finances operations. */
public interface FinancesApi {
    /** GET /api/finances/scholarship/total; Totals in rubles for an optional inclusive date range; omitted bounds are not sent. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getScholarshipTotals(dateFrom: LocalDate? = null, dateTo: LocalDate? = null): List<ScholarshipTotal>

    /** GET /api/finances/edupayments/availability; whether the student has paid-education (tuition) agreements;
     * false for state-funded students. Read-only: the payment session call is deliberately not exposed.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTuitionAvailability(): Boolean

    /** GET /api/finances/edupayments/contracts; tuition agreements; the site shows the first one as current and the rest as archived.
     * State-funded students get an empty list; a null result, which the published schema allows, is returned as empty.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTuitionContracts(): List<TuitionContract>

    /** GET /api/finances/edupayments/payments; tuition agreements with their balance and schedule of payments.
     * State-funded students get an empty list; a null result, which the published schema allows, is returned as empty.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTuitionPayments(): List<TuitionPaymentContract>
}

internal class FinancesApiImpl(private val transport: ItmoTransport) : FinancesApi {
    override suspend fun getScholarshipTotals(dateFrom: LocalDate?, dateTo: LocalDate?): List<ScholarshipTotal> = transport.result(
        ListSerializer(ScholarshipTotal.serializer()), HttpMethod.Get, "api/finances/scholarship/total",
    ) {
        if (dateFrom != null) parameter("dateFrom", dateFrom.toQueryValue())
        if (dateTo != null) parameter("dateTo", dateTo.toQueryValue())
    }

    override suspend fun getTuitionAvailability(): Boolean = transport.result(
        Boolean.serializer(), HttpMethod.Get, "api/finances/edupayments/availability",
    )

    override suspend fun getTuitionContracts(): List<TuitionContract> = transport.resultOrNull(
        ListSerializer(TuitionContract.serializer()), HttpMethod.Get, "api/finances/edupayments/contracts",
    ).orEmpty()

    override suspend fun getTuitionPayments(): List<TuitionPaymentContract> = transport.resultOrNull(
        ListSerializer(TuitionPaymentContract.serializer()), HttpMethod.Get, "api/finances/edupayments/payments",
    ).orEmpty()
}
