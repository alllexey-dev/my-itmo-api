package api.myitmo;

import api.myitmo.model.*;
import api.myitmo.model.other.QrData;
import api.myitmo.model.personality.Personality;
import api.myitmo.model.personality.PersonalityMin;
import api.myitmo.model.recordbook.ControlEntry;
import api.myitmo.model.recordbook.FlowChainsWrapper;
import api.myitmo.model.recordbook.RecordBookEntry;
import api.myitmo.model.recordbook.Specialization;
import api.myitmo.model.schedule.ExtendedTimeSlot;
import api.myitmo.model.schedule.Schedule;
import api.myitmo.model.sport.*;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.http.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

public interface MyItmoApi {

    // region schedule

    @GET("/api/schedule/schedule/personal")
    Call<DataResponse<List<Schedule>>> getPersonalSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd);

    @GET("/api/schedule/meta/time_slots")
    Call<DataResponse<List<ExtendedTimeSlot>>> getTimeSlots();

    // endregion schedule

    // region record book

    @GET("/api/record_book/specializations")
    Call<ResultResponse<List<Specialization>>> getSpecializations();

    @GET("/api/record_book/{specialization_id}/{semester}")
    Call<ResultResponse<List<RecordBookEntry>>> getRecordBook(@Path("specialization_id") int specializationId, @Path("semester") int semester);

    @GET("/api/record_book/{record_book_entry_id}")
    Call<ResultResponse<List<ControlEntry>>> getControlEntries(@Path("record_book_entry_id") int recordBookEntryId);

    @GET("/api/election/students/selected_flow_chains")
    Call<ResultResponse<FlowChainsWrapper>> getSelectedFlowChains();

    // endregion record boot

    // region personalities

    @GET("/api/personalities/persons/{personId}")
    Call<ResultResponse<Personality>> getPersonality(@Path("personId") int personId);

    @GET("/api/personalities/persons")
    Call<ResultResponse<CountWrapper<List<PersonalityMin>>>> searchPersonalities(@Query("limit") int limit, @Query("offset") int offset, @Query("q") String query);

    // endregion personalities

    // region sport

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

    @GET("/api/sport/personal/have_attempts")
    Call<ResultResponse<SportAttempts>> getSportAttempts();

    @GET("/api/sport/sign/schedule/limits")
    Call<ResultResponse<HashMap<Long, HashMap<Long, SportSignLimit>>>> getSportSignLimits();

    @GET("/api/sport/sign/chosen")
    Call<ResultResponse<List<ChosenSportSection>>> getChosenSportSections();

    @POST("/api/sport/sign/schedule/lessons")
    Call<ResultResponse<List<Long>>> signInLessons(@Body List<Long> lessonIds);

    @HTTP(method = "DELETE", path = "/api/sport/sign/schedule/lessons", hasBody = true)
    Call<ResultResponse<List<Long>>> signOutLessons(@Body List<Long> lessonIds);

    // endregion sport

    // region other

    @GET("https://qr.itmo.su/v1/user/pass")
    Call<SimpleResponse<QrData>> getQrCode();

    // endregion other
}
