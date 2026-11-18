package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Regular lessons within a selected sports section. */
@Serializable
public data class SportLessonGroup(
    /** Lesson group identifier. */
    public val id: Long = 0,
    /** Observed 1 free attendance, 2 training, 3 intermediate, 4 team; may expand. */
    public val level: Int = 0,
    /** Level display name. */
    @SerialName("level_name") public val levelName: String = "",
    /** Whether the group contains future one-off lessons. */
    @SerialName("has_future_lessons") public val hasFutureLessons: Boolean = false,
    /** Concrete lessons with calendar dates. */
    public val lessons: List<ChosenSportLesson> = emptyList(),
    /** Regular lessons described by weekday. */
    public val weekdays: List<ChosenSportWeekday> = emptyList(),
)
