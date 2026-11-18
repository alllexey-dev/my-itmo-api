package dev.alllexey.itmoapi.myitmo.dormitory

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** Academic year that has dormitory rental agreements; its bounds select agreements in
 * [DormitoryApi.getDormitoryContracts]. Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class DormitoryContractPeriod(
    /** Starting calendar year of the academic year, e.g. 2026 for 2026/2027. */
    public val year: Int = 0,
    /** First day of the period, a yyyy-MM-dd calendar date without a time zone. */
    public val dateFrom: LocalDate = LocalDate(1970, 1, 1),
    /** Last day of the period, a yyyy-MM-dd calendar date without a time zone. */
    public val dateTo: LocalDate = LocalDate(1970, 1, 1),
)
