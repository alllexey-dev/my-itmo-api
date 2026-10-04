package dev.alllexey.itmoapi.myitmo.finances

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Total accruals in one financial category.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class ScholarshipTotal(
    /** Financial category identifier. */
    @SerialName("category_id")
    public val categoryId: Long = 0,
    /** Category label; preserve server whitespace and line breaks. */
    @SerialName("category_name")
    public val categoryName: String = "",
    /** Total in rubles, not kopecks. */
    public val sum: Long = 0,
)
