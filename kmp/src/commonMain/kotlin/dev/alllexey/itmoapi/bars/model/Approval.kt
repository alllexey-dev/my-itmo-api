package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Confirmed attempt: BARS grades include Отл., A; Хор., C; Удвл., E; Неуд., FX; Зачет.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class Approval(
    /** Record identifier. */
    public val id: Long = 0,
    /** BARS student identifier, not ISU. */
    @SerialName("student_id")
    public val studentId: Long = 0,
    /** ITMO.ID login. */
    @SerialName("student_login")
    public val studentLogin: String = "",
    /** Checkpoint plan identifier. */
    @SerialName("checkpoint_plan_id")
    public val checkpointPlanId: Long = 0,
    /** Attempt number starting at 1; retakes create a higher attempt. */
    public val attempt: Int = 0,
    /** Score sum when approved; nullable on the wire. */
    @SerialName("marks_sum")
    public val marksSum: Double? = null,
    /** BARS textual grade; absence has no grade. */
    @SerialName("mark_string")
    public val markString: String? = null,
    /** Whether the approval is active. */
    @SerialName("is_active")
    public val active: Boolean = false,
    /** Invalidated approval, observed on an unsuccessful first attempt. */
    @SerialName("is_invalid")
    public val invalid: Boolean = false,
    /** Explicit absence flag. */
    @SerialName("is_absent")
    public val absent: Boolean = false,
    /** Whether scores were recalculated. */
    @SerialName("was_recalculated")
    public val recalculated: Boolean = false,
    /** Approval of the course project rather than the discipline. */
    public val course: Boolean = false,
    /** Creation Unix epoch time in milliseconds; may be absent. */
    @SerialName("created_at")
    public val createdAt: Long? = null,
    /** Update Unix epoch time in milliseconds; may be absent. */
    @SerialName("updated_at")
    public val updatedAt: Long? = null,
) {
    /** MyITMO grade code (5/A, 4/C, 3/E, 2/FX); pass/fail and unknown text are trimmed unchanged. */
    public val gradeCode: String?
        get() {
            val text = markString?.trim() ?: return null
            val comma = text.indexOf(',')
            if (comma < 0) return text
            val word = text.substring(0, comma).trim().lowercase().replace('ё', 'е').trimEnd('.')
            val letter = text.substring(comma + 1).trim().uppercase()
            if (!letter.matches(Regex("[A-FX]{1,2}"))) return text
            val number = when (word) {
                "отл" -> 5
                "хор" -> 4
                "удвл", "удовл" -> 3
                "неуд" -> 2
                else -> return text
            }
            return "$number/$letter"
        }
}
