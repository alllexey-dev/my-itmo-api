package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.bars.model.Discipline
import dev.alllexey.itmoapi.bars.model.GroupOrFlow
import dev.alllexey.itmoapi.bars.model.Setting
import dev.alllexey.itmoapi.bars.model.StudentJournal
import dev.alllexey.itmoapi.bars.model.Term
import dev.alllexey.itmoapi.bars.model.User
import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.runBlocking
import java.util.function.Function

/** Blocking view of [BarsClient] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the client method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingBars internal constructor(private val client: BarsClient) {
    /** Blocking [BarsClient.login]. */
    @Throws(MyItmoException::class)
    public fun login(code: String): Unit = runBlocking { client.login(code) }

    /** Blocking [BarsClient.hasSession]. */
    @Throws(MyItmoException::class)
    public fun hasSession(): Boolean = runBlocking { client.hasSession() }

    /** Blocking [BarsClient.logout]. */
    @Throws(MyItmoException::class)
    public fun logout(): Unit = runBlocking { client.logout() }

    /** Blocking [BarsClient.getCurrentUser]. */
    @Throws(MyItmoException::class)
    public fun getCurrentUser(): User = runBlocking { client.getCurrentUser() }

    /** Blocking [BarsClient.getConfig]. */
    @Throws(MyItmoException::class)
    public fun getConfig(): List<Setting> = runBlocking { client.getConfig() }

    /** Blocking [BarsClient.setPersonalSetting]. */
    @Throws(MyItmoException::class)
    public fun setPersonalSetting(setting: Setting): Setting = runBlocking { client.setPersonalSetting(setting) }

    /** Blocking [BarsClient.getDisciplines]. */
    @JvmOverloads
    @Throws(MyItmoException::class)
    public fun getDisciplines(withCheckpointPlansOnly: Boolean? = null): List<Discipline> =
        runBlocking { client.getDisciplines(withCheckpointPlansOnly) }

    /** Blocking [BarsClient.getGroupsAndFlows]. */
    @JvmOverloads
    @Throws(MyItmoException::class)
    public fun getGroupsAndFlows(disciplineId: Long? = null): List<GroupOrFlow> =
        runBlocking { client.getGroupsAndFlows(disciplineId) }

    /** Blocking [BarsClient.getStudentJournal]. */
    @Throws(MyItmoException::class)
    public fun getStudentJournal(checkpointPlanId: Long, type: String, identifier: String): StudentJournal =
        runBlocking { client.getStudentJournal(checkpointPlanId, type, identifier) }

    /** Blocking [BarsClient.selectPeriod]. */
    @Throws(MyItmoException::class)
    public fun selectPeriod(year: String, term: Term): User = runBlocking { client.selectPeriod(year, term) }

    /** Blocking [BarsClient.withPeriod]: selects the period, then runs [action] with this view.
     * As there, do not change the period inside [action].
     */
    @Throws(MyItmoException::class)
    public fun <T> withPeriod(year: String, term: Term, action: Function<BlockingBars, T>): T =
        runBlocking { client.withPeriod(year, term) { action.apply(this@BlockingBars) } }
}
