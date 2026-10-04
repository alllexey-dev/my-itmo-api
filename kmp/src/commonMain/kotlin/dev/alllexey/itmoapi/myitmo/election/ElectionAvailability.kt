package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable
import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlin.time.Instant

/** Campaign status and enrollment dates; id 1 denotes an open campaign.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class ElectionAvailability(
    /** Campaign status identifier; observed value 1 means open. */
    public val id: Int = 0,
    /** Server campaign status label. */
    public val status: String = "",
    /** Semester start instant from an offset date-time. */
    @Serializable(with = WireInstantSerializer::class)
    public val semesterStart: Instant = Instant.fromEpochSeconds(0),
    /** Semester end instant from an offset date-time. */
    @Serializable(with = WireInstantSerializer::class)
    public val semesterEnd: Instant = Instant.fromEpochSeconds(0),
    /** Enrollment opening instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateStart: Instant = Instant.fromEpochSeconds(0),
    /** Enrollment closing instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateEnd: Instant = Instant.fromEpochSeconds(0),
    /** Local enrollment opening time text. */
    public val timeStart: String = "",
    /** Local enrollment closing time text. */
    public val timeEnd: String = "",
    /** Academic year label. */
    public val studyYear: String = "",
    /** Semester identifier. */
    public val semesterId: Long = 0,
    /** Semester number. */
    public val semester: Int = 0,
)
