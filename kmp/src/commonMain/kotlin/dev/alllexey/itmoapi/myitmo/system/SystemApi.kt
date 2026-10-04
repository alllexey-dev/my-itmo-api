package dev.alllexey.itmoapi.myitmo.system

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer

/** Typed MyITMO system operations; callers own authentication and result unwrapping. */
public interface SystemApi {
    /** GET /api/system/dashboard; Saved dashboard grid layout. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDashboard(): ResultResponse<List<DashboardItem>>

    /** GET /api/system/v1/menu/items; Navigation menu, including nested sections. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getMenuItems(): ResultResponse<MenuResponse>

    /** GET /api/system/v1/menu/services; Service catalog available to the current user. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getServices(): ResultResponse<MenuResponse>

}

internal class SystemApiImpl(private val transport: ItmoTransport) : SystemApi {
    override suspend fun getDashboard(): ResultResponse<List<DashboardItem>> = transport.execute(
        ResultResponse.serializer(ListSerializer(DashboardItem.serializer())), HttpMethod.Get, "api/system/dashboard",
    ) {
        url { encodedPath = "/api/system/dashboard" }
    }

    override suspend fun getMenuItems(): ResultResponse<MenuResponse> = transport.execute(
        ResultResponse.serializer(MenuResponse.serializer()), HttpMethod.Get, "api/system/v1/menu/items",
    ) {
        url { encodedPath = "/api/system/v1/menu/items" }
    }

    override suspend fun getServices(): ResultResponse<MenuResponse> = transport.execute(
        ResultResponse.serializer(MenuResponse.serializer()), HttpMethod.Get, "api/system/v1/menu/services",
    ) {
        url { encodedPath = "/api/system/v1/menu/services" }
    }

}
