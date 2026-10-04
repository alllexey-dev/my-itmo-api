package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Complete sports semester with enrollment control periods. */
@Serializable
public data class SportSemester(
    /** Semester identifier. */
    public val id: Long = 0,
    /** Academic year in YYYY/YYYY format. */
    @SerialName("study_year") public val studyYear: String = "",
    /** Server semester classification; 0 has been observed. */
    public val semester: Int = 0,
    /** Semester start. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("date_start") public val dateStart: Instant = Instant.fromEpochMilliseconds(0),
    /** Soft end of the main period. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("date_end") public val dateEnd: Instant = Instant.fromEpochMilliseconds(0),
    /** Hard end after which the semester is closed. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("hard_date_end") public val hardDateEnd: Instant = Instant.fromEpochMilliseconds(0),
    /** Selection period start; technical minimum dates may occur. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("choice_start") public val choiceStart: Instant = Instant.fromEpochMilliseconds(0),
    /** Bachelor boundary; technical minimum dates may occur. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("bachelor_bound") public val bachelorBound: Instant = Instant.fromEpochMilliseconds(0),
    /** First resit period start. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("ppa1_start") public val ppa1Start: Instant = Instant.fromEpochMilliseconds(0),
    /** First resit period end. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("ppa1_end") public val ppa1End: Instant = Instant.fromEpochMilliseconds(0),
    /** Second resit period start. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("ppa2_start") public val ppa2Start: Instant = Instant.fromEpochMilliseconds(0),
    /** Second resit period end. */
    @Serializable(with = WireInstantSerializer::class)
    @SerialName("ppa2_end") public val ppa2End: Instant = Instant.fromEpochMilliseconds(0),
    /** Whether this is the current sports semester. */
    public val current: Boolean = false,
    /** Permitted enrollment duration in days. */
    @SerialName("sign_duration") public val signDuration: Int = 0,
)
