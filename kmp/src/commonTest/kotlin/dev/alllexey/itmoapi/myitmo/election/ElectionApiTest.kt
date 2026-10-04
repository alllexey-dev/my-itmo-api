package dev.alllexey.itmoapi.myitmo.election

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.Instant

class ElectionApiTest {
    @Test
    fun campaignRetainsStatusTimesAndAcademicSemester() = runTest {
        val campaign = myItmoAreaExchange("election/students/availability", fixture("election/availability.json")) {
            ElectionApiImpl(it).getElectionAvailability().requireResult()
        }
        assertEquals(1, campaign.id)
        assertEquals("Open", campaign.status)
        assertEquals(Instant.parse("2026-01-04T21:00:00Z"), campaign.semesterStart)
        assertEquals(Instant.parse("2026-06-29T21:00:00Z"), campaign.semesterEnd)
        assertEquals(Instant.parse("2026-01-06T07:00:00Z"), campaign.dateStart)
        assertEquals(Instant.parse("2026-01-07T15:30:00Z"), campaign.dateEnd)
        assertEquals("10:00", campaign.timeStart)
        assertEquals("18:30", campaign.timeEnd)
        assertEquals("2025/2026", campaign.studyYear)
        assertEquals(101L, campaign.semesterId)
        assertEquals(2, campaign.semester)
    }

    @Test
    fun disciplinesRetainOpaqueGroupFlowAndCompatibility() = runTest {
        val discipline = myItmoAreaExchange("election/students/available_disciplines", fixture("election/disciplines.json")) {
            ElectionApiImpl(it).getAvailableDisciplines().requireResult()
        }.single()
        assertEquals(11L, discipline.dcId)
        assertEquals(22L, discipline.discId)
        assertEquals(1L, discipline.langId)
        assertEquals("Synthetic Department", discipline.depName)
        assertEquals("Synthetic Discipline", discipline.discName)
        assertEquals("en", discipline.langCode)
        assertTrue(discipline.required)
        assertEquals("Synthetic description", discipline.description)
        assertEquals("SYN", discipline.depNameShort)
        assertEquals(listOf(23L, 24L), discipline.notCompatibleWith)
        assertEquals(AvailableDisciplineSemester(2, 1, "00a /+&?", "Available"), discipline.semesters.single())
    }

    @Test
    fun validationPostsOrderedOpaqueStringsAndRetainsAllRuleCounts() = runTest {
        val ids = listOf("00a /+&?", "00a /+&?", "000b")
        val validation = myItmoAreaExchange("election/students/group_flow_available_disciplines", fixture("election/validation.json"),
            method = HttpMethod.Post, body = "[\"00a /+&?\",\"00a /+&?\",\"000b\"]") {
            ElectionApiImpl(it).validateSelectedDisciplines(ids).requireResult()
        }
        assertEquals(DisciplineSelectionValidation(listOf(22, 23), true, false, 1, 2, 3, true), validation)
    }

    @Test
    fun limitsKeepOpaqueKeysAndStudentCapacities() = runTest {
        val limits = myItmoAreaExchange("election/students/limits/flows", fixture("election/limits.json")) {
            ElectionApiImpl(it).getFlowLimits().requireResult()
        }
        assertEquals(setOf("00a /+&?"), limits.keys)
        assertEquals(FlowLimit(20, 17, 3), limits.getValue("00a /+&?"))
        assertTrue(myItmoAreaExchange("election/students/limits/flows", fixture("election/empty-limits.json")) {
            ElectionApiImpl(it).getFlowLimits().requireResult()
        }.isEmpty())
    }

    @Test
    fun orderedChainsRetainRecursiveModernFlowsWithoutDeprecatedModels() = runTest {
        val chain = myItmoAreaExchange("election/students/ordered_flow_chains", fixture("election/chains.json")) {
            ElectionApiImpl(it).getOrderedFlowChains().requireResult()
        }.single()
        assertEquals("00a /+&?", chain.groupFlow)
        assertEquals(22L, chain.disciplineId)
        assertEquals("Synthetic Discipline", chain.disciplineName)
        val flow = chain.flows.single()
        assertEquals(101L, flow.id)
        assertEquals("Synthetic flow", flow.name)
        assertEquals(20L, flow.limitMax)
        assertEquals(listOf("Synthetic Teacher"), flow.teachers)
        assertEquals(1, flow.workType)
        assertTrue(flow.available)
        assertEquals(listOf(1, 3), flow.selections)
        assertEquals(ElectionFlow(102, "Alternative", null, emptyList(), emptyList(), 2, false, emptyList()), flow.variants.single())
    }

