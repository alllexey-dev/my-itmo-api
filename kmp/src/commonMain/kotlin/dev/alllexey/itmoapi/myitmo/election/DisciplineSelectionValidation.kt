package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Server completeness and compatibility check for selected disciplines.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class DisciplineSelectionValidation(
    /** Validated discipline identifiers. */
    public val disciplines: List<Long> = emptyList(),
    /** Whether elective selection satisfies the rules. */
    public val validVariantSelection: Boolean = false,
    /** Whether required selection satisfies the rules. */
    public val validRequiredSelection: Boolean = false,
    /** Required discipline count still needed. */
    public val needSelectRequired: Int = 0,
    /** Minimum elective discipline count still needed. */
    public val needSelectVariants: Int = 0,
    /** Maximum elective discipline count permitted. */
    public val needSelectVariantsMax: Int = 0,
    /** Whether a compatible schedule is available. */
    public val scheduleAvailable: Boolean = false,
)
