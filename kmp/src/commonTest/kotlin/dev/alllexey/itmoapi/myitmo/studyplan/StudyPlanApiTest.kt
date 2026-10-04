package dev.alllexey.itmoapi.myitmo.studyplan

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.myitmo.recordbook.recordBookExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyPlanApiTest {
    @Test
    fun programsDecodePlanIdsAndNullableSpecialization() = runTest {
        val result = recordBookExchange("/api/eduPlanNew/programs", fixture("studyplan/programs.json")) {
            StudyPlanApiImpl(it).getStudyPlanPrograms().requireResult()
        }
        assertEquals(100001L, result.isu)
        val active = result.programs.first()
        assertEquals(50001L, active.planId)
        assertEquals(50011L, active.specializationId)
        assertEquals("Синтетическая программа", active.name)
        assertTrue(active.isActive)
        val inactive = result.programs.last()
        assertEquals(50002L, inactive.planId)
        assertNull(inactive.specializationId)
        assertFalse(inactive.isActive)
    }

    @Test
    fun studyPlanSendsSpecializationAndDecodesAllNineModels() = runTest {
        val plan = recordBookExchange(
            "/api/eduPlanNew/study_plan/50001", fixture("studyplan/study-plan.json"),
            query = mapOf("spec_id" to listOf("50011")),
        ) { StudyPlanApiImpl(it).getStudyPlan(50001L, 50011L).requireResult() }
        assertEquals(50001L, plan.id)
        assertEquals(3, plan.currentSemester)
        assertEquals(77003L, plan.currentSemesterId)
        assertEquals(8, plan.semestersCount)
        assertEquals(
            StudyPlanInfo("09.03.04", "Синтетическое направление", "Бакалавр", "Синтетический тип", "Синтетическая программа", 2024),
            plan.planInfo,
        )
        assertEquals(
            listOf(StudyPlanSemester(3, 77003L, 1, "2025/2026"), StudyPlanSemester(4, 77004L, 0, "2025/2026")),
            plan.semesters,
        )
        val block = plan.structure.single()
        assertEquals(73001L, block.id)
        assertEquals("block", block.type)
        val module = block.children.single()
        assertEquals(72001L, module.id)
        assertEquals("module", module.type)
        val discipline = module.children.first()
        assertEquals(70001L, discipline.id)
        assertEquals("Синтетическая дисциплина", discipline.name)
        assertEquals("discipline", discipline.type)
        assertEquals(72001L, discipline.moduleId)
        assertEquals(73001L, discipline.blockId)
        assertEquals("Синтетический блок", discipline.blockName)
        assertEquals(74001L, discipline.choiceParameterId)
        assertEquals("Синтетический выбор", discipline.choiceParameterName)
        assertEquals(true, discipline.choiceAvailable)
        assertEquals(false, discipline.flowSelectable)
        assertEquals(true, discipline.replaceable)
        assertEquals(false, discipline.startSemesterSelectable)
        assertEquals(3, discipline.creditPoints)
        assertEquals(1, discipline.disciplineDuration)
        assertEquals("Синтетическое описание", discipline.description)
        assertEquals("ru", discipline.langCode)
        assertEquals("Русский", discipline.langName)
        assertEquals("https://example.invalid/syllabus/70001", discipline.rpdUrl)
        assertEquals(StudyPlanDepartment(75001L, "Синтетический факультет", "СФ"), discipline.department)
        assertEquals(listOf(76001L, 76002L), discipline.rules)
        assertTrue(discipline.children.isEmpty())
        assertEquals(setOf("3", "4"), discipline.contents.keys)
        val content = discipline.contents.getValue("3").single()
        assertEquals(90001L, content.id)
        assertEquals(72001L, content.moduleId)
        assertEquals(70001L, content.disciplineId)
        assertEquals(1, content.order)
        assertEquals(3, content.semester)
        assertEquals(3, content.creditPoints)
        assertEquals(StudyPlanActivity(91001L, 90001L, "Лекции", 32.0, 1L), content.activities.first())
        assertEquals(listOf(1L, 3L, 4L, 5L), content.activities.map { it.workTypeId })
        assertNull(content.activities.last().volume)
        assertEquals(6L, discipline.contents.getValue("4").single().activities.single().workTypeId)
        val minimal = module.children.last()
        assertEquals(70002L, minimal.id)
        assertNull(minimal.creditPoints)
        assertTrue(minimal.contents.isEmpty())
    }

    @Test
    fun noSpecializationOmitsQueryAndPreservesAbsentNodeFields() = runTest {
        val plan = recordBookExchange("/api/eduPlanNew/study_plan/50002", fixture("studyplan/absence.json")) {
            StudyPlanApiImpl(it).getStudyPlan(50002L).requireResult()
        }
        assertEquals(50002L, plan.id)
        assertEquals(StudyPlanInfo(), plan.planInfo)
        assertTrue(plan.semesters.isEmpty())
        assertEquals(
            StudyPlanNode(id = 70002L, name = "Синтетическая дисциплина без деталей", type = "discipline"),
            plan.structure.single(),
        )
    }

    @Test
    fun explicitNullSpecializationAlsoOmitsQuery() = runTest {
        val plan = recordBookExchange("/api/eduPlanNew/study_plan/50002", fixture("studyplan/absence.json")) {
            StudyPlanApiImpl(it).getStudyPlan(50002L, null).requireResult()
        }
        assertEquals(50002L, plan.id)
    }

    @Test
    fun emptyProgramsRemainEmpty() = runTest {
        val result = recordBookExchange("/api/eduPlanNew/programs", fixture("studyplan/empty-programs.json")) {
            StudyPlanApiImpl(it).getStudyPlanPrograms().requireResult()
        }
        assertEquals(100001L, result.isu)
        assertTrue(result.programs.isEmpty())
    }

    @Test
    fun numericApiFailureIsMappedBeforeReturningStudyPlan() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            recordBookExchange("/api/eduPlanNew/study_plan/50001", fixture("errors/error-envelope-400.json"), status = 400) {
                StudyPlanApiImpl(it).getStudyPlan(50001L)
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
    }
}
