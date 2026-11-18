package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.IdValuePair
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.sport.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toKotlinLocalDate

/** Blocking view of [SportApi] for Java: each method waits for the result on the calling thread.
 * Dates are `java.time.LocalDate`. Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingSport internal constructor(private val api: SportApi) {
    /** Blocking [SportApi.getSportTimeSlots]. */
    @Throws(MyItmoException::class)
    public fun getSportTimeSlots(): List<TimeSlot> = runBlocking { api.getSportTimeSlots() }

    /** Blocking [SportApi.getSportFilters]. */
    @Throws(MyItmoException::class)
    public fun getSportFilters(): SportFilters = runBlocking { api.getSportFilters() }

    /** Blocking [SportApi.getSportSchedule]. */
    @JvmOverloads
    @Throws(MyItmoException::class)
    public fun getSportSchedule(dateStart: LocalDate, dateEnd: LocalDate, buildingId: Long? = null, sportTypeIds: List<Long>? = null, teacherIsu: List<Long>? = null): List<SportSchedule> =
        runBlocking { api.getSportSchedule(dateStart.toKotlinLocalDate(), dateEnd.toKotlinLocalDate(), buildingId, sportTypeIds, teacherIsu) }

    /** Blocking [SportApi.getSportScore]. */
    @JvmOverloads
    @Throws(MyItmoException::class)
    public fun getSportScore(semesterId: Long? = null): SportScore = runBlocking { api.getSportScore(semesterId) }

    /** Blocking [SportApi.getSportAttempts]. */
    @Throws(MyItmoException::class)
    public fun getSportAttempts(): SportAttempts = runBlocking { api.getSportAttempts() }

    /** Blocking [SportApi.getSportSemesters]. */
    @Throws(MyItmoException::class)
    public fun getSportSemesters(): List<SportSemesterOption> = runBlocking { api.getSportSemesters() }

    /** Blocking [SportApi.getCurrentSportSemester]. */
    @Throws(MyItmoException::class)
    public fun getCurrentSportSemester(): SportSemester = runBlocking { api.getCurrentSportSemester() }

    /** Blocking [SportApi.getSportSignLimits]. */
    @Throws(MyItmoException::class)
    public fun getSportSignLimits(): Map<Long, Map<Long, SportSignLimit>> = runBlocking { api.getSportSignLimits() }

    /** Blocking [SportApi.getChosenSportSections]. */
    @Throws(MyItmoException::class)
    public fun getChosenSportSections(): List<ChosenSportSection> = runBlocking { api.getChosenSportSections() }

    /** Blocking [SportApi.signInLessons]. */
    @Throws(MyItmoException::class)
    public fun signInLessons(lessonIds: List<Long>): List<Long> = runBlocking { api.signInLessons(lessonIds) }

    /** Blocking [SportApi.signInLesson]. */
    public fun signInLesson(lessonId: Long): SportSignResult = runBlocking { api.signInLesson(lessonId) }

    /** Blocking [SportApi.signOutLessons]. */
    @Throws(MyItmoException::class)
    public fun signOutLessons(lessonIds: List<Long>): List<Long> = runBlocking { api.signOutLessons(lessonIds) }

    /** Blocking [SportApi.getSportTypes]. */
    @Throws(MyItmoException::class)
    public fun getSportTypes(): List<IdValuePair> = runBlocking { api.getSportTypes() }

    /** Blocking [SportApi.getSportSignAttempts]. */
    @Throws(MyItmoException::class)
    public fun getSportSignAttempts(): Int = runBlocking { api.getSportSignAttempts() }

    /** Blocking [SportApi.getPersonalSportCalendar]. */
    @Throws(MyItmoException::class)
    public fun getPersonalSportCalendar(dateStart: LocalDate, dateEnd: LocalDate): List<SportSchedule> =
        runBlocking { api.getPersonalSportCalendar(dateStart.toKotlinLocalDate(), dateEnd.toKotlinLocalDate()) }

    /** Blocking [SportApi.getSportDebt]. */
    @Throws(MyItmoException::class)
    public fun getSportDebt(): SportDebt = runBlocking { api.getSportDebt() }

    /** Blocking [SportApi.getSportExternat]. */
    @Throws(MyItmoException::class)
    public fun getSportExternat(): SportExternat = runBlocking { api.getSportExternat() }

    /** Blocking [SportApi.getSportHealthLevel]. */
    @Throws(MyItmoException::class)
    public fun getSportHealthLevel(): SportHealthLevelResponse = runBlocking { api.getSportHealthLevel() }

    /** Blocking [SportApi.getSportSelections]. */
    @Throws(MyItmoException::class)
    public fun getSportSelections(): List<SportSelection> = runBlocking { api.getSportSelections() }

    /** Blocking [SportApi.getSportProjects]. */
    @Throws(MyItmoException::class)
    public fun getSportProjects(): List<SportProject> = runBlocking { api.getSportProjects() }
}
