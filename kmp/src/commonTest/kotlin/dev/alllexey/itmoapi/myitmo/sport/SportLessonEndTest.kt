package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.*
import kotlin.time.Instant

class SportLessonEndTest {
    @Test
    fun weeklySeriesWithEndTwoDaysEarlierEndsOnItsStartDateAtTheSlotEnd() {
        val lessons = ItmoApiJson.decodeFromString(ResultResponse.serializer(ListSerializer(SportSchedule.serializer())), fixture("sport/wrong-end-date.json"))
            .requireResult().flatMap { it.lessons.orEmpty() }
        assertEquals(
            listOf(Instant.parse("2026-10-15T21:00:00+03:00"), Instant.parse("2026-10-22T21:00:00+03:00")),
            lessons.map { it.normalizedDateEnd },
        )
        assertEquals(Instant.parse("2026-10-13T21:00:00+03:00"), lessons.first().dateEnd, "the raw value is kept")
    }

    @Test
    fun consistentLessonIsUnchanged() {
        val lesson = lesson(start = "2026-10-05T10:00:00+03:00", end = "2026-10-05T11:30:00+03:00", slotEnd = "11:45")
        assertEquals(lesson.dateEnd, lesson.normalizedDateEnd)
    }

    @Test
    fun unparsableSlotEndFallsBackToTheRawTimeOfDay() {
        val lesson = lesson(start = "2026-10-15T19:00:00+03:00", end = "2026-10-13T20:30:00+03:00", slotEnd = "")
        assertEquals(Instant.parse("2026-10-15T20:30:00+03:00"), lesson.normalizedDateEnd)
    }

    @Test
    fun overnightOrInsaneEndHasNoNormalizedEnd() {
        assertNull(lesson(start = "2026-10-15T23:00:00+03:00", end = "2026-10-16T01:00:00+03:00", slotEnd = "01:00").normalizedDateEnd)
        assertNull(lesson(start = "2026-10-15T19:00:00+03:00", end = "2026-10-13T19:05:00+03:00", slotEnd = "19:05").normalizedDateEnd)
        assertNull(lesson(start = "2026-10-15T08:00:00+03:00", end = "2026-10-13T21:00:00+03:00", slotEnd = "21:00").normalizedDateEnd)
        assertNull(lesson(start = "2026-10-15T19:00:00+03:00", end = "2026-10-15T19:00:00+03:00", slotEnd = "19:00").normalizedDateEnd)
    }

    @Test
    fun dayIsTheMoscowDateNotTheUtcDate() {
        // 00:30 Moscow is 21:30 UTC of the previous day.
        val consistent = lesson(start = "2026-10-15T21:30:00Z", end = "2026-10-15T23:00:00Z", slotEnd = "02:00")
        assertEquals(consistent.dateEnd, consistent.normalizedDateEnd)
        val wrongDate = lesson(start = "2026-10-15T21:30:00Z", end = "2026-10-13T23:00:00Z", slotEnd = "02:00")
        assertEquals(Instant.parse("2026-10-16T02:00:00+03:00"), wrongDate.normalizedDateEnd)
    }

    @Test
    fun chosenLessonUsesItsTimeEndAndNeedsBothDates() {
        val chosen = ChosenSportLesson(
            dateStart = Instant.parse("2026-10-15T19:00:00+03:00"),
            dateEnd = Instant.parse("2026-10-13T21:00:00+03:00"),
            timeEnd = "21:00",
        )
        assertEquals(Instant.parse("2026-10-15T21:00:00+03:00"), chosen.normalizedDateEnd)
        assertNull(chosen.copy(dateStart = null).normalizedDateEnd)
        assertNull(chosen.copy(dateEnd = null).normalizedDateEnd)
    }

    private fun lesson(start: String, end: String, slotEnd: String) =
        SportLesson(date = Instant.parse(start), dateEnd = Instant.parse(end), timeSlotEnd = slotEnd)
}
