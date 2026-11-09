package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Tuition agreement with its balance and payment schedule, from GET /api/finances/edupayments/payments.
 * No non-empty response has been observed yet (only empty lists of state-funded students): the my.itmo site
 * reads the same fields as for a dormitory rental agreement, and the types follow that verified shape.
 * Number and amounts are personal financial data: never log them.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class TuitionPaymentContract(
    /** Agreement identifier, the same key as [TuitionContract.contractId]. */
    public val contractId: String = "",
    /** Agreement number as printed on the agreement. */
    public val number: String = "",
    /** Agreement title. */
    public val name: String = "",
    /** Start of the agreement's academic year, an offset date-time exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateStart: Instant = Instant.fromEpochMilliseconds(0),
    /** End of the agreement's academic year, an offset date-time exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateEnd: Instant = Instant.fromEpochMilliseconds(0),
    /** Whether the agreement is in force; earlier agreements are archived. */
    public val active: Boolean = false,
    /** Balance in rubles, possibly with kopecks as a fraction; positive is an overpayment kept for later periods. */
    public val balance: Double = 0.0,
    /** Payment schedule in server order. */
    public val payments: List<TuitionPayment> = emptyList(),
)

/** One scheduled tuition payment. */
@Serializable
public data class TuitionPayment(
    /** Amount due in rubles, possibly with kopecks as a fraction. */
    public val sum: Double = 0.0,
    /** Amount already paid in rubles; the payment is settled when it reaches [sum]. */
    public val paid: Double = 0.0,
    /** Due date, an offset date-time exposed as an instant; display it in Moscow time. */
    @Serializable(with = WireInstantSerializer::class)
    public val payUntil: Instant = Instant.fromEpochMilliseconds(0),
    /** Label of the billed period, such as a semester. */
    public val period: String = "",
)
