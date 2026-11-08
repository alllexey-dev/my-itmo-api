package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Paid-education (tuition) agreement from GET /api/finances/edupayments/contracts.
 * No non-empty response has been observed yet (only empty lists of state-funded students): the fields are the
 * ones the my.itmo site reads, with the types of the matching dormitory agreement fields.
 * Number and amounts are personal financial data: never log them.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class TuitionContract(
    /** Agreement identifier, the key [TuitionPaymentContract.contractId] uses too. */
    public val contractId: String = "",
    /** Agreement number as printed on the agreement. */
    public val number: String = "",
    /** Start of the agreement's academic year, an offset date-time exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateStart: Instant = Instant.fromEpochMilliseconds(0),
    /** End of the agreement's academic year, an offset date-time exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateEnd: Instant = Instant.fromEpochMilliseconds(0),
    /** Whether the agreement is in force; earlier agreements are archived. */
    public val active: Boolean = false,
    /** Name of the educational program. */
    @SerialName("op")
    public val program: String = "",
    /** Billing period label [sum] applies to, as the site prints it after "в", such as a semester. */
    public val period: String = "",
    /** Payment size per [period] in rubles, possibly with kopecks as a fraction. */
    public val sum: Double = 0.0,
)
