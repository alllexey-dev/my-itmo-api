package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.recordbook.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [RecordBookApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingRecordBook internal constructor(private val api: RecordBookApi) {
    /** Blocking [RecordBookApi.getSpecializations]. */
    @Throws(MyItmoException::class)
    public fun getSpecializations(): List<Specialization> = runBlocking { api.getSpecializations() }

    /** Blocking [RecordBookApi.getRecordBook]. */
    @Throws(MyItmoException::class)
    public fun getRecordBook(specializationId: Long, semester: Int): List<RecordBookEntry> =
        runBlocking { api.getRecordBook(specializationId, semester) }

    /** Blocking [RecordBookApi.getControlEntries]. */
    @Throws(MyItmoException::class)
    public fun getControlEntries(recordBookEntryId: Long): List<ControlEntry> =
        runBlocking { api.getControlEntries(recordBookEntryId) }
}
