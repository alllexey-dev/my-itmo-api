package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Current BARS user and server-selected academic period; catalogs and journals have no year/term query parameters.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class User(
    /** BARS user identifier, not ISU. */
    public val id: Long = 0,
    /** ITMO.ID login; students use an ISU string. */
    public val login: String = "",
    /** Given name. */
    @SerialName("first_name")
    public val firstName: String = "",
    /** Middle name. */
    @SerialName("middle_name")
    public val middleName: String = "",
    /** Family name. */
    @SerialName("last_name")
    public val lastName: String = "",
    /** Available roles, e.g. Обучающийся. */
    @SerialName("user_roles")
    public val userRoles: List<UserRole> = emptyList(),
    /** Selected server role. */
    @SerialName("selected_role")
    public val selectedRole: UserRole = UserRole(),
    /** Academic year in yyyy/yyyy form. */
    @SerialName("selected_year")
    public val selectedYear: String = "",
    /** Season wire value: SPRING 0, AUTUMN 1, not continuous semester number. */
    @SerialName("selected_term")
    public val selectedTerm: Int = 0,
    /** Personal settings including current_year and current_term. */
    @SerialName("personal_config")
    public val personalConfig: List<Setting> = emptyList(),
    /** Server permission to switch user. */
    @SerialName("can_change_user")
    public val canChangeUser: Boolean = false,
    /** Read-only access restriction. */
    @SerialName("restricted_to_have_read_only_access")
    public val restrictedToHaveReadOnlyAccess: Boolean = false,
) {
    // TODO: created_by, updated_by, created_at, updated_at, super_user_personal_number: unknown; observed only null.
    /** Season decoded with the throwing legacy wire mapping. */
    public val selectedTermValue: Term get() = Term.fromWire(selectedTerm)
}
