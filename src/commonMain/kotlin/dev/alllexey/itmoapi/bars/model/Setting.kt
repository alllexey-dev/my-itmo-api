package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Name/value setting from GET config/ or POST config/personal; even numeric and boolean values are strings.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class Setting(
    /** Present in responses, omitted when submitting a personal setting. */
    public val id: Long? = null,
    /** Examples: current_year, current_term, daily_message. */
    public val name: String = "",
    /** String setting value; null observed in global configuration. */
    public val value: String? = null,
) {
}
