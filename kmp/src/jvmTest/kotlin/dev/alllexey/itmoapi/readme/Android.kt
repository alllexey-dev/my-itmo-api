package dev.alllexey.itmoapi.readme

// README android begin
import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.myitmo.schedule.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

// scope - viewModelScope или lifecycleScope: запросы не блокируют главный поток.
// itmo - один на ViewModel (MyItmo.fromStorage(...)), itmo.close() в onCleared().
fun loadWeek(
    scope: CoroutineScope,
    itmo: MyItmo,
    monday: LocalDate,
    show: (List<Schedule>) -> Unit,
    fail: (MyItmoException) -> Unit,
) {
    scope.launch {
        try {
            show(itmo.schedule.getPersonalSchedule(monday, monday.plus(6, DateTimeUnit.DAY)).requireResult())
        } catch (error: MyItmoException) {
            fail(error) // MyItmoException.Auth - нужен повторный вход
        }
    }
}
// README android end
