package dev.alllexey.itmoapi.itmoid

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class PkceTest {
    @Test
    fun rfc7636AppendixBVectorMatchesOnEveryTarget() {
        // Public RFC test vector, not a user verifier: https://www.rfc-editor.org/rfc/rfc7636#appendix-B
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test
    fun generatedVerifiersHaveThirtyTwoBytesOfEntropyAndNoPadding() {
        val first = Pkce.newVerifier()
        val second = Pkce.newVerifier()
        assertTrue(first.length == 43 && first.all { it in ALPHABET }, "Verifier shape")
        assertTrue(first != second, "Independent secure random verifiers")
        assertTrue(Pkce.challenge(first).length == 43 && '=' !in Pkce.challenge(first), "Challenge shape")
    }

    @Test
    fun everyRfcUnreservedCharacterIsAcceptedAtBothLengthBoundaries() {
        assertTrue(Pkce.challenge("a".repeat(43)).length == 43)
        assertTrue(Pkce.challenge(ALPHABET + ".~" + "a".repeat(62)).length == 43)
        assertTrue(Pkce.challenge("a".repeat(128)).length == 43)
    }

    @Test
    fun invalidVerifierShapeIsRejectedWithoutRenderingIt() {
        for (value in listOf("", "a".repeat(42), "a".repeat(129), "a".repeat(42) + "=", "a".repeat(42) + "é", "a".repeat(42) + " ")) {
            val failure = assertFailsWith<IllegalArgumentException> { Pkce.challenge(value) }
            assertEquals("Invalid PKCE verifier", failure.message)
        }
    }

    @Test
    fun stateUsesFreshUuidValues() {
        val first = Pkce.newState()
        val second = Pkce.newState()
        assertTrue(Uuid.parse(first).toString() == first && first != second, "Fresh UUID state")
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    }
}
