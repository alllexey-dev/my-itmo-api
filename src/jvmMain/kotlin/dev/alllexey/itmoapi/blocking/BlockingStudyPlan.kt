package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.studyplan.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [StudyPlanApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingStudyPlan internal constructor(private val api: StudyPlanApi) {
    /** Blocking [StudyPlanApi.getStudyPlanPrograms]. */
    @Throws(MyItmoException::class)
    public fun getStudyPlanPrograms(): StudyPlanPrograms = runBlocking { api.getStudyPlanPrograms() }

    /** Blocking [StudyPlanApi.getStudyPlan]. */
    @JvmOverloads
    @Throws(MyItmoException::class)
    public fun getStudyPlan(planId: Long, specializationId: Long? = null): StudyPlan =
        runBlocking { api.getStudyPlan(planId, specializationId) }
}
