package dev.alllexey.itmoapi.myitmo.personalities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Employee position. All declared string fields were observed present and non-null. */
@Serializable
public data class Position(
    /** Department name, observed string. */
    @SerialName("department_name") public val departmentName: String = "",
    /** Department page link, observed string. */
    @SerialName("department_link") public val departmentLink: String = "",
    /** Position name, observed string. */
    @SerialName("position_name") public val positionName: String = "",
) {
    // TODO: vacation, start_vacation and end_vacation were only null; non-null types/date formats are unknown.
    // val vacation: ?
    // @SerialName("start_vacation") val startVacation: ?
    // @SerialName("end_vacation") val endVacation: ?
}
