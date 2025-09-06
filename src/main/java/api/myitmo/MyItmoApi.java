package api.myitmo;

import api.myitmo.model.DataResponse;
import api.myitmo.model.ResultResponse;
import api.myitmo.model.Schedule;
import api.myitmo.model.Specialization;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.time.LocalDate;
import java.util.List;

public interface MyItmoApi {

    @GET("/api/schedule/schedule/personal")
    Call<DataResponse<List<Schedule>>> getPersonalSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd);

    @GET("/api/record_book/specializations")
    Call<ResultResponse<List<Specialization>>> getSpecializations();
}
