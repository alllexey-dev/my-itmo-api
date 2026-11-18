package dev.alllexey.itmoapi.myitmo.election

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** Typed MyITMO election operations. */
public interface ElectionApi {
    /** GET /api/election/students/availability; Campaign status and opening/closing instants. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getElectionAvailability(): ElectionAvailability

    /** GET /api/election/students/available_disciplines; Available elective disciplines and their semester identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getAvailableDisciplines(): List<AvailableDiscipline>

    /** POST /api/election/students/group_flow_available_disciplines; Checks completeness and compatibility using opaque groupFlow identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun validateSelectedDisciplines(groupFlowIds: List<String>): DisciplineSelectionValidation

    /** GET /api/election/students/limits/flows; Current flow capacity keyed by opaque server identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getFlowLimits(): Map<String, FlowLimit>

    /** GET /api/election/students/ordered_flow_chains; Selected disciplines and their recursive available flow trees. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getOrderedFlowChains(): List<ElectionFlowChain>

    /** GET /api/election/students/chosen_flows; Already selected flow identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getChosenFlows(): List<Long>

    /** POST /api/election/students/order/change; Replaces selected flows with the ordered identifiers. The result has no stable shape and is ignored. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun changeSelectedFlows(flowIds: List<Long>): Unit

    /** POST /api/election/students/order/clear; Clears the current flow selection. The result (null or arbitrary JSON) is ignored. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun clearAllSelectedFlows(): Unit

    /** POST /api/election/students/order/; Replaces selected disciplines using opaque groupFlow identifiers. */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun changeSelectedDisciplines(groupFlowIds: List<String>): Unit

}

internal class ElectionApiImpl(private val transport: ItmoTransport) : ElectionApi {
    override suspend fun getElectionAvailability(): ElectionAvailability = transport.result(
        ElectionAvailability.serializer(), HttpMethod.Get, "api/election/students/availability",
    )

    override suspend fun getAvailableDisciplines(): List<AvailableDiscipline> = transport.result(
        ListSerializer(AvailableDiscipline.serializer()), HttpMethod.Get, "api/election/students/available_disciplines",
    )

    override suspend fun validateSelectedDisciplines(groupFlowIds: List<String>): DisciplineSelectionValidation = transport.result(
        DisciplineSelectionValidation.serializer(), HttpMethod.Post, "api/election/students/group_flow_available_disciplines",
    ) {
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(String.serializer()), groupFlowIds))
    }

    override suspend fun getFlowLimits(): Map<String, FlowLimit> = transport.result(
        MapSerializer(String.serializer(), FlowLimit.serializer()), HttpMethod.Get, "api/election/students/limits/flows",
    )

    override suspend fun getOrderedFlowChains(): List<ElectionFlowChain> = transport.result(
        ListSerializer(ElectionFlowChain.serializer()), HttpMethod.Get, "api/election/students/ordered_flow_chains",
    )

    override suspend fun getChosenFlows(): List<Long> = transport.result(
        ListSerializer(Long.serializer()), HttpMethod.Get, "api/election/students/chosen_flows",
    )

    override suspend fun changeSelectedFlows(flowIds: List<Long>): Unit = transport.send(
        HttpMethod.Post, "api/election/students/order/change",
    ) {
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(Long.serializer()), flowIds))
    }

    override suspend fun clearAllSelectedFlows(): Unit = transport.send(
        HttpMethod.Post, "api/election/students/order/clear",
    )

    override suspend fun changeSelectedDisciplines(groupFlowIds: List<String>): Unit = transport.send(
        HttpMethod.Post, "api/election/students/order/",
    ) {
        contentType(ContentType.Application.Json)
        setBody(ItmoApiJson.encodeToString(ListSerializer(String.serializer()), groupFlowIds))
    }

}
