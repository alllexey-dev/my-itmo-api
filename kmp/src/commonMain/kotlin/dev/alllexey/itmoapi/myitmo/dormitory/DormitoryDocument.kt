package dev.alllexey.itmoapi.myitmo.dormitory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Document needed for dormitory settlement, such as a medical certificate, and its status.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class DormitoryDocument(
    /** Document type identifier. */
    public val documentType: Long = 0,
    /** Document type label. */
    public val documentTypeName: String = "",
    /** Status code; the complete list is undocumented, use [statusName] for display. */
    public val status: Int = 0,
    /** Status label, observed values include `Отсутствует` (missing) and `Подтверждено` (confirmed). */
    @SerialName("status_name")
    public val statusName: String = "",
    /** Request template through which the document is uploaded, see the requests area. */
    public val templateId: Long = 0,
) {
    // TODO: requestId (the filed request) was only null; its non-null type is unknown.
    // val requestId: ?
}
