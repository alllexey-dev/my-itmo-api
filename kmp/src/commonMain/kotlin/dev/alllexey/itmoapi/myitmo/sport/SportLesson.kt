package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sports lesson available for enrollment or shown in the personal calendar. */
@Serializable
public data class SportLesson(
    /** Lesson identifier. */
    public val id: Long = 0,
    /** Start offset date-time, exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val date: Instant = Instant.fromEpochMilliseconds(0),
    /** End offset date-time, exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("date_end") public val dateEnd: Instant = Instant.fromEpochMilliseconds(0),
    /** Section identifier. */
    @SerialName("section_id") public val sectionId: Long = 0,
    /** Section display name. */
    @SerialName("section_name") public val sectionName: String = "",
    /** Observed 1 free attendance, 2 selection-based section. */
    @SerialName("section_level") public val sectionLevel: Long = 0,
    /** Lesson group identifier. */
    @SerialName("lesson_group_id") public val lessonGroupId: Long = 0,
    /** Observed 1 open/free, 2 training, 3 intermediate, 4 team. */
    @SerialName("lesson_level") public val lessonLevel: Long = 0,
    /** Observed 1 open, 2 free attendance, 5 debt, 6 standards, 7 externship, 8 additional. */
    @SerialName("type_id") public val typeId: Long = 0,
    /** Raw venue ID, not necessarily a filter option. Online has null; off-site venues have positive IDs. Preserve both. */
    @SerialName("building_id") public val buildingId: Long? = null,
    /** Raw room ID; -1 explicitly denotes Online (observed 2026-09-09). */
    @SerialName("room_id") public val roomId: Long = 0,
    /** Room display name. */
    @SerialName("room_name") public val roomName: String = "",
    /** Total places. */
    public val limit: Long = 0,
    /** Available places. */
    public val available: Long = 0,
    /** Optional server comment. */
    public val comment: String? = null,
    /** Time slot identifier. */
    @SerialName("time_slot_id") public val timeSlotId: Long = 0,
    /** Local start time, HH:mm. */
    @SerialName("time_slot_start") public val timeSlotStart: String = "",
    /** Local end time, HH:mm. */
    @SerialName("time_slot_end") public val timeSlotEnd: String = "",
    /** Whether this lesson overlaps another user event. */
    public val intersection: Boolean = false,
    /** Server enrollment decision and refusal reasons. */
    @SerialName("can_sign_in") public val canSignIn: CanSignIn = CanSignIn(),
    /** Related alternative lessons. */
    @SerialName("other_lessons") public val otherLessons: List<OtherLesson> = emptyList(),
    /** Whether the user is enrolled. */
    public val signed: Boolean = false,
    /** Teacher ISU identifier. */
    @SerialName("teacher_isu") public val teacherIsu: Long = 0,
    /** Teacher full name. */
    @SerialName("teacher_fio") public val teacherFio: String = "",
) {
    /** Short description of a related alternative lesson. */
    @Serializable
    public data class OtherLesson(
        /** Alternative lesson identifier. */
        public val id: Long = 0,
        /** Server numeric weekday. */
        public val weekday: Int = 0,
        /** Raw room identifier. */
        @SerialName("room_id") public val roomId: Long = 0,
        /** Room display name. */
        @SerialName("room_name") public val roomName: String = "",
        /** Evaluation identifier; observed null. */
        @SerialName("evaluation_id") public val evaluationId: Long? = null,
        /** Evaluation display name; observed null. */
        @SerialName("evaluation_name") public val evaluationName: String? = null,
        /** Time slot identifier. */
        @SerialName("time_slot_id") public val timeSlotId: Long = 0,
        /** Local start time, HH:mm. */
        @SerialName("time_slot_start") public val timeSlotStart: String = "",
        /** Local end time, HH:mm. */
        @SerialName("time_slot_end") public val timeSlotEnd: String = "",
        /** Whether this alternative repeats. */
        public val repeatable: Boolean = false,
        /** Teacher ISU identifier. */
        @SerialName("teacher_isu") public val teacherIsu: Long = 0,
        /** Teacher full name. */
        @SerialName("teacher_fio") public val teacherFio: String = "",
        /** Server lesson type. */
        @SerialName("type_id") public val typeId: Long = 0,
        /** Optional server comment. */
        public val comment: String? = null,
        /** Whether the alternative overlaps another user event. */
        public val intersection: Boolean = false,
    )
}
