package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.studyplan.*
import kotlin.test.Test
import kotlin.test.assertNotEquals
import dev.alllexey.itmoapi.core.ItmoApiJson

class StudyPlanParityTest {
    @Test
    fun `programs wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.studyplan.StudyPlanPrograms>, ResultResponse<StudyPlanPrograms>>(
            "studyplan/programs.json",
            ResultResponse.serializer(StudyPlanPrograms.serializer()),
        )
    }

    @Test
    fun `empty-programs wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.studyplan.StudyPlanPrograms>, ResultResponse<StudyPlanPrograms>>(
            "studyplan/empty-programs.json",
            ResultResponse.serializer(StudyPlanPrograms.serializer()),
        )
    }

    @Test
    fun `study-plan wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.studyplan.StudyPlan>, ResultResponse<StudyPlan>>(
            "studyplan/study-plan.json",
            ResultResponse.serializer(StudyPlan.serializer()),
        )
    }

    @Test
    fun `absence wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.studyplan.StudyPlan>, ResultResponse<StudyPlan>>(
            "studyplan/absence.json",
            ResultResponse.serializer(StudyPlan.serializer()),
        )
    }
    @Test
    fun `reviewed fallbacks do not hide changed values or extra fields`() {
        val absent = ItmoApiJson.parseToJsonElement("""{"result":{"structure":[{}]}}""")
        val changed = ItmoApiJson.parseToJsonElement("""{"result":{"structure":[{"rules":[17]}]}}""")
        val unexpected = ItmoApiJson.parseToJsonElement("""{"result":{"structure":[{"unexpected":0}]}}""")
        val changedInfo = ItmoApiJson.parseToJsonElement("""{"result":{"structure":[{}],"planInfo":{"directionCode":"changed","directionName":"","levelQualification":"","planType":"","programName":"","startYear":0}}}""")
        val normalized = IntendedDifferences.normalize("studyplan/absence.json", absent)
        for (drift in listOf(changed, unexpected, changedInfo)) {
            assertNotEquals(normalized, IntendedDifferences.normalize("studyplan/absence.json", drift))
        }
    }
}
