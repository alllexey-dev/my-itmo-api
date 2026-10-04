package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

class SportRemainingApiTest {
    @Test
    fun sportsTypeCatalogKeepsServerIdentifiersAndLabels() = runTest {
        val types = sportExchange("sport_types", fixture("sport/sport-types.json")) {
            val api: SportApi = SportApiImpl(it)
            api.getSportTypes().requireResult()
        }
        assertEquals(listOf(2L, 7L), types.map { it.id })
        assertEquals("Тестовый вид спорта", types.first().value)
        assertTrue(sportExchange("sport_types", fixture("sport/sport-types-empty.json")) {
            SportApiImpl(it).getSportTypes().requireResult()
        }.isEmpty())
    }

    @Test
    fun signAttemptsIsAnIntegerCounterIncludingZero() = runTest {
        for ((name, expected) in listOf("sign-attempts" to 3, "sign-attempts-zero" to 0)) {
            val count: Int = sportExchange("personal/sign_attempts", fixture("sport/$name.json")) {
                SportApiImpl(it).getSportSignAttempts().requireResult()
            }
            assertEquals(expected, count)
        }
    }

    @Test
    fun calendarUsesZeroPaddedInclusiveDatesAndPreservesLessonsNullAndEmptyDays() = runTest {
        val query = mapOf("date_start" to listOf("2026-01-05"), "date_end" to listOf("2026-01-11"))
        val days = sportExchange("personal/calendar", fixture("sport/calendar.json"), query) {
            SportApiImpl(it).getPersonalSportCalendar(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11)).requireResult()
        }
        assertEquals(LocalDate(2026, 10, 5), days.first().date)
        val lessons = requireNotNull(days.first().lessons)
        assertEquals(500001L, lessons.first().id)
        assertEquals(Instant.parse("2026-10-05T07:00:01Z"), lessons.first().date)
        assertEquals(Instant.parse("2026-10-05T08:30:01Z"), lessons.first().dateEnd)
        assertTrue(lessons.last().signed)
        assertNull(lessons.last().buildingId)
        assertEquals(-1L, lessons.last().roomId)
        assertNull(days[1].lessons)
        assertEquals(emptyList(), days[2].lessons)
        assertTrue(sportExchange("personal/calendar", fixture("sport/calendar-empty.json"), query) {
            SportApiImpl(it).getPersonalSportCalendar(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11)).requireResult()
        }.isEmpty())
    }

    @Test
    fun debtKeepsFractionalPointsAndOptionalSpecialAttempts() = runTest {
        val debt = sportExchange("personal/debt", fixture("sport/debt.json")) {
            SportApiImpl(it).getSportDebt().requireResult()
        }
        assertEquals(SportDebt(true, 12.5, 2), debt)
        val none = sportExchange("personal/debt", fixture("sport/debt-none.json")) {
            SportApiImpl(it).getSportDebt().requireResult()
        }
        assertFalse(none.havingDebt)
        assertNull(none.neededScore)
        assertNull(none.freeAttempts)
    }

    @Test
    fun externshipKeepsAcceptedDeclinedAndAbsentApplicationStates() = runTest {
        val accepted = sportExchange("personal/externat", fixture("sport/externat.json")) {
            SportApiImpl(it).getSportExternat().requireResult()
        }
        assertEquals(SportExternat(true, 12, null), accepted)
        val declined = sportExchange("personal/externat", fixture("sport/externat-declined.json")) {
            SportApiImpl(it).getSportExternat().requireResult()
        }
        assertEquals(SportExternat(false, 13, "Synthetic refusal"), declined)
        val none = sportExchange("personal/externat", fixture("sport/externat-none.json")) {
            SportApiImpl(it).getSportExternat().requireResult()
        }
        assertEquals(SportExternat(), none)
    }

    @Test
    fun healthGroupStaysInsideItsNestedResponse() = runTest {
        val response = sportExchange("personal/health_level", fixture("sport/health-level.json")) {
            SportApiImpl(it).getSportHealthLevel().requireResult()
        }
        assertEquals(SportHealthLevel(1, 200001, "Основная группа здоровья"), response.healthLevel)
    }

    @Test
    fun selectionsKeepQualificationLevelsAndEmptyLists() = runTest {
        val selections = sportExchange("personal/selections", fixture("sport/selections.json")) {
            SportApiImpl(it).getSportSelections().requireResult()
        }
        assertEquals(31L, selections.first().id)
        assertEquals("Тестовая дисциплина", selections.first().name)
        assertEquals(SportRequisite(41, "Сборная команда", "Тестовый отбор"), selections.first().requisites.single())
        assertTrue(selections.last().requisites.isEmpty())
        assertTrue(sportExchange("personal/selections", fixture("sport/selections-empty.json")) {
            SportApiImpl(it).getSportSelections().requireResult()
        }.isEmpty())
    }

    @Test
    fun projectsKeepCapacityPrerequisitesInstructionAndOptionalSupportingDocument() = runTest {
        val projects = sportExchange("projects/list", fixture("sport/projects.json")) {
            SportApiImpl(it).getSportProjects().requireResult()
        }
        val project = projects.first()
        assertEquals(71L, project.id)
        assertEquals("Тестовый экстернат", project.name)
        assertEquals("Synthetic credit format", project.description)
        assertTrue(project.signed)
        assertEquals(10, project.limit)
        assertEquals(2, project.available)
        assertEquals("https://example.invalid/rules", project.instructionLink)
        assertEquals("Synthetic confirmation", project.instructionDescription)
        assertTrue(project.requisiteAvailable)
        assertEquals("https://example.invalid/document", project.link)
        assertFalse(projects.last().signed)
        assertFalse(projects.last().requisiteAvailable)
        assertEquals(0, projects.last().available)
        assertNull(projects.last().link)
        assertTrue(sportExchange("projects/list", fixture("sport/projects-empty.json")) {
            SportApiImpl(it).getSportProjects().requireResult()
        }.isEmpty())
    }

    private fun operations(): List<Pair<String, suspend (SportApi) -> Unit>> = listOf(
        "sport_types" to { it.getSportTypes() },
        "personal/sign_attempts" to { it.getSportSignAttempts() },
        "personal/calendar" to { it.getPersonalSportCalendar(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11)) },
        "personal/debt" to { it.getSportDebt() },
        "personal/externat" to { it.getSportExternat() },
        "personal/health_level" to { it.getSportHealthLevel() },
        "personal/selections" to { it.getSportSelections() },
        "projects/list" to { it.getSportProjects() },
    )

    @Test
    fun everyRemainingOperationMapsNumericApi100AtHttp200And400() = runTest {
        for ((path, action) in operations()) {
            val query = if (path == "personal/calendar") {
                mapOf("date_start" to listOf("2026-01-05"), "date_end" to listOf("2026-01-11"))
            } else emptyMap()
            for (status in listOf(200, 400)) {
                val failure = assertFailsWith<MyItmoException.Api> {
                    sportExchange(path, fixture("sport/remaining-error.json"), query, status = status) {
                        action(SportApiImpl(it))
                    }
                }
                assertEquals(status, failure.status)
                assertEquals(100, failure.errorCode)
                assertEquals("Synthetic validation failure", failure.serverMessage)
            }
        }
    }

    @Test
    fun modelsKeepDocumentedClientFallbacksAndIgnoreUnknownFields() {
        assertEquals(SportDebt(), ItmoApiJson.decodeFromString<SportDebt>("{}"))
        assertEquals(SportExternat(), ItmoApiJson.decodeFromString<SportExternat>("{}"))
        assertEquals(SportHealthLevelResponse(), ItmoApiJson.decodeFromString<SportHealthLevelResponse>("{}"))
        assertEquals(SportSelection(), ItmoApiJson.decodeFromString<SportSelection>("{}"))
        assertEquals(SportRequisite(), ItmoApiJson.decodeFromString<SportRequisite>("{}"))
        assertEquals(SportProject(), ItmoApiJson.decodeFromString<SportProject>("""{"unknown":"ignored"}"""))
    }
}
