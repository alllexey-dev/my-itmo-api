package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sports points awarded for a lesson, competition or other activity. */
@Serializable
public data class SportAttendance(
    /** Observed lesson and competition; this set may expand. */
    public val type: String = "",
    /** Activity display name. */
    public val name: String = "",
    /** Evaluation identifier. */
    @SerialName("evaluation_id") public val evaluationId: Long = 0,
    /** Evaluation display name. */
    @SerialName("evaluation_name") public val evaluationName: String = "",
    /** Server section level. */
    @SerialName("section_level") public val sectionLevel: Int = 0,
    /** Points awarded for this event. */
    public val score: Int = 0,
    /** Event offset date-time, exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val date: Instant = Instant.fromEpochMilliseconds(0),
    /** Whether this award is for a competition. */
    @SerialName("is_competition") public val isCompetition: Boolean = false,
    /** Sport discipline for competitions; may be absent for ordinary lessons. */
    @SerialName("discipline_name") public val disciplineName: String? = null,
    /** Competition name; absent for ordinary lessons. */
    @SerialName("competition_name") public val competitionName: String? = null,
    /** Competition result or place, for example participation; absent for ordinary lessons. */
    public val place: String? = null,
)
