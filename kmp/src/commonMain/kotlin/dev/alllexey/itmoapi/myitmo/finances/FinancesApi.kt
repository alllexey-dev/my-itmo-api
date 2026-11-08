package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Typed MyITMO finances operations; callers own authentication and result unwrapping. */
public interface FinancesApi {
    /** GET /api/finances/scholarship/total; Totals in rubles for an optional inclusive date range; omitted bounds are not sent. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getScholarshipTotals(dateFrom: LocalDate? = null, dateTo: LocalDate? = null): ResultResponse<List<ScholarshipTotal>>

    /** GET /api/finances/edupayments/availability; whether the student has paid-education (tuition) agreements;
     * false for state-funded students. Read-only: the payment session call is deliberately not exposed.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTuitionAvailability(): ResultResponse<Boolean>

    /** GET /api/finances/edupayments/contracts; tuition agreements, newest first as the site shows them.
     * State-funded students get an empty list; the published schema also allows a null result, read it as empty.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTuitionContracts(): ResultResponse<List<TuitionContract>>

    /** GET /api/finances/edupayments/payments; tuition agreements with their balance and schedule of payments.
     * State-funded students get an empty list; the published schema also allows a null result, read it as empty.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getTuitionPayments(): ResultResponse<List<TuitionPaymentContract>>
}

internal class FinancesApiImpl(private val transport: ItmoTransport) : FinancesApi {
    override suspend fun getScholarshipTotals(dateFrom: LocalDate?, dateTo: LocalDate?): ResultResponse<List<ScholarshipTotal>> = transport.execute(
        ResultResponse.serializer(ListSerializer(ScholarshipTotal.serializer())), HttpMethod.Get, "api/finances/scholarship/total",
    ) {
        if (dateFrom != null) parameter("dateFrom", dateFrom.toQueryValue())
        if (dateTo != null) parameter("dateTo", dateTo.toQueryValue())
    }

    override suspend fun getTuitionAvailability(): ResultResponse<Boolean> = transport.execute(
        ResultResponse.serializer(Boolean.serializer()), HttpMethod.Get, "api/finances/edupayments/availability",
    )

    override suspend fun getTuitionContracts(): ResultResponse<List<TuitionContract>> = transport.execute(
        ResultResponse.serializer(ListSerializer(TuitionContract.serializer())), HttpMethod.Get, "api/finances/edupayments/contracts",
    )

    override suspend fun getTuitionPayments(): ResultResponse<List<TuitionPaymentContract>> = transport.execute(
        ResultResponse.serializer(ListSerializer(TuitionPaymentContract.serializer())), HttpMethod.Get, "api/finances/edupayments/payments",
    )
}
