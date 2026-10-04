package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Typed MyITMO finances operations; callers own authentication and result unwrapping. */
public interface FinancesApi {
    /** GET /api/finances/scholarship/total; Totals in rubles for an optional inclusive date range; omitted bounds are not sent. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getScholarshipTotals(dateFrom: LocalDate? = null, dateTo: LocalDate? = null): ResultResponse<List<ScholarshipTotal>>

}

internal class FinancesApiImpl(private val transport: ItmoTransport) : FinancesApi {
    override suspend fun getScholarshipTotals(dateFrom: LocalDate?, dateTo: LocalDate?): ResultResponse<List<ScholarshipTotal>> = transport.execute(
        ResultResponse.serializer(ListSerializer(ScholarshipTotal.serializer())), HttpMethod.Get, "api/finances/scholarship/total",
    ) {
        url { encodedPath = "/api/finances/scholarship/total" }
        if (dateFrom != null) parameter("dateFrom", dateFrom.toQueryValue())
        if (dateTo != null) parameter("dateTo", dateTo.toQueryValue())
    }

}
