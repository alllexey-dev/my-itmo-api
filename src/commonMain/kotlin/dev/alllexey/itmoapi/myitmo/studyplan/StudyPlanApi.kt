package dev.alllexey.itmoapi.myitmo.studyplan

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException

/** Read-only study-plan programs and complete recursive plan structures. */
public interface StudyPlanApi {
    /** GET /api/eduPlanNew/programs; returns available plans and the active educational program. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getStudyPlanPrograms(): StudyPlanPrograms

    /** GET /api/eduPlanNew/study_plan/{plan_id}; planId is StudyPlanProgram.planId.
     * spec_id is omitted when specializationId is null, as on programs without a specialization.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getStudyPlan(planId: Long, specializationId: Long? = null): StudyPlan
}

internal class StudyPlanApiImpl(private val transport: ItmoTransport) : StudyPlanApi {
    override suspend fun getStudyPlanPrograms(): StudyPlanPrograms = transport.result(
        StudyPlanPrograms.serializer(), HttpMethod.Get, "api/eduPlanNew/programs",
    )

    override suspend fun getStudyPlan(planId: Long, specializationId: Long?): StudyPlan = transport.result(
        StudyPlan.serializer(), HttpMethod.Get, "api/eduPlanNew/study_plan/$planId",
    ) {
        if (specializationId != null) parameter("spec_id", specializationId)
    }
}
