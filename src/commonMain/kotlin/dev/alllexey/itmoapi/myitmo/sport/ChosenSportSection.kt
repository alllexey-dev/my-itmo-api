package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sports section selected by the user. */
@Serializable
public data class ChosenSportSection(
    /** Section identifier. */
    public val id: Long = 0,
    /** Section display name. */
    @SerialName("section_name") public val sectionName: String = "",
    /** Server section level. */
    public val level: Int = 0,
    /** Selected lesson groups. */
    @SerialName("lesson_groups") public val lessonGroups: List<SportLessonGroup> = emptyList(),
)
