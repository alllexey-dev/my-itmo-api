package dev.alllexey.itmoapi.myitmo.schedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Lesson in the personal academic timetable. Nullable properties have observed null/absence.
 * Times are local HH:mm strings, not instants; IDs are raw university identifiers. */
@Serializable
public data class Lesson(
    /** Unique scheduled pair identifier. */
    @SerialName("pair_id") public val pairId: Long = 0,
    /** Subject display name; may be null. */
    public val subject: String? = null,
    /** Internal subject identifier. */
    @SerialName("subject_id") public val subjectId: Long = 0,
    /** Additional note, usually null. */
    public val note: String? = null,
    /** Server lesson type label, e.g. lecture or practice. */
    public val type: String = "",
    /** Local start time in HH:mm. */
    @SerialName("time_start") public val timeStart: String = "",
    /** Local end time in HH:mm. */
    @SerialName("time_end") public val timeEnd: String = "",
    /** Teacher ISU; may be absent. */
    @SerialName("teacher_id") public val teacherId: Long? = null,
    /** Teacher full name; may be absent. */
    @SerialName("teacher_name") public val teacherName: String? = null,
    /** Room label; may be null. */
    public val room: String? = null,
    /** Building address/label; may be null. */
    public val building: String? = null,
    /** Format label: in-person, mixed or remote. */
    public val format: String = "",
    /** Work label, e.g. lecture, practice or laboratory. */
    @SerialName("work_type") public val workType: String = "",
    /** Observed IDs: 1 lecture, 2 laboratory, 3 practice, 5 exam, 6 credit, 10 consultation, 11 sport. */
    @SerialName("work_type_id") public val workTypeId: Int = 0,
    /** Academic group label. */
    public val group: String = "",
    /** Observed flow types: 2 academic lessons, 3 sport, 5 room reservations. */
    @SerialName("flow_type_id") public val flowTypeId: Int = 0,
    /** Raw flow identifier. */
    @SerialName("flow_id") public val flowId: Int = 0,
    /** Remote meeting URL; may be absent. */
    @SerialName("zoom_url") public val zoomUrl: String? = null,
    /** Remote meeting password; may be absent. Never log it. */
    @SerialName("zoom_password") public val zoomPassword: String? = null,
    /** Additional joining instructions; may be absent. */
    @SerialName("zoom_info") public val zoomInfo: String? = null,
    /** Actual building identifier; may be absent. */
    @SerialName("bld_id") public val bldId: Int? = null,
    /** Observed formats: 1 in-person, 2 mixed, 3 remote. */
    @SerialName("format_id") public val formatId: Int = 0,
    /** Main building: commonly 13 Kronverksky, 273 Lomonosova, 5 Vyazemsky, 319 virtual; may be absent. */
    @SerialName("main_bld_id") public val mainBldId: Int? = null,
) {
    override fun toString(): String = "Lesson(pairId=$pairId, details=[redacted])"
}
