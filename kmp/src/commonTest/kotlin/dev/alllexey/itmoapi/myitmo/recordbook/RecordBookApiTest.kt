package dev.alllexey.itmoapi.myitmo.recordbook

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class RecordBookApiTest {
    @Test
    fun specializationsDecodeProgramAndAcademicPeriods() = runTest {
        val programs = recordBookExchange("/api/record_book/specializations", fixture("recordbook/specializations.json")) {
            RecordBookApiImpl(it).getSpecializations().requireResult()
        }
        val program = programs.single()
        assertEquals(50001L, program.mainPlan)
        assertEquals("Синтетическая программа", program.specializationName)
        assertEquals(listOf(3, 2), program.semesters.map { it.semester })
        assertEquals("2025/2026", program.semesters.first().studyYear)
        assertEquals(2, program.semesters.first().course)
        assertTrue(program.semesters.first().actual)
        assertFalse(program.semesters.last().actual)
    }

    @Test
    fun recordBookUsesPlanAndSemesterAndPreservesPointsGradesAndNames() = runTest {
        val entries = recordBookExchange("/api/record_book/50001/3", fixture("recordbook/record-book.json")) {
            RecordBookApiImpl(it).getRecordBook(50001L, 3).requireResult()
        }
        val graded = entries.first()
        assertEquals(" Тестовая дисциплина ", graded.name)
        assertEquals(70001L, graded.disciplineId)
        assertEquals(60001L, graded.estId)
        assertEquals(71.5, graded.currentScore)
        assertEquals("4/C", graded.rate)
        assertEquals(1, graded.attempt)
        assertEquals("Экзамен", graded.controlType)
        assertEquals(2L, graded.controlTypeId)
        assertEquals(Instant.parse("2026-01-15T07:00:00Z"), graded.examDate)
        assertTrue(graded.haveTree)
        assertNull(graded.lmsLink)
        assertEquals(RecordBookTeacher("Тестовый", "Преподаватель", "Синтетический"), graded.teacher)
        val ungraded = entries.last()
        assertNull(ungraded.currentScore)
        assertNull(ungraded.rate)
        assertEquals(0, ungraded.attempt)
        assertEquals("Зачёт", ungraded.controlType)
        assertEquals(1L, ungraded.controlTypeId)
        assertFalse(ungraded.haveTree)
        assertNull(ungraded.examDate)
        assertNull(ungraded.lmsLink)
        assertNull(ungraded.teacher)
    }

    @Test
    fun controlsUseEntryIdentifierAndDecodeFlatTreeAndPartialTeacher() = runTest {
        val controls = recordBookExchange("/api/record_book/60001", fixture("recordbook/controls.json")) {
            RecordBookApiImpl(it).getControlEntries(60001L).requireResult()
        }
        val root = controls.first()
        assertEquals(81001L, root.id)
        assertEquals("Синтетический контроль", root.controlName)
        assertNull(root.parentId)
        assertEquals(0.0, root.lowerValue)
        assertEquals(100.0, root.maxValue)
        assertEquals(60.0, root.minValue)
        assertTrue(root.required)
        assertEquals(71.5, root.rate)
        assertEquals(Instant.parse("2026-01-15T07:00:00Z"), root.date)
        assertEquals(RecordBookTeacher("Тестовый", "Преподаватель", "Синтетический"), root.teacher)
        val child = controls.last()
        assertEquals(81002L, child.id)
        assertEquals(81001L, child.parentId)
        assertNull(child.lowerValue)
        assertEquals(20.0, child.maxValue)
        assertEquals(0.0, child.minValue)
        assertFalse(child.required)
        assertNull(child.rate)
        assertNull(child.date)
        assertEquals(RecordBookTeacher(surname = "Тестовый"), child.teacher)
    }

    @Test
    fun missingResultsRemainAbsentRatherThanZero() = runTest {
        val entry = recordBookExchange("/api/record_book/50001/4", fixture("recordbook/absence.json")) {
            RecordBookApiImpl(it).getRecordBook(50001L, 4).requireResult().single()
        }
        assertNull(entry.currentScore)
        assertNull(entry.rate)
        assertNull(entry.examDate)
        assertNull(entry.teacher)
        assertNull(entry.lmsLink)
    }

    @Test
    fun emptyCollectionsRemainEmptyForAllRecordBookEndpoints() = runTest {
        val response = fixture("recordbook/empty.json")
        assertTrue(recordBookExchange("/api/record_book/specializations", response) {
            RecordBookApiImpl(it).getSpecializations().requireResult()
        }.isEmpty())
        assertTrue(recordBookExchange("/api/record_book/50001/3", response) {
            RecordBookApiImpl(it).getRecordBook(50001L, 3).requireResult()
        }.isEmpty())
        assertTrue(recordBookExchange("/api/record_book/60001", response) {
            RecordBookApiImpl(it).getControlEntries(60001L).requireResult()
        }.isEmpty())
    }

    @Test
    fun numericApiFailureIsMappedBeforeReturningRecordBook() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            recordBookExchange("/api/record_book/50001/3", fixture("errors/error-envelope-400.json"), status = 400) {
                RecordBookApiImpl(it).getRecordBook(50001L, 3)
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
    }
}
