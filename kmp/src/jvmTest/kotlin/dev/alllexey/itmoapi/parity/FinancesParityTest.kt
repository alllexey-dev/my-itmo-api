package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.myItmoAreaExchange
import dev.alllexey.itmoapi.myitmo.finances.*
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.*
import java.time.LocalDate as JavaDate

class FinancesParityTest {
    @Test
    fun `totals full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.finance.ScholarshipTotal>>, ResultResponse<List<ScholarshipTotal>>>(
            "finances/totals.json", ResultResponse.serializer(ListSerializer(ScholarshipTotal.serializer())),
        )
    }

    @Test
    fun `empty full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.finance.ScholarshipTotal>>, ResultResponse<List<ScholarshipTotal>>>(
            "finances/empty.json", ResultResponse.serializer(ListSerializer(ScholarshipTotal.serializer())),
        )
    }

    @Test
    fun `error full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.finance.ScholarshipTotal>>, ResultResponse<List<ScholarshipTotal>>>(
            "finances/error.json", ResultResponse.serializer(ListSerializer(ScholarshipTotal.serializer())),
        )
    }

    private fun legacyApi(): api.myitmo.MyItmoApi = Retrofit.Builder().baseUrl("https://example.invalid/custom/base/")
        .addConverterFactory(GsonConverterFactory.create(api.myitmo.MyItmo().gson))
        .build().create(api.myitmo.MyItmoApi::class.java)

    @Test
    fun `production date queries match both legacy overloads and independently missing bounds`() = runTest {
        val api = legacyApi()
        for ((from, to) in listOf(null to null, JavaDate.of(2026, 1, 5) to null, null to JavaDate.of(2026, 1, 9), JavaDate.of(2026, 1, 5) to JavaDate.of(2026, 1, 9))) {
            val legacy = (if (from == null && to == null) api.getScholarshipTotals() else api.getScholarshipTotals(from, to)).request()
            val query = legacy.url.queryParameterNames.associateWith { key -> legacy.url.queryParameterValues(key).map { requireNotNull(it) } }
            myItmoAreaExchange("finances/scholarship/total", fixture("finances/totals.json"), query, inspect = { assertLegacyAreaRequest(legacy, it) }) {
                FinancesApiImpl(it).getScholarshipTotals(from?.let { LocalDate(it.year, it.monthValue, it.dayOfMonth) }, to?.let { LocalDate(it.year, it.monthValue, it.dayOfMonth) })
            }
        }
    }
}
