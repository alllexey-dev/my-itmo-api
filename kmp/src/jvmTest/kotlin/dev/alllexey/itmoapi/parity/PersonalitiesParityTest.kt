package dev.alllexey.itmoapi.parity

import dev.alllexey.itmoapi.core.*
import dev.alllexey.itmoapi.myitmo.personalities.*
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test

class PersonalitiesParityTest {
    @Test
    fun `person wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.personality.Personality>, ResultResponse<Personality>>(
            "personalities/person.json",
            ResultResponse.serializer(Personality.serializer()),
        )
    }

    @Test
    fun `student wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.personality.Personality>, ResultResponse<Personality>>(
            "personalities/student.json",
            ResultResponse.serializer(Personality.serializer()),
        )
    }

    @Test
    fun `employee wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.personality.Personality>, ResultResponse<Personality>>(
            "personalities/employee.json",
            ResultResponse.serializer(Personality.serializer()),
        )
    }

    @Test
    fun `service wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.personality.Personality>, ResultResponse<Personality>>(
            "personalities/service.json",
            ResultResponse.serializer(Personality.serializer()),
        )
    }

    @Test
    fun `missing-person wire tree`() {
        assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.personality.Personality>, ResultResponse<Personality>>(
            "personalities/missing-person.json",
            ResultResponse.serializer(Personality.serializer()),
        )
    }

    @Test
    fun `search wire trees`() {
        for (path in listOf("personalities/search.json", "personalities/search-empty.json")) {
            assertParity<api.myitmo.model.ResultResponse<api.myitmo.model.CountWrapper<List<api.myitmo.model.personality.PersonalityMin>>>, ResultResponse<CountWrapper<List<PersonalityMin>>>>(
                path, ResultResponse.serializer(CountWrapper.serializer(ListSerializer(PersonalityMin.serializer()))),
            )
        }
    }
}
