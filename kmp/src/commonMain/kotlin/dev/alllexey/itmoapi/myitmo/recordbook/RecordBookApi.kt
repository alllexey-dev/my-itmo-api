package dev.alllexey.itmoapi.myitmo.recordbook

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer

/** Read-only recordbook programs, discipline results and assessment trees. */
public interface RecordBookApi {
    /** GET /api/record_book/specializations; returns programs and their available semesters. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getSpecializations(): ResultResponse<List<Specialization>>

    /** GET /api/record_book/{specialization_id}/{semester}; specializationId is Specialization.mainPlan.
     * semester is the sequential semester number in that plan, starting at 1.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getRecordBook(specializationId: Long, semester: Int): ResultResponse<List<RecordBookEntry>>

    /** GET /api/record_book/{record_book_entry_id}; the identifier is RecordBookEntry.estId.
     * Returns the flat assessment tree linked through ControlEntry.parentId.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getControlEntries(recordBookEntryId: Long): ResultResponse<List<ControlEntry>>
}

internal class RecordBookApiImpl(private val transport: ItmoTransport) : RecordBookApi {
    override suspend fun getSpecializations(): ResultResponse<List<Specialization>> = transport.execute(
        ResultResponse.serializer(ListSerializer(Specialization.serializer())), HttpMethod.Get,
        "api/record_book/specializations",
    ) { url { encodedPath = "/api/record_book/specializations" } }

    override suspend fun getRecordBook(specializationId: Long, semester: Int): ResultResponse<List<RecordBookEntry>> =
        transport.execute(
            ResultResponse.serializer(ListSerializer(RecordBookEntry.serializer())), HttpMethod.Get,
            "api/record_book/$specializationId/$semester",
        ) { url { encodedPath = "/api/record_book/$specializationId/$semester" } }

    override suspend fun getControlEntries(recordBookEntryId: Long): ResultResponse<List<ControlEntry>> =
        transport.execute(
            ResultResponse.serializer(ListSerializer(ControlEntry.serializer())), HttpMethod.Get,
            "api/record_book/$recordBookEntryId",
        ) { url { encodedPath = "/api/record_book/$recordBookEntryId" } }
}
