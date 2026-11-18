package dev.alllexey.itmoapi.myitmo.system

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer

/** Typed MyITMO system operations. */
public interface SystemApi {
    /** GET /api/system/dashboard; Saved dashboard grid layout. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getDashboard(): List<DashboardItem>

    /** GET /api/system/v1/menu/items; Navigation menu, including nested sections. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getMenuItems(): MenuResponse

    /** GET /api/system/v1/menu/services; Service catalog available to the current user. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getServices(): MenuResponse

}

internal class SystemApiImpl(private val transport: ItmoTransport) : SystemApi {
    override suspend fun getDashboard(): List<DashboardItem> = transport.result(
        ListSerializer(DashboardItem.serializer()), HttpMethod.Get, "api/system/dashboard",
    )

    override suspend fun getMenuItems(): MenuResponse = transport.result(
        MenuResponse.serializer(), HttpMethod.Get, "api/system/v1/menu/items",
    )

    override suspend fun getServices(): MenuResponse = transport.result(
        MenuResponse.serializer(), HttpMethod.Get, "api/system/v1/menu/services",
    )

}