    @Test
    fun chosenFlowIdsKeepOrderAndEmptyCollectionsRemainEmpty() = runTest {
        assertEquals(listOf(101L, 102L), myItmoAreaExchange("election/students/chosen_flows", fixture("election/chosen.json")) {
            ElectionApiImpl(it).getChosenFlows().requireResult()
        })
        for (path in listOf("available_disciplines", "ordered_flow_chains", "chosen_flows")) {
            val result = myItmoAreaExchange("election/students/$path", fixture("election/empty.json")) {
                val api = ElectionApiImpl(it)
                when (path) {
                    "available_disciplines" -> api.getAvailableDisciplines().requireResult()
                    "ordered_flow_chains" -> api.getOrderedFlowChains().requireResult()
                    else -> api.getChosenFlows().requireResult()
                }
            }
            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun orderChangeAndDisciplineSelectionPostExactJsonArrays() = runTest {
        val flowResult = myItmoAreaExchange("election/students/order/change", fixture("election/change-flows.json"),
            method = HttpMethod.Post, body = "[101,102,101]") {
            ElectionApiImpl(it).changeSelectedFlows(listOf(101, 102, 101)).result
        }
        assertEquals(JsonArray(listOf(JsonPrimitive(101), JsonPrimitive(102))), flowResult!!.jsonObject["flows"])
        val disciplineResult = myItmoAreaExchange("election/students/order/", fixture("election/change-disciplines.json"),
            method = HttpMethod.Post, body = "[\"00a /+&?\",\"000b\"]") {
            ElectionApiImpl(it).changeSelectedDisciplines(listOf("00a /+&?", "000b")).result
        }
        assertEquals(JsonPrimitive("Synthetic discipline"), disciplineResult!!.jsonObject["name"])
    }

    @Test
    fun allOrderPostsPreserveArbitraryObjectsArraysScalarsAndNullWithoutBodyOnClear() = runTest {
        for (case in listOf("order-object", "order-array", "order-scalar", "clear")) {
            val expected = ItmoApiJson.parseToJsonElement(fixture("election/$case.json")).jsonObject["result"]
            for (operation in listOf("change", "clear", "")) {
                val result = myItmoAreaExchange("election/students/order/$operation", fixture("election/$case.json"),
                    method = HttpMethod.Post, body = if (operation == "clear") null else "[]") {
                    val api = ElectionApiImpl(it)
                    when (operation) {
                        "change" -> api.changeSelectedFlows(emptyList())
                        "clear" -> api.clearAllSelectedFlows()
                        else -> api.changeSelectedDisciplines(emptyList())
                    }
                }
                assertEquals(if (expected == JsonNull) null else expected, result.result)
            }
        }
    }

    @Test
    fun everyEndpointMapsHttp400Api100WithoutLosingServerCode() = runTest {
        val operations: List<Pair<String, suspend (ElectionApi) -> Unit>> = listOf(
            "availability" to { it.getElectionAvailability() },
            "available_disciplines" to { it.getAvailableDisciplines() },
            "group_flow_available_disciplines" to { it.validateSelectedDisciplines(emptyList()) },
            "limits/flows" to { it.getFlowLimits() },
            "ordered_flow_chains" to { it.getOrderedFlowChains() },
            "chosen_flows" to { it.getChosenFlows() },
            "order/change" to { it.changeSelectedFlows(emptyList()) },
            "order/clear" to { it.clearAllSelectedFlows() },
            "order/" to { it.changeSelectedDisciplines(emptyList()) },
        )
        for ((path, operation) in operations) {
            val post = path.startsWith("order/") || path.startsWith("group_flow")
            val failure = assertFailsWith<MyItmoException.Api> {
                myItmoAreaExchange("election/students/$path", fixture("election/error.json"),
                    method = if (post) HttpMethod.Post else HttpMethod.Get,
                    body = if (post && path != "order/clear") "[]" else null, status = 400) {
                    operation(ElectionApiImpl(it))
                }
            }
            assertEquals(400, failure.status)
            assertEquals(100, failure.errorCode)
        }
    }
}
