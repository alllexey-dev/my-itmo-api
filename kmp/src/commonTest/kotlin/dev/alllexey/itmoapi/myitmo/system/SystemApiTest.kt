package dev.alllexey.itmoapi.myitmo.system

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.election.myItmoAreaExchange
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SystemApiTest {
    @Test
    fun dashboardKeepsGridConstraintsAndObservedOptionalFields() = runTest {
        val dashboard = myItmoAreaExchange("system/dashboard", fixture("system/dashboard.json")) {
            SystemApiImpl(it).getDashboard().requireResult()
        }
        assertEquals(DashboardItem("synthetic-widget", 1, 2, 3, 4, "synthetic", true, 1, 6, 2, 8,
            false, true, false, ".handle", ".button", ".locked"), dashboard.first())
        assertEquals(DashboardItem("minimal", 0, 0, 1, 1, "synthetic-minimal", false), dashboard.last())
    }

    @Test
    fun navigationRetainsNestedLeafAndServiceCatalogColor() = runTest {
        val section = myItmoAreaExchange("system/v1/menu/items", fixture("system/items.json")) {
            SystemApiImpl(it).getMenuItems().requireResult()
        }.menu.single()
        assertEquals("Synthetic section", section.title)
        assertNull(section.color)
        assertFalse(section.exact)
        val leaf = section.children!!.single()
        assertEquals(MenuItem("Synthetic leaf", "Leaf description", "/section/leaf", "leaf", "leaf-color", true), leaf)
        val service = myItmoAreaExchange("system/v1/menu/services", fixture("system/services.json")) {
            SystemApiImpl(it).getServices().requireResult()
        }.menu.single()
        assertEquals(MenuItem("Synthetic service", "Service description", "https://example.invalid/service",
            "service", "service-color", false, "#123456", emptyList()), service)
    }

    @Test
    fun eachEndpointPreservesEmptyCollectionsAndHttp400Api100() = runTest {
        for (path in listOf("dashboard", "v1/menu/items", "v1/menu/services")) {
            val empty = myItmoAreaExchange("system/$path", fixture(if (path == "dashboard") "system/empty-dashboard.json" else "system/empty-menu.json")) {
                val api = SystemApiImpl(it)
                when (path) {
                    "dashboard" -> api.getDashboard().requireResult()
                    "v1/menu/items" -> api.getMenuItems().requireResult().menu
                    else -> api.getServices().requireResult().menu
                }
            }
            assertTrue(empty.isEmpty())
            val failure = assertFailsWith<MyItmoException.Api> {
                myItmoAreaExchange("system/$path", fixture("system/error.json"), status = 400) {
                    val api = SystemApiImpl(it)
                    when (path) {
                        "dashboard" -> api.getDashboard()
                        "v1/menu/items" -> api.getMenuItems()
                        else -> api.getServices()
                    }
                }
            }
            assertEquals(400, failure.status)
            assertEquals(100, failure.errorCode)
        }
    }
}
