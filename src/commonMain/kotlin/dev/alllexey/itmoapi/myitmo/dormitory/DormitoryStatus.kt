package dev.alllexey.itmoapi.myitmo.dormitory

import kotlinx.serialization.Serializable

/** Dormitory application status from GET /api/dormitory/status/full.
 * The whole result is null for a student without a dormitory application.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class DormitoryStatus(
    /** Stage of the application. Observed: 1 in the queue; 2 a place is offered and a settlement date must be
     * picked within a limited window (24 or 48 hours); 3 a settlement date is picked; 4 settled in.
     */
    public val statusId: Int = 0,
    /** Place in the dormitory queue; observed null once a place is assigned. */
    public val queuePlace: Int? = null,
    /** Server code of the dormitory type, a short string; the list of codes is undocumented. */
    public val dormType: String = "",
    /** The assigned dormitory; null or absent before a place is assigned. */
    public val assignedDormitory: AssignedDormitory? = null,
)

/** Dormitory assigned to the student, with its settlement instructions. */
@Serializable
public data class AssignedDormitory(
    /** Building identifier. */
    public val buildingId: Long = 0,
    /** Dormitory name. */
    public val name: String = "",
    /** Postal address of the building, a single line. */
    public val address: String = "",
    /** Settlement instructions as HTML from ITMO; render it as sanitized text, never as trusted markup. */
    public val settlementProcedureText: String = "",
)
