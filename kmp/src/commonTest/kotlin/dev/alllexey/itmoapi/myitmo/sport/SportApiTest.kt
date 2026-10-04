package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

class SportApiTest {
    private val dates = mapOf("date_start" to listOf("2026-01-05"), "date_end" to listOf("2026-01-11"))

    @Test
    fun timeSlotsKeepLocalIntervals() = runTest {
        val slot = sportExchange("time_slots", fixture("sport/time-slots.json")) {
            SportApiImpl(it).getSportTimeSlots().requireResult()
        }.single()
        assertEquals(1L, slot.id)
        assertEquals("08:20", slot.timeStart)
        assertEquals("09:50", slot.timeEnd)
    }

    @Test
    fun filtersKeepOtherVenueCategoryWithoutInventingCatalogEntries() = runTest {
        val filters = sportExchange("sign/schedule/filters", fixture("sport/filters.json")) {
            SportApiImpl(it).getSportFilters().requireResult()
        }
        assertEquals(0L, filters.buildingId.single().id)
        assertEquals(31L, filters.sectionId.single().id)
        assertEquals(2L, filters.sportTypeId.single().id)
        assertEquals(200002L, filters.teacherIsu.single().id)
    }

    @Test
    fun scheduleUsesInclusiveDatesAndRepeatedFiltersWithoutDeduplication() = runTest {
        val query = dates + mapOf("building_id" to listOf("335"), "sport_type_id" to listOf("3", "1", "3"), "teacher_isu" to listOf("200002", "200001"))
        val days = sportExchange("sign/schedule", fixture("sport/schedule.json"), query) {
            SportApiImpl(it).getSportSchedule(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11), 335, listOf(3, 1, 3), listOf(200002, 200001)).requireResult()
        }
        assertEquals(LocalDate(2026, 10, 5), days.first().date)
        assertNull(days.last().lessons)
        val lesson = days.first().lessons!!.first()
        assertEquals(Instant.parse("2026-10-05T07:00:00Z"), lesson.date)
        assertEquals(Instant.parse("2026-10-05T08:30:00Z"), lesson.dateEnd)
        assertEquals(500001L, lesson.id)
        assertEquals(13L, lesson.buildingId)
        assertEquals(20L, lesson.limit)
        assertEquals(3L, lesson.available)
        assertEquals(listOf("synthetic_reason"), lesson.canSignIn.unavailableReasons)
        assertFalse(lesson.canSignIn.canSignIn)
        val alternative = lesson.otherLessons.single()
        assertEquals(500002L, alternative.id)
        assertEquals(3, alternative.weekday)
        assertNull(alternative.evaluationId)
        assertNull(alternative.evaluationName)
        assertTrue(alternative.repeatable)
        assertTrue(days.first().lessons!!.last().signed)
    }

    @Test
    fun nullAndEmptyFiltersOmitParametersAndEmptyScheduleStaysEmpty() = runTest {
        for (filters in listOf(null, emptyList<Long>())) {
            assertTrue(sportExchange("sign/schedule", fixture("sport/empty.json"), dates) {
                SportApiImpl(it).getSportSchedule(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11), sportTypeIds = filters, teacherIsu = filters).requireResult()
            }.isEmpty())
        }
    }

    @Test
    fun scoreKeepsSumAndLessonCompetitionAwards() = runTest {
        val score = sportExchange("personal/score", fixture("sport/score.json"), mapOf("semester_id" to listOf("101"))) {
            SportApiImpl(it).getSportScore(101).requireResult()
        }
        assertEquals(2L, score.sum.attendances)
        assertEquals(3L, score.sum.other)
        val awards = score.attendances!!
        assertFalse(awards.first().isCompetition)
        assertNull(awards.first().competitionName)
        assertTrue(awards.last().isCompetition)
        assertEquals("Participation", awards.last().place)
        assertEquals(Instant.parse("2026-01-05T07:00:00Z"), awards.first().date)
    }

    @Test
    fun absentSemesterQueryAndNullAwardHistoryArePreserved() = runTest {
        val score = sportExchange("personal/score", fixture("sport/score-empty.json")) {
            SportApiImpl(it).getSportScore().requireResult()
        }
        assertNull(score.attendances)
        assertEquals(SportScore.Sum(), score.sum)
    }

    @Test
    fun attemptsKeepGrantedUsedRemainingAndServerPermission() = runTest {
        val attempts = sportExchange("personal/have_attempts", fixture("sport/attempts.json")) {
            SportApiImpl(it).getSportAttempts().requireResult()
        }
        assertEquals(SportAttempts(4, 1, 3, true), attempts)
    }

    @Test
    fun semestersKeepSelectionLabelAndOptionalComment() = runTest {
        val semester = sportExchange("semesters/list", fixture("sport/semesters.json")) {
            SportApiImpl(it).getSportSemesters().requireResult()
        }.single()
        assertEquals(101L, semester.id)
        assertEquals("Spring 2025/2026", semester.value)
        assertNull(semester.comment)
    }

    @Test
    fun currentSemesterKeepsControlPeriodsAndTechnicalMinimumDates() = runTest {
        val semester = sportExchange("semesters/current", fixture("sport/current-semester.json")) {
            SportApiImpl(it).getCurrentSportSemester().requireResult()
        }
        assertEquals("2025/2026", semester.studyYear)
        assertEquals(0, semester.semester)
        assertTrue(semester.current)
        assertEquals(7, semester.signDuration)
        assertEquals(Instant.parse("0000-12-31T21:00:00Z"), semester.choiceStart)
        assertEquals(semester.choiceStart, semester.bachelorBound)
        assertTrue(semester.dateEnd < semester.hardDateEnd)
        assertTrue(semester.ppa1Start < semester.ppa1End)
        assertTrue(semester.ppa2Start < semester.ppa2End)
    }

    @Test
    fun limitsKeepBothNumericMapKeyLevelsAndEmptyRestrictions() = runTest {
        val limits = sportExchange("sign/schedule/limits", fixture("sport/limits.json")) {
            SportApiImpl(it).getSportSignLimits().requireResult()
        }
        assertEquals(setOf(1L, 2L), limits.keys)
        assertEquals(SportSignLimit(4, 2), limits.getValue(1).getValue(10))
        assertEquals(SportSignLimit(2, 0), limits.getValue(1).getValue(11))
        assertEquals(SportSignLimit(1, 1), limits.getValue(2).getValue(20))
        assertTrue(sportExchange("sign/schedule/limits", fixture("sport/limits-empty.json")) {
            SportApiImpl(it).getSportSignLimits().requireResult()
        }.isEmpty())
    }

    @Test
    fun chosenSectionsKeepGroupsConcreteLessonsAndWeekdaySlots() = runTest {
        val section = sportExchange("sign/chosen", fixture("sport/chosen.json")) {
            SportApiImpl(it).getChosenSportSections().requireResult()
        }.single()
        assertEquals(31L, section.id)
        val group = section.lessonGroups.single()
        assertEquals(2, group.level)
        assertTrue(group.hasFutureLessons)
        assertEquals(500001L, group.lessons.single().id)
        assertEquals(Instant.parse("2026-01-05T07:00:00Z"), group.lessons.single().dateStart)
        assertEquals("https://example.invalid/lesson", group.lessons.single().linkUrl)
        val weekday = group.weekdays.single()
        assertEquals("Monday", weekday.weekday)
        assertNull(weekday.dateStart)
        assertNull(weekday.dateEnd)
        assertEquals(-1L, weekday.roomId)
    }

    @Test
    fun enrollmentPostsAndWithdrawalDeletesJsonIdArray() = runTest {
        for (method in listOf(HttpMethod.Post, HttpMethod.Delete)) {
            val result = sportExchange("sign/schedule/lessons", fixture("sport/lessons.json"), method = method, body = "[500001,500003]") {
                val api = SportApiImpl(it)
                if (method == HttpMethod.Post) api.signInLessons(listOf(500001, 500003)).requireResult()
                else api.signOutLessons(listOf(500001, 500003)).requireResult()
            }
            assertEquals(listOf(500001L, 500003L), result)
            assertTrue(sportExchange("sign/schedule/lessons", fixture("sport/empty.json"), method = method, body = "[]") {
                val api = SportApiImpl(it)
                if (method == HttpMethod.Post) api.signInLessons(emptyList()).requireResult()
                else api.signOutLessons(emptyList()).requireResult()
            }.isEmpty())
        }
    }

    @Test
    fun enrollment137AndWithdrawal130RetainAllServerReasons() = runTest {
        for ((method, code, name) in listOf(Triple(HttpMethod.Post, 137, "sign-in-error"), Triple(HttpMethod.Delete, 130, "sign-out-error"))) {
            for (status in listOf(200, 400)) {
                val failure = assertFailsWith<MyItmoException.Api> {
                    sportExchange("sign/schedule/lessons", fixture("sport/$name.json"), method = method, body = "[500001]", status = status) {
                        if (method == HttpMethod.Post) SportApiImpl(it).signInLessons(listOf(500001))
                        else SportApiImpl(it).signOutLessons(listOf(500001))
                    }
                }
                assertEquals(code, failure.errorCode)
                assertEquals(status, failure.status)
                val expected = if (code == 137) "[cannot enroll student: [daily limit], [overlapping enrollment], [weekly limit]]" else "[cannot withdraw student: [not enrolled]]"
                assertEquals(expected, failure.serverMessage)
            }
        }
    }
}
