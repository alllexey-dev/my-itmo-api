package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.myItmoAreaExchange
import dev.alllexey.itmoapi.myitmo.system.*
import dev.alllexey.itmoapi.testing.fixture
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.*

class SystemParityTest {
    @Test
    fun `dashboard full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.system.DashboardItem>>, ResultResponse<List<DashboardItem>>>(
            "system/dashboard.json", ResultResponse.serializer(ListSerializer(DashboardItem.serializer())),
        )
    }

    @Test
    fun `items full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.system.MenuResponse>, ResultResponse<MenuResponse>>(
            "system/items.json", ResultResponse.serializer(MenuResponse.serializer()),
        )
    }

    @Test
    fun `services full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.system.MenuResponse>, ResultResponse<MenuResponse>>(
            "system/services.json", ResultResponse.serializer(MenuResponse.serializer()),
        )
    }

    @Test
    fun `empty-dashboard full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.system.DashboardItem>>, ResultResponse<List<DashboardItem>>>(
            "system/empty-dashboard.json", ResultResponse.serializer(ListSerializer(DashboardItem.serializer())),
        )
    }

    @Test
    fun `empty-menu full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.system.MenuResponse>, ResultResponse<MenuResponse>>(
            "system/empty-menu.json", ResultResponse.serializer(MenuResponse.serializer()),
        )
    }

    @Test
    fun `error full wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.system.MenuResponse>, ResultResponse<MenuResponse>>(
            "system/error.json", ResultResponse.serializer(MenuResponse.serializer()),
        )
    }

    private fun legacyApi(): api.myitmo.MyItmoApi = Retrofit.Builder().baseUrl("https://example.invalid/custom/base/")
        .addConverterFactory(GsonConverterFactory.create(api.myitmo.MyItmo().gson))
        .build().create(api.myitmo.MyItmoApi::class.java)

    @Test
    fun `production getDashboard request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getDashboard().request()
        myItmoAreaExchange("system/dashboard", fixture("system/dashboard.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            SystemApiImpl(it).getDashboard()
        }
    }

    @Test
    fun `production getMenuItems request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getMenuItems().request()
        myItmoAreaExchange("system/v1/menu/items", fixture("system/items.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            SystemApiImpl(it).getMenuItems()
        }
    }

    @Test
    fun `production getServices request matches pinned legacy Retrofit`() = runTest {
        val legacy = legacyApi().getServices().request()
        myItmoAreaExchange("system/v1/menu/services", fixture("system/services.json"), method = HttpMethod.Get, body = null, inspect = { assertLegacyAreaRequest(legacy, it) }) {
            SystemApiImpl(it).getServices()
        }
    }

}
