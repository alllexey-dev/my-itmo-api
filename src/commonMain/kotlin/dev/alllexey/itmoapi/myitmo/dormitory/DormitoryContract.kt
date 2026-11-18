package dev.alllexey.itmoapi.myitmo.dormitory

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Dormitory rental agreement with its balance and payment schedule.
 * Number and amounts are personal financial data: never log them.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class DormitoryContract(
    /** Agreement identifier, a string such as `00-00000000`. */
    public val contractId: String = "",
    /** Agreement number as printed on the agreement. */
    public val number: String = "",
    /** Agreement title. */
    public val name: String = "",
    /** Start of the agreement, an offset date-time exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateStart: Instant = Instant.fromEpochMilliseconds(0),
    /** End of the agreement, an offset date-time exposed as an instant. */
    @Serializable(with = WireInstantSerializer::class)
    public val dateEnd: Instant = Instant.fromEpochMilliseconds(0),
    /** Whether the agreement is in force; earlier agreements are inactive. */
    public val active: Boolean = false,
    /** Balance in rubles, possibly with kopecks as a fraction; negative means debt, positive an overpayment. */
    public val balance: Double = 0.0,
    /** Payment schedule in server order. */
    public val payments: List<DormitoryPayment> = emptyList(),
)

/** One scheduled payment of a rental agreement. */
@Serializable
public data class DormitoryPayment(
    /** Amount due in rubles, possibly with kopecks as a fraction. */
    public val sum: Double = 0.0,
    /** Amount already paid in rubles; the payment is settled when it reaches [sum]. */
    public val paid: Double = 0.0,
    /** Due date, an offset date-time exposed as an instant; display it in Moscow time. */
    @Serializable(with = WireInstantSerializer::class)
    public val payUntil: Instant = Instant.fromEpochMilliseconds(0),
    /** Label of the billed period, such as a month and year. */
    public val period: String = "",
)
