package dev.alllexey.itmoapi.myitmo.requests

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import io.ktor.http.HttpMethod
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer

/** Typed MyITMO requests operations; callers own authentication and result unwrapping. */
public interface RequestsApi {
    /** GET /api/requests/my; Summaries of requests belonging to the current user. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getMyRequests(): ResultResponse<List<RequestSummary>>

}

internal class RequestsApiImpl(private val transport: ItmoTransport) : RequestsApi {
    override suspend fun getMyRequests(): ResultResponse<List<RequestSummary>> = transport.execute(
        ResultResponse.serializer(ListSerializer(RequestSummary.serializer())), HttpMethod.Get, "api/requests/my",
    ) {
        url { encodedPath = "/api/requests/my" }
    }

}
