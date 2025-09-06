package api.myitmo;

import api.myitmo.model.Response;
import api.myitmo.model.Schedule;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.time.LocalDate;
import java.util.List;

public interface MyItmoApi {

    @GET("/api/schedule/schedule/personal")
    Response<List<Schedule>> getPersonalSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd);
}
