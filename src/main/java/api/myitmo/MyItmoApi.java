package api.myitmo;

import api.myitmo.model.*;
import org.jetbrains.annotations.Nullable;
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
    Call<DataResponse<List<ExtendedTimeSlot>>> getTimeSlots();

    @GET("/api/election/students/selected_flow_chains")
    Call<ResultResponse<FlowChainsWrapper>> getSelectedFlowChains();

    @GET("/api/personalities/persons/{personId}")
    Call<ResultResponse<Personality>> getPersonality(@Path("personId") int personId);

    @GET("/api/personalities/persons")
    Call<ResultResponse<CountWrapper<List<PersonalityMin>>>> searchPersonalities(@Query("limit") int limit, @Query("offset") int offset, @Query("q") String query);

    @GET("/api/sport/time_slots")
    Call<ResultResponse<List<TimeSlot>>> getSportTimeSlots();

    @GET("/api/sport/sport_types")
    Call<ResultResponse<List<IdValuePair>>> getSportTypes();

    @GET("/api/sport/sign/schedule/filters")
    Call<ResultResponse<SportFilters>> getSportFilters();

    @GET("/api/sport/sign/schedule")
    Call<ResultResponse<List<SportSchedule>>> getSportSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd,
                                                         @Query("building_id") Long buildingId, @Query("sport_type_id") @Nullable List<Long> sportTypeIds,
                                                         @Query("teacher_isu") @Nullable List<Long> teacherIsu);

    @GET("/api/sport/personal/score")
    Call<ResultResponse<SportScore>> getSportScore(@Query("semester_id") Long semesterId);

    @GET("https://qr.itmo.su/v1/user/pass")
    Call<SimpleResponse<QrData>> getQrCode();

}
