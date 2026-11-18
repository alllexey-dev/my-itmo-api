package dev.alllexey.itmoapi.myitmo.personalities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Full public MyITMO profile. Student, employee and service samples contained all declared keys.
 * These are observations, not an API guarantee. Collections are empty rather than null in these samples. */
@Serializable
public data class Personality(
    /** Numeric ISU, observed non-null and equal to the requested number. */
    public val isu: Long = 0,
    /** Full name in server format; observed string without null/absence. */
    public val fio: String = "",
    /** Server gender string, including "male"; kept open-ended. */
    public val gender: String = "",
    /** Photo URL for student/employee; observed null for service profile. */
    @SerialName("photo") public val photoUrl: String? = null,
    /** Employee contact groups; empty for student/service. */
    public val contacts: List<Contact> = emptyList(),
    /** Employee rooms; all checked profiles had an empty array. */
    public val rooms: List<Room> = emptyList(),
    /** Employee positions; empty for student/service. */
    public val positions: List<Position> = emptyList(),
    /** Student education records; empty for employee/service. */
    public val education: List<Education> = emptyList(),
    /** Exchange-study flag, observed as non-null JSON boolean. */
    @SerialName("exchange_training") public val exchangeTraining: Boolean = false,
) {
    // TODO: powers was [] or objects with string dep_name, dep_link, power_name; other forms/nested nullability unknown.
    // val powers: ?
    // TODO: levels was null or an object with string rank, degree; other forms unconfirmed.
    // val levels: ?
    // TODO: activities was only null; non-null JSON type/structure unknown.
    // val activities: ?
}
