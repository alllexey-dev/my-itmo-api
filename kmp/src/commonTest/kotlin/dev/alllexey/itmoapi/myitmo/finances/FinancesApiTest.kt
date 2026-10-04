package dev.alllexey.itmoapi.myitmo.finances

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.myItmoAreaExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

class FinancesApiTest {
    @Test
    fun inclusiveDateBoundsAreZeroPaddedIndependentlyAndCategoryWhitespaceIsUnmodified() = runTest {
        for ((from, to) in listOf(null to null, LocalDate(2026, 1, 5) to null,
            null to LocalDate(2026, 1, 9), LocalDate(2026, 1, 5) to LocalDate(2026, 1, 9))) {
            val query = buildMap {
                if (from != null) put("dateFrom", listOf("2026-01-05"))
                if (to != null) put("dateTo", listOf("2026-01-09"))
            }
            val total = myItmoAreaExchange("finances/scholarship/total", fixture("finances/totals.json"), query) {
                FinancesApiImpl(it).getScholarshipTotals(from, to).requireResult()
            }.single()
            assertEquals(ScholarshipTotal(901, " \nSynthetic scholarship\n ", 12345), total)
        }
    }

    @Test
    fun emptyTotalsAndHttp400Api100ArePreserved() = runTest {
        assertTrue(myItmoAreaExchange("finances/scholarship/total", fixture("finances/empty.json")) {
            FinancesApiImpl(it).getScholarshipTotals().requireResult()
        }.isEmpty())
        val failure = assertFailsWith<MyItmoException.Api> {
            myItmoAreaExchange("finances/scholarship/total", fixture("finances/error.json"), status = 400) {
                FinancesApiImpl(it).getScholarshipTotals()
            }
        }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
    }
}
