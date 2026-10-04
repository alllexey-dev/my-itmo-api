package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Discipline availability in one academic semester.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class AvailableDisciplineSemester(
    /** Semester number. */
    public val semester: Int = 0,
    /** Server status; observed value 1 means available. */
    public val statusId: Int = 0,
    /** Opaque identifier accepted by the order endpoint; never parse numerically. */
    public val groupFlow: String = "",
    /** Server availability label. */
    public val statusName: String = "",
)
