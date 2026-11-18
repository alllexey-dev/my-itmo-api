package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Scores and approvals. regularSum and total are server aggregates and must not be recomputed.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class StudentMarks(
    /** Regular scores; empty for an unfilled journal. */
    public val regular: List<Mark> = emptyList(),
    /** Final exam/pass score, absent until marked. */
    @SerialName("final")
    public val finalMark: Mark? = null,
    /** Additional points with null checkpoint_id. */
    public val additional: Mark? = null,
    /** Server current-assessment sum; may be absent. */
    public val regularSum: Double? = null,
    /** Server total; 0.0 in an empty journal means no marks, not a grade. */
    public val total: Double? = null,
    /** Confirmed attempts; the highest attempt number is current. */
    @SerialName("active_approvals")
    public val activeApprovals: List<Approval> = emptyList(),
) {
    /** Whether any score record exists, independently of total=0.0. */
    public fun hasAnyMark(): Boolean = regular.isNotEmpty() || finalMark != null || additional != null
}
