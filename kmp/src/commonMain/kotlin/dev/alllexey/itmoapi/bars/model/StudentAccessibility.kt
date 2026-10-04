package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Server journal permissions, all observed as booleans, not score values.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class StudentAccessibility(
    /** Permission to edit current marks. */
    @SerialName("can_edit_current_marks")
    public val canEditCurrentMarks: Boolean = false,
    /** Permission to edit additional points. */
    @SerialName("can_edit_additional_marks")
    public val canEditAdditionalMarks: Boolean = false,
    /** Permission to edit course project marks. */
    @SerialName("can_edit_course_marks")
    public val canEditCourseMarks: Boolean = false,
    /** Permission to edit final marks. */
    @SerialName("can_edit_final_marks")
    public val canEditFinalMarks: Boolean = false,
    /** Permission to approve marks. */
    @SerialName("can_approve_marks")
    public val canApproveMarks: Boolean = false,
    /** Permission to approve retake marks. */
    @SerialName("can_approve_retry_marks")
    public val canApproveRetryMarks: Boolean = false,
    /** Unfilled key checkpoints flag. */
    @SerialName("has_unfilled_key_checkpoints")
    public val hasUnfilledKeyCheckpoints: Boolean = false,
    /** Unapproved project theme flag. */
    @SerialName("course_project_theme_not_approved")
    public val courseProjectThemeNotApproved: Boolean = false,
) {
}
