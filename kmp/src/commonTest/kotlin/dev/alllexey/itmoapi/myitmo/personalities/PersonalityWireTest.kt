package dev.alllexey.itmoapi.myitmo.personalities

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalityWireTest {
    @Test
    fun sp02SeedKeepsNumericIsuBooleanExchangeAndTypedNestedFields() {
        val profile = ItmoApiJson.decodeFromString(ResultResponse.serializer(Personality.serializer()),
            fixture("personalities/person.json")).requireResult()
        assertEquals(100001L, profile.isu)
        assertFalse(profile.exchangeTraining)
        assertEquals("test@example.invalid", profile.contacts.single().contact.single())
        assertEquals("Инженер", profile.positions.single().positionName)
        assertEquals("3", profile.education.single().course)
        assertTrue(profile.rooms.isEmpty())
    }

    @Test
    fun unobservedNumericCourseIsNotMadeLenient() {
        assertFailsWith<SerializationException> {
            ItmoApiJson.decodeFromString(Education.serializer(), """{"course":3}""")
        }
    }
}
