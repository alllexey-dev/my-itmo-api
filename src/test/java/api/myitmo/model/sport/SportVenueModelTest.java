package api.myitmo.model.sport;

import api.myitmo.MyItmo;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SportVenueModelTest {
    /** The client's own Gson: a bare one cannot reach the java.time fields these models declare. */
    private final Gson gson = new MyItmo().getGson();

    @Test
    void onlineKeepsAbsentBuildingAndExplicitOnlineRoom() {
        SportLesson lesson = gson.fromJson("{\"building_id\":null,\"room_id\":-1,\"room_name\":\"Online\"}", SportLesson.class);
        assertNull(lesson.getBuildingId());
        assertEquals(Long.valueOf(-1), lesson.getRoomId());
        assertEquals("Online", lesson.getRoomName());
    }

    @Test
    void externalVenueIsNotReplacedWithOtherFilterCategory() {
        SportFilters filters = gson.fromJson("{\"building_id\":[{\"id\":0,\"value\":\"Other venues\"}]}", SportFilters.class);
        SportLesson lesson = gson.fromJson("{\"building_id\":335,\"room_id\":20013,\"room_name\":\"External venue\"}", SportLesson.class);
        assertEquals(0, filters.getBuildingId().get(0).getId());
        assertEquals(Long.valueOf(335), lesson.getBuildingId());
        assertEquals(Long.valueOf(20013), lesson.getRoomId());
    }
}
