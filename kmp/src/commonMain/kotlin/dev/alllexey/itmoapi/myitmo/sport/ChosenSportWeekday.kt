package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Regular slot described by weekday; flattens legacy ChosenSportLesson fields. */
@Serializable
public data class ChosenSportWeekday(
    /** Lesson identifier. */
    public val id: Long = 0,
    /** Start offset date-time; absent for weekday-only slots. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("date_start") public val dateStart: Instant? = null,
    /** End offset date-time; absent for weekday-only slots. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("date_end") public val dateEnd: Instant? = null,
    /** Time slot identifier. */
    @SerialName("time_slot_id") public val timeSlotId: Long = 0,
    /** Local start time, HH:mm. */
    @SerialName("time_start") public val timeStart: String = "",
    /** Local end time, HH:mm. */
    @SerialName("time_end") public val timeEnd: String = "",
    /** Raw room identifier; -1 denotes Online. */
    @SerialName("room_id") public val roomId: Long = 0,
    /** Room display name. */
    @SerialName("room_name") public val roomName: String = "",
    /** Teacher ISU identifier. */
    @SerialName("teacher_isu") public val teacherIsu: Long = 0,
    /** Teacher full name. */
    @SerialName("teacher_fio") public val teacherFio: String = "",
    /** Server lesson type identifier. */
    @SerialName("type_id") public val typeId: Long = 0,
    /** Remote lesson link; may be absent. */
    @SerialName("link_url") public val linkUrl: String? = null,
    /** Optional server comment. */
    public val comment: String? = null,
    /** Whether the lesson overlaps another user event. */
    public val intersection: Boolean = false,
    /** Server-provided or localized weekday name. */
    public val weekday: String = "",
)
