package dev.alllexey.itmoapi.myitmo.recordbook

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Assessment inside a recordbook discipline; parentId links the flat tree. */
@Serializable
public data class ControlEntry(
    /** Assessment identifier. */
    public val id: Long = 0,
    /** Display name of the assessment. */
    @SerialName("control_name")
    public val controlName: String = "",
    /** Parent identifier; null for root items. */
    @SerialName("parent_id")
    public val parentId: Long? = null,
    /** Lower bound of the points range, not the actual score. */
    @SerialName("lower_value")
    public val lowerValue: Double? = null,
    /** Maximum possible points. */
    @SerialName("max_value")
    public val maxValue: Double? = null,
    /** Minimum points configured for the assessment. */
    @SerialName("min_value")
    public val minValue: Double? = null,
    /** Whether the assessment is mandatory. */
    public val required: Boolean = false,
    /** Actual points; null when there is no result yet. */
    public val rate: Double? = null,
    /** Assessment or grading date-time; may be absent. The offset is normalized to UTC. */
    @Serializable(with = WireInstantSerializer::class)
    public val date: Instant? = null,
    /** Teacher who recorded the result; may be absent. */
    public val teacher: RecordBookTeacher? = null,
)
