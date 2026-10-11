package dev.alllexey.itmoapi.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

/** ITMO's time zone, Europe/Moscow. Show API instants in it rather than the device zone;
 * schedule and sport HH:mm strings are already Moscow time.
 */
public val MOSCOW: TimeZone = TimeZone.of("Europe/Moscow")

/** Offset date-times become instants; Gson-written wire dates may omit zero seconds.
 * Observed precision ranges from minutes to nanoseconds. Encoding normalizes to UTC.
 * BARS epoch values are Long and must not use this serializer. Display instants in [MOSCOW].
 */
public object WireInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WireInstant", PrimitiveKind.STRING)
    private val format = DateTimeComponents.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour()
        char(':')
        minute()
        optional {
            char(':')
            second()
            optional { char('.'); secondFraction(1, 9) }
        }
        offset(UtcOffset.Formats.ISO)
    }

    override fun deserialize(decoder: Decoder): Instant = format.parse(decoder.decodeString()).toInstantUsingOffset()
    override fun serialize(encoder: Encoder, value: Instant): Unit = encoder.encodeString(value.toString())
}

/** Formats a calendar date explicitly as ISO yyyy-MM-dd for MyITMO query parameters. */
public fun LocalDate.toQueryValue(): String = LocalDate.Formats.ISO.format(this)
