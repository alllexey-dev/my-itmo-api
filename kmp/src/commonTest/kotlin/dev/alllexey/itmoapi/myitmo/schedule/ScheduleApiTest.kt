package dev.alllexey.itmoapi.myitmo.schedule

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.myitmo.qr.areaExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleApiTest {
    private val dates = mapOf("date_start" to listOf("2026-01-05"), "date_end" to listOf("2026-01-11"))

    @Test
    fun personalScheduleUsesExplicitZeroPaddedDateQueryAndDataEnvelope() = runTest {
        val days = areaExchange("/api/schedule/schedule/personal", fixture("schedule/personal.json"), dates) {
            ScheduleApiImpl(it).getPersonalSchedule(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11)).requireResult()
        }
        val day = days.single()
        assertEquals(1, day.dayNumber)
        assertEquals(6, day.weekNumber)
        assertEquals(LocalDate(2026, 10, 5), day.date)
        assertNull(day.note)
        val lesson = day.lessons.first()
        assertEquals(9000001L, lesson.pairId)
        assertEquals("Тестовая дисциплина", lesson.subject)
        assertEquals(70001L, lesson.subjectId)
        assertNull(lesson.note)
        assertEquals("Лекция", lesson.type)
        assertEquals("08:20", lesson.timeStart)
        assertEquals("09:50", lesson.timeEnd)
        assertEquals(200001L, lesson.teacherId)
        assertEquals("Преподаватель Тестовый", lesson.teacherName)
        assertEquals("1404", lesson.room)
        assertEquals("Кронверкский пр., д.49, лит.А", lesson.building)
        assertEquals("Очно", lesson.format)
        assertEquals("Лекции", lesson.workType)
        assertEquals(1, lesson.workTypeId)
        assertEquals("T3100", lesson.group)
        assertEquals(2, lesson.flowTypeId)
        assertEquals(80001, lesson.flowId)
        assertNull(lesson.zoomUrl)
        assertNull(lesson.zoomPassword)
        assertNull(lesson.zoomInfo)
        assertEquals(13, lesson.bldId)
        assertEquals(1, lesson.formatId)
        assertEquals(13, lesson.mainBldId)
        val remote = day.lessons.last()
        assertNull(remote.subject)
        assertNull(remote.teacherId)
        assertNull(remote.teacherName)
        assertNull(remote.bldId)
        assertNull(remote.mainBldId)
        assertEquals("https://example.invalid/meeting/1", remote.zoomUrl)
        assertEquals(3, remote.formatId)
    }

    @Test
    fun timeSlotsDecodeFlattenedLegacyFieldsAndOrder() = runTest {
        val slots = areaExchange("/api/schedule/meta/time_slots", fixture("schedule/time-slots.json")) {
            ScheduleApiImpl(it).getTimeSlots().requireResult()
        }
        assertEquals(listOf(1, 2), slots.map { it.order })
        assertEquals(1L, slots.first().id)
        assertEquals("08:20", slots.first().timeStart)
        assertEquals("09:50", slots.first().timeEnd)
    }

    @Test
    fun emptyPersonalScheduleStaysEmpty() = runTest {
        val days = areaExchange("/api/schedule/schedule/personal", fixture("schedule/empty.json"), dates) {
            ScheduleApiImpl(it).getPersonalSchedule(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11)).requireResult()
        }
        assertTrue(days.isEmpty())
    }

    @Test
    fun scheduleCodeMapsToTypedApiFailureEvenWithHttp200() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            areaExchange("/api/schedule/meta/time_slots", fixture("schedule/error.json")) {
                ScheduleApiImpl(it).getTimeSlots()
            }
        }
        assertEquals(200, failure.status)
        assertEquals(7, failure.errorCode)
        assertEquals("Synthetic schedule error", failure.serverMessage)
    }
}
