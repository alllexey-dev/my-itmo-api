package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Own journal from marks/{plan}/{type}/{identifier}/student. Observed students had one element; consumers should verify the login.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class StudentJournal(
    /** Student journal rows. */
    public val students: List<StudentRecord> = emptyList(),
    /** Journal plan and flow address. */
    public val headers: JournalHeaders = JournalHeaders(),
) {
}
