package dev.alllexey.itmoapi.myitmo.personalities

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.myitmo.qr.areaExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalitiesApiTest {
    @Test
    fun personalityRequestUsesTheIsuPathRussianLanguageAndBearer() = runTest {
        val profile = areaExchange("/api/personalities/persons/123456", fixture("personalities/student.json")) {
            PersonalitiesApiImpl(it).getPersonality(123456).requireResult()
        }
        assertEquals(123456L, profile.isu)
    }

    @Test
    fun missingPersonalityPreservesObservedHttp400AndApi100() = runTest {
        val failure = assertFailsWith<MyItmoException.Api> {
            areaExchange("/api/personalities/persons/123457", fixture("personalities/missing-person.json"), status = 400) {
                PersonalitiesApiImpl(it).getPersonality(123457)
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
        assertNull(failure.serverMessage)
    }

    @Test
    fun searchKeepsPagingQueryEncodingAndCountWrapper() = runTest {
        val query = "Тестовый + & /"
        val page = areaExchange("/api/personalities/persons", fixture("personalities/search.json"),
            mapOf("limit" to listOf("10"), "offset" to listOf("20"), "q" to listOf(query))) {
            PersonalitiesApiImpl(it).searchPersonalities(10, 20, query).requireResult()
        }
        assertEquals(31, page.count)
        val profile = page.data.single()
        assertEquals(123456L, profile.id)
        assertEquals("Тестовый Студент", profile.fio)
        assertEquals("male", profile.gender)
        assertEquals("", profile.phone)
        assertEquals("student@example.test", profile.email)
        assertEquals("", profile.work)
        assertEquals("https://example.test/student.jpg", profile.photoUrl)
    }

    @Test
    fun emptySearchPreservesZeroCountAndEmptyData() = runTest {
        val page = areaExchange("/api/personalities/persons", fixture("personalities/search-empty.json"),
            mapOf("limit" to listOf("10"), "offset" to listOf("0"), "q" to listOf(""))) {
            PersonalitiesApiImpl(it).searchPersonalities(10, 0, "").requireResult()
        }
        assertEquals(0, page.count)
        assertTrue(page.data.isEmpty())
    }

    @Test
    fun studentProfileKeepsStringCourseAndEmptyEmployeeLists() = runTest {
        val profile = areaExchange("/api/personalities/persons/123456", fixture("personalities/student.json")) {
            PersonalitiesApiImpl(it).getPersonality(123456).requireResult()
        }
        assertEquals(123456L, profile.isu)
        assertEquals("Тестовый Студент", profile.fio)
        assertEquals("male", profile.gender)
        assertEquals("https://example.test/student.jpg", profile.photoUrl)
        assertTrue(profile.contacts.isEmpty())
        assertTrue(profile.rooms.isEmpty())
        assertTrue(profile.positions.isEmpty())
        val education = profile.education.single()
        assertEquals("3", education.course)
        assertEquals("T1234", education.group)
        assertEquals("Тестовый факультет", education.facultyName)
        assertFalse(profile.exchangeTraining)
    }

    @Test
    fun employeeProfileKeepsContactsAndPositionsAndIgnoresUnmodelledFields() = runTest {
        val profile = areaExchange("/api/personalities/persons/234567", fixture("personalities/employee.json")) {
            PersonalitiesApiImpl(it).getPersonality(234567).requireResult()
        }
        assertEquals(234567L, profile.isu)
        assertEquals("Тестовый Сотрудник", profile.fio)
        assertEquals("male", profile.gender)
        assertEquals("https://example.test/employee.jpg", profile.photoUrl)
        val contact = profile.contacts.single()
        assertEquals(listOf("teacher@example.test"), contact.contact)
        assertEquals("Электронная почта", contact.contactAlias)
        assertTrue(profile.rooms.isEmpty())
        val position = profile.positions.single()
        assertEquals("Тестовое подразделение", position.departmentName)
        assertEquals("https://example.test/department", position.departmentLink)
        assertEquals("Преподаватель", position.positionName)
        assertTrue(profile.education.isEmpty())
        assertFalse(profile.exchangeTraining)
    }

    @Test
    fun serviceProfileKeepsNullPhotoAndEmptyLists() = runTest {
        val profile = areaExchange("/api/personalities/persons/345678", fixture("personalities/service.json")) {
            PersonalitiesApiImpl(it).getPersonality(345678).requireResult()
        }
        assertEquals(345678L, profile.isu)
        assertEquals("Тестовая Служебная Запись", profile.fio)
        assertNull(profile.photoUrl)
        assertTrue(profile.contacts.isEmpty())
        assertTrue(profile.rooms.isEmpty())
        assertTrue(profile.positions.isEmpty())
        assertTrue(profile.education.isEmpty())
        assertFalse(profile.exchangeTraining)
    }
}
