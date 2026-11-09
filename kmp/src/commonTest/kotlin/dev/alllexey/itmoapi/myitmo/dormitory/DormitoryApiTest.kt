package dev.alllexey.itmoapi.myitmo.dormitory

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import dev.alllexey.itmoapi.testing.mockExchange
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

class DormitoryApiTest {
    @Test
    fun fullStatusKeepsStageDormitoryAndHtmlInstructions() = runTest {
        val status = mockExchange("/api/dormitory/status/full", fixture("dormitory/status.json")) {
            val api: DormitoryApi = DormitoryApiImpl(it)
            api.getDormitoryStatus().requireResult()
        }
        assertEquals(4, status.statusId)
        assertNull(status.queuePlace)
        assertEquals("test", status.dormType)
        assertEquals(
            AssignedDormitory(9001, "Синтетическое общежитие", "Тестовая ул., 1", "<p>Синтетическая <b>инструкция</b></p>"),
            status.assignedDormitory,
        )
    }

    @Test
    fun queuedStatusHasAPlaceAndNoDormitory() = runTest {
        val status = mockExchange("/api/dormitory/status/full", fixture("dormitory/status-queue.json")) {
            DormitoryApiImpl(it).getDormitoryStatus().requireResult()
        }
        assertEquals(DormitoryStatus(statusId = 1, queuePlace = 42, dormType = "test", assignedDormitory = null), status)
    }

    @Test
    fun noStatusIsASuccessfulNullResultAndErrorsStayApiErrors() = runTest {
        val response = mockExchange("/api/dormitory/status/full", fixture("dormitory/status-empty.json")) {
            DormitoryApiImpl(it).getDormitoryStatus()
        }
        assertEquals(0, response.errorCode)
        assertNull(response.result)
        val failure = assertFailsWith<MyItmoException.Api> {
            mockExchange("/api/dormitory/status/full", fixture("dormitory/error.json"), status = 400) {
                DormitoryApiImpl(it).getDormitoryStatus()
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
    }

    @Test
    fun agreementPeriodsAreAcademicYearsWithCalendarDateBounds() = runTest {
        val periods = mockExchange("/api/dormitory/payments/contracts/periods", fixture("dormitory/periods.json")) {
            DormitoryApiImpl(it).getDormitoryContractPeriods().requireResult()
        }
        assertEquals(
            listOf(
                DormitoryContractPeriod(2025, LocalDate(2025, 9, 1), LocalDate(2026, 8, 31)),
                DormitoryContractPeriod(2026, LocalDate(2026, 9, 1), LocalDate(2027, 8, 31)),
            ),
            periods,
        )
        assertTrue(mockExchange("/api/dormitory/payments/contracts/periods", fixture("dormitory/periods-empty.json")) {
            DormitoryApiImpl(it).getDormitoryContractPeriods().requireResult()
        }.isEmpty())
    }

    @Test
    fun contractsForAPeriodSendItsBoundsAndKeepBalanceAndPaymentSchedule() = runTest {
        val query = mapOf("from" to listOf("2026-09-01"), "to" to listOf("2027-08-31"))
        val contract = mockExchange("/api/dormitory/payments/contracts", fixture("dormitory/contracts.json"), query) {
            DormitoryApiImpl(it).getDormitoryContracts(LocalDate(2026, 9, 1), LocalDate(2027, 8, 31)).requireResult()
        }.single()
        assertEquals("00-00000001", contract.contractId)
        assertEquals("0000.00.00.0001", contract.number)
        assertEquals("Синтетический договор найма", contract.name)
        assertEquals(Instant.parse("2026-08-31T21:00:00Z"), contract.dateStart)
        assertEquals(Instant.parse("2027-06-29T21:00:00Z"), contract.dateEnd)
        assertTrue(contract.active)
        assertEquals(-1500.0, contract.balance)
        assertEquals(
            listOf(
                DormitoryPayment(1500.5, 1500.5, Instant.parse("2026-09-09T21:00:00Z"), "Сентябрь 2026"),
                DormitoryPayment(1500.0, 0.0, Instant.parse("2026-10-09T21:00:00Z"), "Октябрь 2026"),
            ),
            contract.payments,
        )
        assertTrue(mockExchange("/api/dormitory/payments/contracts", fixture("dormitory/contracts-empty.json"), query) {
            DormitoryApiImpl(it).getDormitoryContracts(LocalDate(2026, 9, 1), LocalDate(2027, 8, 31)).requireResult()
        }.isEmpty())
    }

    @Test
    fun settlementDocumentsKeepTypeStatusLabelAndRequestTemplate() = runTest {
        val documents = mockExchange("/api/dormitory/documents", fixture("dormitory/documents.json")) {
            DormitoryApiImpl(it).getDormitoryDocuments().requireResult()
        }
        assertEquals(
            listOf(
                DormitoryDocument(7001, "Синтетическая справка", 1, "Отсутствует", 8001),
                DormitoryDocument(7002, "Синтетический анализ", 2, "Подтверждено", 8002),
            ),
            documents,
        )
        assertTrue(mockExchange("/api/dormitory/documents", fixture("dormitory/documents-empty.json")) {
            DormitoryApiImpl(it).getDormitoryDocuments().requireResult()
        }.isEmpty())
    }
}
