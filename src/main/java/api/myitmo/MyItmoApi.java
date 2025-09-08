package api.myitmo;

import api.myitmo.model.*;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.time.LocalDate;
import java.util.List;

public interface MyItmoApi {

    @GET("/api/schedule/schedule/personal")
    Call<DataResponse<List<Schedule>>> getPersonalSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd);

    @GET("/api/record_book/specializations")
    Call<ResultResponse<List<Specialization>>> getSpecializations();

    @GET("/api/record_book/{specialization_id}/{semester}")
    Call<ResultResponse<List<RecordBookEntry>>> getRecordBook(@Path("specialization_id") int specializationId, @Path("semester") int semester);

    @GET("/api/record_book/{record_book_entry_id}")
    Call<ResultResponse<List<ControlEntry>>> getControlEntries(@Path("record_book_entry_id") int recordBookEntryId);

    @GET("/api/schedule/meta/time_slots")
    Call<DataResponse<List<TimeSlot>>> getTimeSlots();

    @GET("/api/election/students/selected_flow_chains")
    Call<ResultResponse<FlowChainsWrapper>> getSelectedFlowChains();

    @GET("https://qr.itmo.su/v1/user/pass")
    Call<SimpleResponse<QrData>> getQrCode();
}
