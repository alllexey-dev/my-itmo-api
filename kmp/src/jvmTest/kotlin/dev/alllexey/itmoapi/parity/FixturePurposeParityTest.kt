package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.EnvelopesTest
import dev.alllexey.itmoapi.core.ItmoTransportTest
import dev.alllexey.itmoapi.core.WireCompatibilityTest
import dev.alllexey.itmoapi.itmoid.ItmoIdClientTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Non-model fixtures retain their actual purpose, without pretending they are model wire equality. */
class FixturePurposeParityTest {
    private fun register(path: String, purpose: String, verify: () -> Unit) {
        verify()
        ParityRegistrations.record(path, purpose)
    }

    @Test
    fun sp02CodecPolicySeeds() {
        val tests = WireCompatibilityTest()
        tests.chosenConfigurationIsSp02ConfigBWithoutLeniency()
        val purpose = "SP-02 config B / ML-01b codec policy"
        register("core/01-number-to-string.json", purpose, tests::unobservedNumberToStringVariantRemainsRejected)
        register("core/02-quoted-int.json", purpose, tests::quotedIntegersDecodeWithoutLeniency)
        register("core/03-quoted-boolean.json", purpose, tests::quotedBooleansDecodeWithoutLeniency)
        register("core/04-null-primitive.json", purpose, tests::observedNullPrimitivesUseDeclaredDefaults)
        register("core/05-null-non-null.json", purpose, tests::requiredNonDefaultValueRejectsNullAndAbsence)
        register("core/08-unknown-key.json", purpose, tests::unknownKeysAreIgnored)
        register("core/10-duplicate-key-myitmo.json", purpose, tests::duplicateMyItmoKeysKeepLastValue)
        register("core/13-date-forms.json", "SP-02 row 13 / ADR 0025 Q5 date-by-instant codec behavior", tests::everySp02OffsetFormDecodesToTheExpectedInstant)
        register("core/generator-escaping.json", "ML-01b generated fixture escaping", tests::generatedConstantsEscapeDollarQuotesBackslashAndWhitespace)
    }

    @Test
    fun httpAndOAuthFailureFixtures() {
        val transport = ItmoTransportTest()
        register("errors/non-json-502.html", "ML-01b HTTP-before-decode failure behavior", transport::nonJson502MapsStatusBeforePayloadDecoding)
        register("errors/error-code-200.json", "ML-01b API failure at HTTP 200", transport::errorCodeAtHttp200MapsApi)
        register("errors/error-envelope-400.json", "ML-01b HTTP 400 / API 100 envelope behavior", EnvelopesTest()::resultResponsePreserves400AndError100)
        register("itmoid/oauth-error.json", "ML-04a invalid_grant authentication failure and redacted diagnostics", ItmoIdClientTest()::oauthInvalidGrant400MapsAuthWithoutDescriptionOrTokens)
    }

    @Test
    fun fiveFieldTokenConversionFixture() {
        register("itmoid/token-success.json", "ML-04a five-field Storage snapshot / injected Clock conversion") {
            ItmoIdClientTest().exchangePreservesObservedFormAndComputesBothExpiriesFromOneClockRead()
            ItmoIdClientTest().oneShotRefreshUsesPluralScopesAndHasNoImplicitRetryOrStorage()
        }
    }

    @Test
    fun provenanceMetadataFixtures() {
        val kmp = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { if (it.name == "kmp") it else File(it, "kmp") }
            .first { File(it, "fixtures").isDirectory }
        for (area in listOf("election", "recordbook", "studyplan")) {
            val path = "$area/README.md"
            register(path, "synthetic fixture provenance metadata integrity") {
                val text = File(kmp, "fixtures/$path").readText()
                assertTrue("synthetic" in text, "$path declares synthetic provenance")
                assertTrue("source" in text && "SHA" in text, "$path requires source SHA")
                assertTrue("fixture" in text && "path" in text, "$path requires original fixture path")
                val references = Regex("`([a-z-]+\\.json)`").findAll(text).map { it.groupValues[1] }.toList()
                if (area != "election") {
                    assertTrue(references.isNotEmpty(), "$path names concrete fixture references")
                    references.forEach { reference ->
                        assertTrue(File(kmp, "fixtures/$area/$reference").isFile, "$path reference exists: $reference")
                    }
                } else {
                    assertTrue("dev.alllexey:my-itmo-api:1.8.2" in text, "Election pins legacy source artifact")
                    assertTrue("selected_flow_chains" in text && "not ported" in text, "Election identifies the deprecated exclusion")
                    assertTrue("No new missing-field/default difference is accepted" in text, "Election preserves reviewed-difference boundary")
                }
            }
        }
    }
}
