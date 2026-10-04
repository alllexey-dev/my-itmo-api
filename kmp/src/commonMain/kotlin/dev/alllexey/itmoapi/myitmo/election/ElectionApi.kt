package dev.alllexey.itmoapi.myitmo.election

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.ResultResponse
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement

/** Typed MyITMO election operations; callers own authentication and result unwrapping. */
public interface ElectionApi {
    /** GET /api/election/students/availability; Campaign status and opening/closing instants. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getElectionAvailability(): ResultResponse<ElectionAvailability>

    /** GET /api/election/students/available_disciplines; Available elective disciplines and their semester identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getAvailableDisciplines(): ResultResponse<List<AvailableDiscipline>>

    /** POST /api/election/students/group_flow_available_disciplines; Checks completeness and compatibility using opaque groupFlow identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun validateSelectedDisciplines(groupFlowIds: List<String>): ResultResponse<DisciplineSelectionValidation>

    /** GET /api/election/students/limits/flows; Current flow capacity keyed by opaque server identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getFlowLimits(): ResultResponse<Map<String, FlowLimit>>

    /** GET /api/election/students/ordered_flow_chains; Selected disciplines and their recursive available flow trees. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getOrderedFlowChains(): ResultResponse<List<ElectionFlowChain>>

    /** GET /api/election/students/chosen_flows; Already selected flow identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getChosenFlows(): ResultResponse<List<Long>>

    /** POST /api/election/students/order/change; Replaces selected flows with the ordered identifiers; result shape is not guaranteed. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun changeSelectedFlows(flowIds: List<Long>): ResultResponse<JsonElement?>

    /** POST /api/election/students/order/clear; Clears the current flow selection; result may be null or arbitrary JSON. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun clearAllSelectedFlows(): ResultResponse<JsonElement?>

    /** POST /api/election/students/order/; Replaces selected disciplines using opaque groupFlow identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun changeSelectedDisciplines(groupFlowIds: List<String>): ResultResponse<JsonElement?>

}

internal class ElectionApiImpl(private val transport: ItmoTransport) : ElectionApi {
    override suspend fun getElectionAvailability(): ResultResponse<ElectionAvailability> = transport.execute(
        ResultResponse.serializer(ElectionAvailability.serializer()), HttpMethod.Get, "api/election/students/availability",
    ) {
        url { encodedPath = "/api/election/students/availability" }
    }

    override suspend fun getAvailableDisciplines(): ResultResponse<List<AvailableDiscipline>> = transport.execute(
        ResultResponse.serializer(ListSerializer(AvailableDiscipline.serializer())), HttpMethod.Get, "api/election/students/available_disciplines",
    ) {
        url { encodedPath = "/api/election/students/available_disciplines" }
    }

    override suspend fun validateSelectedDisciplines(groupFlowIds: List<String>): ResultResponse<DisciplineSelectionValidation> = transport.execute(
        ResultResponse.serializer(DisciplineSelectionValidation.serializer()), HttpMethod.Post, "api/election/students/group_flow_available_disciplines",
    ) {
        url { encodedPath = "/api/election/students/group_flow_available_disciplines" }
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(String.serializer()), groupFlowIds))
    }

    override suspend fun getFlowLimits(): ResultResponse<Map<String, FlowLimit>> = transport.execute(
        ResultResponse.serializer(MapSerializer(String.serializer(), FlowLimit.serializer())), HttpMethod.Get, "api/election/students/limits/flows",
    ) {
        url { encodedPath = "/api/election/students/limits/flows" }
    }

    override suspend fun getOrderedFlowChains(): ResultResponse<List<ElectionFlowChain>> = transport.execute(
        ResultResponse.serializer(ListSerializer(ElectionFlowChain.serializer())), HttpMethod.Get, "api/election/students/ordered_flow_chains",
    ) {
        url { encodedPath = "/api/election/students/ordered_flow_chains" }
    }

    override suspend fun getChosenFlows(): ResultResponse<List<Long>> = transport.execute(
        ResultResponse.serializer(ListSerializer(Long.serializer())), HttpMethod.Get, "api/election/students/chosen_flows",
    ) {
        url { encodedPath = "/api/election/students/chosen_flows" }
    }

    override suspend fun changeSelectedFlows(flowIds: List<Long>): ResultResponse<JsonElement?> = transport.execute(
        ResultResponse.serializer(JsonElement.serializer().nullable), HttpMethod.Post, "api/election/students/order/change",
    ) {
        url { encodedPath = "/api/election/students/order/change" }
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(Long.serializer()), flowIds))
    }

    override suspend fun clearAllSelectedFlows(): ResultResponse<JsonElement?> = transport.execute(
        ResultResponse.serializer(JsonElement.serializer().nullable), HttpMethod.Post, "api/election/students/order/clear",
    ) {
        url { encodedPath = "/api/election/students/order/clear" }
    }

    override suspend fun changeSelectedDisciplines(groupFlowIds: List<String>): ResultResponse<JsonElement?> = transport.execute(
        ResultResponse.serializer(JsonElement.serializer().nullable), HttpMethod.Post, "api/election/students/order/",
    ) {
        url { encodedPath = "/api/election/students/order/" }
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(String.serializer()), groupFlowIds))
    }

}
