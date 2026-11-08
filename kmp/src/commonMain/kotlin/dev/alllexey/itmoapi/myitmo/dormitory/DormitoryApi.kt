package dev.alllexey.itmoapi.myitmo.dormitory

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Read-only MyITMO dormitory operations; callers own authentication and result unwrapping.
 * Settlement registration, cancellation, category change and payment calls are deliberately not exposed.
 */
public interface DormitoryApi {
    /** GET /api/dormitory/status/full; dormitory stage, queue place, dormitory type and the assigned dormitory.
     * A student without a dormitory application gets a successful envelope with a null result.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryStatus(): ResultResponse<DormitoryStatus>

    /** GET /api/dormitory/payments/contracts/periods; academic years that have rental agreements, empty without any. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryContractPeriods(): ResultResponse<List<DormitoryContractPeriod>>

    /** GET /api/dormitory/payments/contracts; rental agreements of one period with balance and payment schedule.
     * Pass [DormitoryContractPeriod.dateFrom] and [DormitoryContractPeriod.dateTo]; they are sent as ISO
     * yyyy-MM-dd `from` and `to`. A period without agreements answers with an empty list.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryContracts(dateFrom: LocalDate, dateTo: LocalDate): ResultResponse<List<DormitoryContract>>

    /** GET /api/dormitory/documents; documents ITMO needs before settlement and their status, empty without any. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryDocuments(): ResultResponse<List<DormitoryDocument>>
}

internal class DormitoryApiImpl(private val transport: ItmoTransport) : DormitoryApi {
    override suspend fun getDormitoryStatus(): ResultResponse<DormitoryStatus> = transport.execute(
        ResultResponse.serializer(DormitoryStatus.serializer()), HttpMethod.Get, "api/dormitory/status/full",
    )

    override suspend fun getDormitoryContractPeriods(): ResultResponse<List<DormitoryContractPeriod>> = transport.execute(
        ResultResponse.serializer(ListSerializer(DormitoryContractPeriod.serializer())), HttpMethod.Get, "api/dormitory/payments/contracts/periods",
    )

    override suspend fun getDormitoryContracts(dateFrom: LocalDate, dateTo: LocalDate): ResultResponse<List<DormitoryContract>> = transport.execute(
        ResultResponse.serializer(ListSerializer(DormitoryContract.serializer())), HttpMethod.Get, "api/dormitory/payments/contracts",
    ) {
        parameter("from", dateFrom.toQueryValue())
        parameter("to", dateTo.toQueryValue())
    }

    override suspend fun getDormitoryDocuments(): ResultResponse<List<DormitoryDocument>> = transport.execute(
        ResultResponse.serializer(ListSerializer(DormitoryDocument.serializer())), HttpMethod.Get, "api/dormitory/documents",
    )
}
