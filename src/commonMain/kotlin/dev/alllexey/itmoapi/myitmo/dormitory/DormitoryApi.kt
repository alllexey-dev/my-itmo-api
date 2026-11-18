package dev.alllexey.itmoapi.myitmo.dormitory

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.toQueryValue
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer

/** Read-only MyITMO dormitory operations.
 * Settlement registration, cancellation, category change and payment calls are deliberately not exposed.
 */
public interface DormitoryApi {
    /** GET /api/dormitory/status/full; dormitory stage, queue place, dormitory type and the assigned dormitory.
     * Null for a student without a dormitory application (the server answers success with a null result).
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryStatus(): DormitoryStatus?

    /** GET /api/dormitory/payments/contracts/periods; academic years that have rental agreements, empty without any (a null result included). */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryContractPeriods(): List<DormitoryContractPeriod>

    /** GET /api/dormitory/payments/contracts; rental agreements of one period with balance and payment schedule.
     * Pass [DormitoryContractPeriod.dateFrom] and [DormitoryContractPeriod.dateTo]; they are sent as ISO
     * yyyy-MM-dd `from` and `to`. A period without agreements gives an empty list (a null result included).
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryContracts(dateFrom: LocalDate, dateTo: LocalDate): List<DormitoryContract>

    /** GET /api/dormitory/documents; documents ITMO needs before settlement and their status, empty without any. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDormitoryDocuments(): List<DormitoryDocument>
}

internal class DormitoryApiImpl(private val transport: ItmoTransport) : DormitoryApi {
    override suspend fun getDormitoryStatus(): DormitoryStatus? = transport.resultOrNull(
        DormitoryStatus.serializer(), HttpMethod.Get, "api/dormitory/status/full",
    )

    override suspend fun getDormitoryContractPeriods(): List<DormitoryContractPeriod> = transport.resultOrNull(
        ListSerializer(DormitoryContractPeriod.serializer()), HttpMethod.Get, "api/dormitory/payments/contracts/periods",
    ).orEmpty()

    override suspend fun getDormitoryContracts(dateFrom: LocalDate, dateTo: LocalDate): List<DormitoryContract> = transport.resultOrNull(
        ListSerializer(DormitoryContract.serializer()), HttpMethod.Get, "api/dormitory/payments/contracts",
    ) {
        parameter("from", dateFrom.toQueryValue())
        parameter("to", dateTo.toQueryValue())
    }.orEmpty()

    override suspend fun getDormitoryDocuments(): List<DormitoryDocument> = transport.result(
        ListSerializer(DormitoryDocument.serializer()), HttpMethod.Get, "api/dormitory/documents",
    )
}
