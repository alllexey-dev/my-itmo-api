package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.election.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [ElectionApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingElection internal constructor(private val api: ElectionApi) {
    /** Blocking [ElectionApi.getElectionAvailability]. */
    @Throws(MyItmoException::class)
    public fun getElectionAvailability(): ElectionAvailability = runBlocking { api.getElectionAvailability() }

    /** Blocking [ElectionApi.getAvailableDisciplines]. */
    @Throws(MyItmoException::class)
    public fun getAvailableDisciplines(): List<AvailableDiscipline> = runBlocking { api.getAvailableDisciplines() }

    /** Blocking [ElectionApi.validateSelectedDisciplines]. */
    @Throws(MyItmoException::class)
    public fun validateSelectedDisciplines(groupFlowIds: List<String>): DisciplineSelectionValidation =
        runBlocking { api.validateSelectedDisciplines(groupFlowIds) }

    /** Blocking [ElectionApi.getFlowLimits]. */
    @Throws(MyItmoException::class)
    public fun getFlowLimits(): Map<String, FlowLimit> = runBlocking { api.getFlowLimits() }

    /** Blocking [ElectionApi.getOrderedFlowChains]. */
    @Throws(MyItmoException::class)
    public fun getOrderedFlowChains(): List<ElectionFlowChain> = runBlocking { api.getOrderedFlowChains() }

    /** Blocking [ElectionApi.getChosenFlows]. */
    @Throws(MyItmoException::class)
    public fun getChosenFlows(): List<Long> = runBlocking { api.getChosenFlows() }

    /** Blocking [ElectionApi.changeSelectedFlows]. */
    @Throws(MyItmoException::class)
    public fun changeSelectedFlows(flowIds: List<Long>): Unit = runBlocking { api.changeSelectedFlows(flowIds) }

    /** Blocking [ElectionApi.clearAllSelectedFlows]. */
    @Throws(MyItmoException::class)
    public fun clearAllSelectedFlows(): Unit = runBlocking { api.clearAllSelectedFlows() }

    /** Blocking [ElectionApi.changeSelectedDisciplines]. */
    @Throws(MyItmoException::class)
    public fun changeSelectedDisciplines(groupFlowIds: List<String>): Unit =
        runBlocking { api.changeSelectedDisciplines(groupFlowIds) }
}
