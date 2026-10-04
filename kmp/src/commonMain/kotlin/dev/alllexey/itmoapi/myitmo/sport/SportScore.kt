package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.Serializable

/** Physical education points for the selected sports semester. */
@Serializable
public data class SportScore(
    /** Points broken down by source. */
    public val sum: Sum = Sum(),
    /** Award history; the server returns null when there are no awards. */
    public val attendances: List<SportAttendance>? = null,
) {
    /** Totals broken down by source. */
    @Serializable
    public data class Sum(
        /** Points from lesson attendance. */
        public val attendances: Long = 0,
        /** Bonus points and other awards. */
        public val other: Long = 0,
    )
}
