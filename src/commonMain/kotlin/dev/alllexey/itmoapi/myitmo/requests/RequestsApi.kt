package dev.alllexey.itmoapi.myitmo.requests

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer

/** Typed MyITMO requests operations. */
public interface RequestsApi {
    /** GET /api/requests/my; Summaries of requests belonging to the current user. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getMyRequests(): List<RequestSummary>

}

internal class RequestsApiImpl(private val transport: ItmoTransport) : RequestsApi {
    override suspend fun getMyRequests(): List<RequestSummary> = transport.result(
        ListSerializer(RequestSummary.serializer()), HttpMethod.Get, "api/requests/my",
    )

}
