package dev.alllexey.itmoapi.core

import dev.alllexey.itmoapi.testing.fixture
import dev.alllexey.itmoapi.testing.fixturePaths
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class WireCompatibilityTest {
    @Serializable
    private data class Limits(val limit: Int = 0, val available: Int = 0)

    @Serializable
    private data class Sign(@SerialName("can_sign_in") val canSignIn: Boolean = false)

    @Serializable
    private data class PrimitiveDefaults(val attempt: Int = 0, @SerialName("have_tree") val haveTree: Boolean = false)

    @Serializable
    private data class RequiredName(val name: String)

    @Serializable
    private data class Education(val course: String = "")

    @Serializable
    private data class Day(val lessons: List<String> = emptyList())

    @Serializable
    private data class Dated(@Serializable(with = WireInstantSerializer::class) val time: Instant)

    @Test
    fun chosenConfigurationIsSp02ConfigBWithoutLeniency() {
        assertTrue(ItmoApiJson.configuration.ignoreUnknownKeys)
        assertFalse(ItmoApiJson.configuration.explicitNulls)
        assertTrue(ItmoApiJson.configuration.encodeDefaults)
        assertTrue(ItmoApiJson.configuration.coerceInputValues)
        assertFalse(ItmoApiJson.configuration.isLenient)
    }

    @Test
    fun quotedIntegersDecodeWithoutLeniency() {
        assertEquals(Limits(4, 2), ItmoApiJson.decodeFromString<Limits>(fixture("core/02-quoted-int.json")))
    }

    @Test
    fun quotedBooleansDecodeWithoutLeniency() {
        assertTrue(ItmoApiJson.decodeFromString<Sign>(fixture("core/03-quoted-boolean.json")).canSignIn)
    }

    @Test
    fun observedNullPrimitivesUseDeclaredDefaults() {
        assertEquals(PrimitiveDefaults(), ItmoApiJson.decodeFromString<PrimitiveDefaults>(fixture("core/04-null-primitive.json")))
    }

    @Test
    fun observedNullListsUseDeclaredDefaults() {
        assertEquals(Day(), ItmoApiJson.decodeFromString<Day>("""{"lessons":null}"""))
    }

    @Test
    fun unknownKeysAreIgnored() {
        assertEquals(Limits(), ItmoApiJson.decodeFromString<Limits>(fixture("core/08-unknown-key.json")))
    }

    @Test
    fun duplicateMyItmoKeysKeepLastValue() {
        assertEquals(Limits(4, 3), ItmoApiJson.decodeFromString<Limits>(fixture("core/10-duplicate-key-myitmo.json")))
    }

    @Test
    fun unobservedNumberToStringVariantRemainsRejected() {
        assertFailsWith<SerializationException> { ItmoApiJson.decodeFromString<Education>(fixture("core/01-number-to-string.json")) }
    }

    @Test
    fun requiredNonDefaultValueRejectsNullAndAbsence() {
        assertFailsWith<SerializationException> { ItmoApiJson.decodeFromString<RequiredName>(fixture("core/05-null-non-null.json")) }
        assertFailsWith<SerializationException> { ItmoApiJson.decodeFromString<RequiredName>("{}") }
    }

    @Test
    fun everySp02OffsetFormDecodesToTheExpectedInstant() {
        val forms = ItmoApiJson.decodeFromString<Map<String, String>>(fixture("core/13-date-forms.json"))
        val expected = mapOf(
            "gson-fcm-zero-seconds" to "2026-10-03T09:00:00Z",
            "gson-fcm-nanos" to "2026-10-03T09:00:42.123456789Z",
            "jackson-offset" to "2026-10-03T09:00:00Z",
            "jackson-micros" to "2026-10-03T09:00:00.123456Z",
            "jackson-utc" to "2026-10-03T09:00:00Z",
            "gson-utc-zero-seconds" to "2026-10-03T09:00:00Z",
            "myitmo-offset" to "2026-10-05T07:00:00Z",
        )
        assertEquals(expected.keys, forms.keys)
        forms.forEach { (name, value) ->
            val json = ItmoApiJson.encodeToString(value)
            val decoded = ItmoApiJson.decodeFromString(WireInstantSerializer, json)
            assertEquals(Instant.parse(expected.getValue(name)), decoded, name)
        }
    }

    @Test
    fun wireInstantRoundTripNormalizesToUtc() {
        val dated = ItmoApiJson.decodeFromString<Dated>("""{"time":"2026-10-03T12:00+03:00"}""")
        assertEquals("""{"time":"2026-10-03T09:00:00Z"}""", ItmoApiJson.encodeToString(dated))
    }

    @Test
    fun localDateBodyAndQueryHaveIsoFormatting() {
        val date = LocalDate(2026, 1, 5)
        assertEquals("2026-01-05", date.toQueryValue())
        assertEquals("\"2026-01-05\"", ItmoApiJson.encodeToString(date))
    }

    @Test
    fun loaderMakesEverySeedShapeAvailableOnJvmAndNative() {
        val shapes = listOf("schedule/personal.json", "recordbook/record-book.json", "sport/schedule.json", "sport/limits.json", "personalities/person.json")
        shapes.forEach { assertTrue(ItmoApiJson.parseToJsonElement(fixture(it)) is JsonObject) }
        assertTrue(fixturePaths().containsAll(shapes))
        assertTrue(fixture("errors/non-json-502.html").contains("502"))
    }

    @Test
    fun generatedConstantsEscapeDollarQuotesBackslashAndWhitespace() {
        val text = ItmoApiJson.decodeFromString<IdValuePair>(fixture("core/generator-escaping.json")).value
        assertEquals("\$value with \"quotes\", backslash \\ and \t", text)
    }

    @Test
    fun fixtureLookupRejectsUnknownPathsWithoutEchoingThem() {
        val failure = assertFailsWith<IllegalStateException> { fixture("missing") }
        assertEquals("Unknown fixture path", failure.message)
    }
}
