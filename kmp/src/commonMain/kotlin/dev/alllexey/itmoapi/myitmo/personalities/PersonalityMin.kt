package dev.alllexey.itmoapi.myitmo.personalities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Short profile in search results. Numeric id usually matches ISU. Legacy search strings
 * have no documented null variant and use non-null defaults; no null variant is documented for these search fields. */
@Serializable
public data class PersonalityMin(
    /** Person identifier, usually ISU. */
    public val id: Long = 0,
    /** Server full name. */
    public val fio: String = "",
    /** Server gender label; kept open-ended. */
    public val gender: String = "",
    /** Phone display string. */
    public val phone: String = "",
    /** Email display string. */
    public val email: String = "",
    /** Brief workplace/position description. */
    public val work: String = "",
    /** Profile photo URL; no null variant is documented. */
    @SerialName("photo") public val photoUrl: String = "",
) {
    // TODO: education exists in search results, but its structure is unconfirmed.
    // val education: ?
}
