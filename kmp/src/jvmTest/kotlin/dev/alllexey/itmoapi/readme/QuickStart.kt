package dev.alllexey.itmoapi.readme

// README quickstart begin
import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MOSCOW
import dev.alllexey.itmoapi.core.requireResult
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

suspend fun main() {
    MyItmo.login("my_cool_id", "my_strong_password").use { itmo ->
        val today = Clock.System.todayIn(MOSCOW)
        val week = itmo.schedule.getPersonalSchedule(today, today.plus(6, DateTimeUnit.DAY)).requireResult()
        for (day in week) {
            println("${day.date}: " + day.lessons.joinToString { "${it.timeStart} ${it.subject}" })
        }
    }
}
// README quickstart end
