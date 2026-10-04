package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.ResultResponse
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.*

class SportVenueModelTest {
    @Test
    fun onlineKeepsAbsentBuildingAndExplicitOnlineRoom() {
        val lesson = ItmoApiJson.decodeFromString<SportLesson>("""{"building_id":null,"room_id":-1,"room_name":"Online"}""")
        assertNull(lesson.buildingId)
        assertEquals(-1L, lesson.roomId)
        assertEquals("Online", lesson.roomName)
    }

    @Test
    fun externalVenueIsNotReplacedWithOtherFilterCategory() {
        val filters = ItmoApiJson.decodeFromString<SportFilters>("""{"building_id":[{"id":0,"value":"Other venues"}]}""")
        val lesson = ItmoApiJson.decodeFromString<SportLesson>("""{"building_id":335,"room_id":20013,"room_name":"External venue"}""")
        assertEquals(0L, filters.buildingId.single().id)
        assertEquals(335L, lesson.buildingId)
        assertEquals(20013L, lesson.roomId)
        val fixtureLesson = ItmoApiJson.decodeFromString(ResultResponse.serializer(ListSerializer(SportSchedule.serializer())), fixture("sport/external-venue.json"))
            .requireResult().single().lessons!!.single()
        assertEquals(lesson.buildingId, fixtureLesson.buildingId)
        assertEquals(lesson.roomId, fixtureLesson.roomId)
    }
}
