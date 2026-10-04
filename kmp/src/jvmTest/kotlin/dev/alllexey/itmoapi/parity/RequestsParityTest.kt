package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.myItmoAreaExchange
import dev.alllexey.itmoapi.myitmo.requests.*
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.*

class RequestsParityTest {
    @Test
    fun `my full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.requests.RequestSummary>>, ResultResponse<List<RequestSummary>>>(
            "requests/my.json", ResultResponse.serializer(ListSerializer(RequestSummary.serializer())),
        )
    }

    @Test
    fun `empty full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.requests.RequestSummary>>, ResultResponse<List<RequestSummary>>>(
            "requests/empty.json", ResultResponse.serializer(ListSerializer(RequestSummary.serializer())),
        )
    }

    @Test
    fun `error full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.requests.RequestSummary>>, ResultResponse<List<RequestSummary>>>(
            "requests/error.json", ResultResponse.serializer(ListSerializer(RequestSummary.serializer())),
        )
    }

    private fun legacyApi(): api.myitmo.MyItmoApi = Retrofit.Builder().baseUrl("https://example.invalid/custom/base/")
        .addConverterFactory(GsonConverterFactory.create(api.myitmo.MyItmo().gson))
        .build().create(api.myitmo.MyItmoApi::class.java)

    @Test
    fun `production getMyRequests request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getMyRequests().request()
        myItmoAreaExchange("requests/my", fixture("requests/my.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            RequestsApiImpl(it).getMyRequests()
        }
    }

}
