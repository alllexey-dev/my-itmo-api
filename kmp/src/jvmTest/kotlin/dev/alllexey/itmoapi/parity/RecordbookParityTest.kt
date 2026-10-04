package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.recordbook.*
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test

class RecordbookParityTest {
    @Test
    fun `record-book wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.recordbook.RecordBookEntry>>, ResultResponse<List<RecordBookEntry>>>(
            "recordbook/record-book.json",
            ResultResponse.serializer(ListSerializer(RecordBookEntry.serializer())),
        )
    }

    @Test
    fun `specializations wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.recordbook.Specialization>>, ResultResponse<List<Specialization>>>(
            "recordbook/specializations.json",
            ResultResponse.serializer(ListSerializer(Specialization.serializer())),
        )
    }

    @Test
    fun `controls wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.recordbook.ControlEntry>>, ResultResponse<List<ControlEntry>>>(
            "recordbook/controls.json",
            ResultResponse.serializer(ListSerializer(ControlEntry.serializer())),
        )
    }

    @Test
    fun `absence wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.recordbook.RecordBookEntry>>, ResultResponse<List<RecordBookEntry>>>(
            "recordbook/absence.json",
            ResultResponse.serializer(ListSerializer(RecordBookEntry.serializer())),
        )
    }

    @Test
    fun `empty wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<List<api.myitmo.model.recordbook.RecordBookEntry>>, ResultResponse<List<RecordBookEntry>>>(
            "recordbook/empty.json",
            ResultResponse.serializer(ListSerializer(RecordBookEntry.serializer())),
        )
    }
}
