package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.testing.mockExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*
import kotlin.time.Instant

class FinancesApiTest {
    @Test
    fun inclusiveDateBoundsAreZeroPaddedIndependentlyAndCategoryWhitespaceIsUnmodified() = runTest {
        for ((from, to) in listOf(null to null, LocalDate(2026, 1, 5) to null,
            null to LocalDate(2026, 1, 9), LocalDate(2026, 1, 5) to LocalDate(2026, 1, 9))) {
            val query = buildMap {
                if (from != null) put("dateFrom", listOf("2026-01-05"))
                if (to != null) put("dateTo", listOf("2026-01-09"))
            }
            val total = mockExchange("/api/finances/scholarship/total", fixture("finances/totals.json"), query) {
                FinancesApiImpl(it).getScholarshipTotals(from, to).requireResult()
            }.single()
            assertEquals(ScholarshipTotal(901, " \nSynthetic scholarship\n ", 12345), total)
        }
    }

    @Test
    fun emptyTotalsAndHttp400Api100ArePreserved() = runTest {
        assertTrue(mockExchange("/api/finances/scholarship/total", fixture("finances/empty.json")) {
            FinancesApiImpl(it).getScholarshipTotals().requireResult()
        }.isEmpty())
        val failure = assertFailsWith<MyItmoException.Api> {
            mockExchange("/api/finances/scholarship/total", fixture("finances/error.json"), status = 400) {
                FinancesApiImpl(it).getScholarshipTotals()
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
    }

    @Test
    fun tuitionAvailabilityIsAPlainBooleanAndFalseForStateFundedStudents() = runTest {
        for ((name, expected) in listOf("tuition-available" to true, "tuition-unavailable" to false)) {
            val available: Boolean = mockExchange("/api/finances/edupayments/availability", fixture("finances/$name.json")) {
                FinancesApiImpl(it).getTuitionAvailability().requireResult()
            }
            assertEquals(expected, available)
        }
    }

    @Test
    fun tuitionContractsKeepNumberYearProgramAndPaymentSize() = runTest {
        val contract = mockExchange("/api/finances/edupayments/contracts", fixture("finances/tuition-contracts.json")) {
            FinancesApiImpl(it).getTuitionContracts().requireResult()
        }.single()
        assertEquals(
            TuitionContract(
                contractId = "00-00000002",
                number = "T-0000-0002",
                dateStart = Instant.parse("2026-08-31T21:00:00Z"),
                dateEnd = Instant.parse("2027-08-30T21:00:00Z"),
                active = true,
                program = "Синтетическая образовательная программа",
                period = "семестр",
                sum = 100000.5,
            ),
            contract,
        )
    }

    @Test
    fun stateFundedStudentsGetEmptyOrNullTuitionContracts() = runTest {
        for (name in listOf("empty", "null-result")) {
            val response = mockExchange("/api/finances/edupayments/contracts", fixture("finances/$name.json")) {
                FinancesApiImpl(it).getTuitionContracts()
            }
            assertEquals(0, response.errorCode)
            assertTrue(response.result.orEmpty().isEmpty())
        }
    }

    @Test
    fun tuitionPaymentsKeepEachAgreementsBalanceAndSchedule() = runTest {
        val contract = mockExchange("/api/finances/edupayments/payments", fixture("finances/tuition-payments.json")) {
            FinancesApiImpl(it).getTuitionPayments().requireResult()
        }.single()
        assertEquals("00-00000002", contract.contractId)
        assertEquals("T-0000-0002", contract.number)
        assertEquals("Синтетический договор об обучении", contract.name)
        assertEquals(Instant.parse("2026-08-31T21:00:00Z"), contract.dateStart)
        assertEquals(Instant.parse("2027-08-30T21:00:00Z"), contract.dateEnd)
        assertTrue(contract.active)
        assertEquals(0.0, contract.balance)
        assertEquals(
            listOf(
                TuitionPayment(100000.5, 100000.5, Instant.parse("2026-09-14T21:00:00Z"), "1 семестр"),
                TuitionPayment(100000.0, 0.0, Instant.parse("2027-02-14T21:00:00Z"), "2 семестр"),
            ),
            contract.payments,
        )
    }

    @Test
    fun stateFundedStudentsGetEmptyOrNullTuitionPayments() = runTest {
        for (name in listOf("empty", "null-result")) {
            val response = mockExchange("/api/finances/edupayments/payments", fixture("finances/$name.json")) {
                FinancesApiImpl(it).getTuitionPayments()
            }
            assertEquals(0, response.errorCode)
            assertTrue(response.result.orEmpty().isEmpty())
        }
    }
}
