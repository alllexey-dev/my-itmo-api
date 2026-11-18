package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.schedule.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDate

/** Blocking view of [ScheduleApi] for Java: each method waits for the result on the calling thread.
 * Dates are `java.time.LocalDate`. Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingSchedule internal constructor(private val api: ScheduleApi) {
    /** Blocking [ScheduleApi.getPersonalSchedule]. */
    @Throws(MyItmoException::class)
    public fun getPersonalSchedule(dateStart: LocalDate, dateEnd: LocalDate): List<Schedule> =
        runBlocking { api.getPersonalSchedule(dateStart.toKotlinLocalDate(), dateEnd.toKotlinLocalDate()) }

    /** Blocking [ScheduleApi.getTeacherSchedule]. */
    @Throws(MyItmoException::class)
    public fun getTeacherSchedule(isu: Long, dateStart: LocalDate, dateEnd: LocalDate): List<Schedule> =
        runBlocking { api.getTeacherSchedule(isu, dateStart.toKotlinLocalDate(), dateEnd.toKotlinLocalDate()) }

    /** Blocking [ScheduleApi.getTimeSlots]. */
    @Throws(MyItmoException::class)
    public fun getTimeSlots(): List<ExtendedTimeSlot> = runBlocking { api.getTimeSlots() }
}
