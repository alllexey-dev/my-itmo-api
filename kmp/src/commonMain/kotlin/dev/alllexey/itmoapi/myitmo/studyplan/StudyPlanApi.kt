package dev.alllexey.itmoapi.myitmo.studyplan

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException

/** Read-only study-plan programs and complete recursive plan structures. */
public interface StudyPlanApi {
    /** GET /api/eduPlanNew/programs; returns available plans and the active educational program. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getStudyPlanPrograms(): ResultResponse<StudyPlanPrograms>

    /** GET /api/eduPlanNew/study_plan/{plan_id}; planId is StudyPlanProgram.planId.
     * spec_id is omitted when specializationId is null, as on programs without a specialization.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getStudyPlan(planId: Long, specializationId: Long? = null): ResultResponse<StudyPlan>
}

internal class StudyPlanApiImpl(private val transport: ItmoTransport) : StudyPlanApi {
    override suspend fun getStudyPlanPrograms(): ResultResponse<StudyPlanPrograms> = transport.execute(
        ResultResponse.serializer(StudyPlanPrograms.serializer()), HttpMethod.Get, "api/eduPlanNew/programs",
    ) { url { encodedPath = "/api/eduPlanNew/programs" } }

    override suspend fun getStudyPlan(planId: Long, specializationId: Long?): ResultResponse<StudyPlan> = transport.execute(
        ResultResponse.serializer(StudyPlan.serializer()), HttpMethod.Get, "api/eduPlanNew/study_plan/$planId",
    ) {
        url { encodedPath = "/api/eduPlanNew/study_plan/$planId" }
        if (specializationId != null) parameter("spec_id", specializationId)
    }
}
