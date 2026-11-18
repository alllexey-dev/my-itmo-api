package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Discipline offered in the current elective enrollment campaign.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class AvailableDiscipline(
    /** Discipline curriculum identifier. */
    public val dcId: Long = 0,
    /** Discipline identifier. */
    public val discId: Long = 0,
    /** Teaching language identifier. */
    public val langId: Long = 0,
    /** Offering department name. */
    public val depName: String = "",
    /** Discipline name. */
    public val discName: String = "",
    /** Server language code. */
    public val langCode: String = "",
    /** Whether the discipline must be selected. */
    public val required: Boolean = false,
    /** Per-semester availability. */
    public val semesters: List<AvailableDisciplineSemester> = emptyList(),
    /** Discipline description. */
    public val description: String = "",
    /** Short department name. */
    public val depNameShort: String = "",
    /** Incompatible discipline identifiers. */
    public val notCompatibleWith: List<Long> = emptyList(),
)
