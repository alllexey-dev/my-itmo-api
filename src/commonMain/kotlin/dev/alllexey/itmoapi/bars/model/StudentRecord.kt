package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Student row in a journal.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class StudentRecord(
    /** BARS student identifier, not ISU. */
    @SerialName("student_id")
    public val studentId: Long = 0,
    /** ITMO.ID login; compare to the expected session owner. */
    @SerialName("student_login")
    public val studentLogin: String = "",
    /** Student display name. */
    @SerialName("student_name")
    public val studentName: String = "",
    /** Scores and confirmed attempts. */
    public val marks: StudentMarks = StudentMarks(),
    /** Edit permissions, not scores. */
    public val accessibility: StudentAccessibility = StudentAccessibility(),
    /** Whether the student requests score improvement. */
    @SerialName("wants_to_increase_marks")
    public val wantsToIncreaseMarks: Boolean = false,
) {
}
