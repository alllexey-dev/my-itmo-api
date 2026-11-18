package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** BARS user role; numeric id is not ISU. Flags are present only in selected_role.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class UserRole(
    /** Role identifier. */
    public val id: Long = 0,
    /** Role name, e.g. Обучающийся. */
    public val name: String = "",
    /** Selected-role lock flag, absent in available roles. */
    public val locked: Boolean? = null,
    /** Selected-role selection flag, absent in available roles. */
    public val selected: Boolean? = null,
    /** Selected-role whitelist flag. */
    @SerialName("allows_white_list")
    public val allowsWhiteList: Boolean? = null,
    /** Selected-role multiple-selection flag. */
    @SerialName("allows_multiple")
    public val allowsMultiple: Boolean? = null,
) {
    // TODO: white_list: List<unknown>; observed only empty.
    // TODO: created_*, updated_*, main_user, requires_main_user_user_role: unknown; observed only null.
}
