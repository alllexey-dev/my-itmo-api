package api.myitmo;

import api.myitmo.model.*;
import api.myitmo.model.election.ChangeResult;
import api.myitmo.model.finance.ScholarshipTotal;
import api.myitmo.model.other.QrData;
import api.myitmo.model.personality.Personality;
import api.myitmo.model.personality.PersonalityMin;
import api.myitmo.model.recordbook.ControlEntry;
import api.myitmo.model.recordbook.FlowChainsWrapper;
import api.myitmo.model.recordbook.RecordBookEntry;
import api.myitmo.model.recordbook.Specialization;
import api.myitmo.model.requests.RequestSummary;
import api.myitmo.model.schedule.ExtendedTimeSlot;
import api.myitmo.model.schedule.Schedule;
import api.myitmo.model.sport.*;
import api.myitmo.model.studyplan.StudyPlan;
import api.myitmo.model.studyplan.StudyPlanPrograms;
import api.myitmo.model.system.DashboardItem;
import api.myitmo.model.system.MenuResponse;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.http.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

/**
 * Retrofit-описание HTTP API личного кабинета MyITMO.
 *
 * <p>Большинство методов возвращает {@link ResultResponse}: код {@code 0} означает успех.
 * Сервисы расписания исторически используют {@link DataResponse} с аналогичным кодом {@code 0}.
 * Наборы справочных значений и числовых статусов принадлежат серверу и могут расширяться без
 * изменения API, поэтому клиенту не следует считать перечисленные в моделях значения исчерпывающими.</p>
 */
public interface MyItmoApi {

    // region schedule

    /**
     * Возвращает личное расписание пользователя за включительный диапазон дат.
     * Ответ этого сервиса использует обёртку {@link DataResponse}, а не {@link ResultResponse}.
     */
    @GET("/api/schedule/schedule/personal")
    Call<DataResponse<List<Schedule>>> getPersonalSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd);

    /** Возвращает стандартные временные слоты пар в порядке отображения. */
    @GET("/api/schedule/meta/time_slots")
    Call<DataResponse<List<ExtendedTimeSlot>>> getTimeSlots();

    // endregion schedule

    // region record book

    /** Возвращает образовательные программы пользователя и доступные для них семестры зачётки. */
    @GET("/api/record_book/specializations")
    Call<ResultResponse<List<Specialization>>> getSpecializations();

    /**
     * Возвращает дисциплины зачётки для программы и номера семестра.
     * {@code specializationId} соответствует полю {@code mainPlan} модели {@link Specialization}.
     */
    @GET("/api/record_book/{specialization_id}/{semester}")
    Call<ResultResponse<List<RecordBookEntry>>> getRecordBook(@Path("specialization_id") long specializationId, @Path("semester") int semester);

    /**
     * Возвращает дерево контрольных мероприятий дисциплины.
     * Идентификатор берётся из поля {@code estId} модели {@link RecordBookEntry}.
     */
    @GET("/api/record_book/{record_book_entry_id}")
    Call<ResultResponse<List<ControlEntry>>> getControlEntries(@Path("record_book_entry_id") long recordBookEntryId);

    // endregion record boot

    // region study plan

    /** Возвращает доступные пользователю учебные планы и активную образовательную программу. */
    @GET("/api/eduPlanNew/programs")
    Call<ResultResponse<StudyPlanPrograms>> getStudyPlanPrograms();

    /**
     * Возвращает полную рекурсивную структуру учебного плана.
     * {@code specializationId} может отсутствовать у программ без отдельной специализации.
     */
    @GET("/api/eduPlanNew/study_plan/{plan_id}")
    Call<ResultResponse<StudyPlan>> getStudyPlan(@Path("plan_id") long planId,
                                                 @Query("spec_id") @Nullable Long specializationId);

    // endregion study plan

    // region personalities

    /** Возвращает полный публичный профиль человека по номеру ИСУ. */
    @GET("/api/personalities/persons/{personId}")
    Call<ResultResponse<Personality>> getPersonality(@Path("personId") int personId);

    /**
     * Ищет людей по ФИО, номеру ИСУ или другим поддерживаемым сервером атрибутам.
     * {@code limit} задаёт размер страницы, {@code offset} — смещение от начала выдачи.
     */
    @GET("/api/personalities/persons")
    Call<ResultResponse<CountWrapper<List<PersonalityMin>>>> searchPersonalities(@Query("limit") int limit, @Query("offset") int offset, @Query("q") String query);

    // endregion personalities

    // region sport

    /** Возвращает временные слоты спортивных занятий. */
    @GET("/api/sport/time_slots")
    Call<ResultResponse<List<TimeSlot>>> getSportTimeSlots();

    /** Возвращает справочник видов спорта. Набор значений управляется сервером. */
    @GET("/api/sport/sport_types")
    Call<ResultResponse<List<IdValuePair>>> getSportTypes();

    /** Возвращает доступные значения фильтров расписания спорта. */
    @GET("/api/sport/sign/schedule/filters")
    Call<ResultResponse<SportFilters>> getSportFilters();

    /**
     * Возвращает занятия, доступные для записи, за включительный диапазон дат.
     * Списки видов спорта и преподавателей трактуются сервером как множественные фильтры.
     */
    @GET("/api/sport/sign/schedule")
    Call<ResultResponse<List<SportSchedule>>> getSportSchedule(@Query("date_start") LocalDate dateStart, @Query("date_end") LocalDate dateEnd,
                                                               @Query("building_id") Long buildingId, @Query("sport_type_id") @Nullable List<Long> sportTypeIds,
                                                               @Query("teacher_isu") @Nullable List<Long> teacherIsu);

    /** Возвращает спортивные баллы и историю начислений за указанный спортивный семестр. */
    @GET("/api/sport/personal/score")
    Call<ResultResponse<SportScore>> getSportScore(@Query("semester_id") Long semesterId);

    /** Возвращает подробный счётчик использованных и оставшихся попыток записи. */
    @GET("/api/sport/personal/have_attempts")
    Call<ResultResponse<SportAttempts>> getSportAttempts();

    /**
     * Возвращает отдельный серверный счётчик попыток записи.
     * Точная семантика значения не документирована; для UI предпочтительнее {@link #getSportAttempts()}.
     */
    @GET("/api/sport/personal/sign_attempts")
    Call<ResultResponse<Integer>> getSportSignAttempts();

    /** Возвращает список спортивных семестров для выбора в истории. */
    @GET("/api/sport/semesters/list")
    Call<ResultResponse<List<SportSemesterOption>>> getSportSemesters();

    /** Возвращает текущий спортивный семестр и его контрольные даты. */
    @GET("/api/sport/semesters/current")
    Call<ResultResponse<SportSemester>> getCurrentSportSemester();

    /** Возвращает персональный календарь спортивных занятий, включая уже прошедшие занятия. */
    @GET("/api/sport/personal/calendar")
    Call<ResultResponse<List<SportSchedule>>> getPersonalSportCalendar(@Query("date_start") LocalDate dateStart,
                                                                       @Query("date_end") LocalDate dateEnd);

    /** Возвращает состояние задолженности по физической культуре. */
    @GET("/api/sport/personal/debt")
    Call<ResultResponse<SportDebt>> getSportDebt();

    /** Возвращает состояние заявки пользователя на спортивный экстернат. */
    @GET("/api/sport/personal/externat")
    Call<ResultResponse<SportExternat>> getSportExternat();

    /** Возвращает назначенную пользователю медицинскую группу здоровья. */
    @GET("/api/sport/personal/health_level")
    Call<ResultResponse<SportHealthLevelResponse>> getSportHealthLevel();

    /** Возвращает спортивные отборы и нормативы, доступные пользователю. */
    @GET("/api/sport/personal/selections")
    Call<ResultResponse<List<SportSelection>>> getSportSelections();

    /** Возвращает специальные форматы получения зачёта, например экстернат. */
    @GET("/api/sport/projects/list")
    Call<ResultResponse<List<SportProject>>> getSportProjects();

    /**
     * Возвращает лимиты записи, сгруппированные сервером по идентификаторам.
     * Значение пустой карты означает отсутствие специальных ограничений для текущего пользователя.
     */
    @GET("/api/sport/sign/schedule/limits")
    Call<ResultResponse<HashMap<Long, HashMap<Long, SportSignLimit>>>> getSportSignLimits();

    /** Возвращает выбранные спортивные секции, группы занятий и регулярные слоты пользователя. */
    @GET("/api/sport/sign/chosen")
    Call<ResultResponse<List<ChosenSportSection>>> getChosenSportSections();

    /**
     * Записывает студента на указанные занятия.
     *
     * <p>API endpoint: {@code POST /api/sport/sign/schedule/lessons}</p>
     *
     * При невозможности записать студента возвращает {@code errorCode = 137} и перечисление причин.
     * Формат перечисления: [нельзя записать студента: [причина 1], [причина 2], ...]
     * <p>Возможные причины:</p>
     * <ul>
     *     <li>Выбрано 1 занятие в этот день</li>
     *     <li>Есть запись на занятия в это время: [секция] в [дата]</li>
     *     <li>Выбрано 2 занятия на неделе: [дата начала недели]</li>
     *     <li>... и другие ошибки</li>
     * </ul>
     *
     * @param lessonIds список ID занятий, на которые нужно записаться
     * @return {@link ResultResponse}, содержащий список ID успешно добавленных записей
     */

    @POST("/api/sport/sign/schedule/lessons")
    Call<ResultResponse<List<Long>>> signInLessons(@Body List<Long> lessonIds);

    /**
     * Отписывает студента от указанных занятий.
     *
     * <p>API endpoint: {@code DELETE /api/sport/sign/schedule/lessons}</p>
     *
     * При невозможности отписать студента возвращает {@code errorCode = 130} и перечисление причин.
     * Формат перечисления: [нельзя отписать студента: [причина 1], [причина 2], ...]
     * <p>Возможные причины:</p>
     * <ul>
     *     <li>Вы не записаны на это занятие</li>
     * </ul>
     *
     * @param lessonIds список ID занятий, от которых нужно отписаться
     * @return {@link ResultResponse}, содержащий список ID занятий, от которых успешно отписались
     */
    @HTTP(method = "DELETE", path = "/api/sport/sign/schedule/lessons", hasBody = true)
    Call<ResultResponse<List<Long>>> signOutLessons(@Body List<Long> lessonIds);

    // endregion sport

    // region system

    /** Возвращает сохранённую пользователем раскладку виджетов главного экрана MyITMO. */
    @GET("/api/system/dashboard")
    Call<ResultResponse<List<DashboardItem>>> getDashboard();

    /** Возвращает основное навигационное меню, включая вложенные разделы. */
    @GET("/api/system/v1/menu/items")
    Call<ResultResponse<MenuResponse>> getMenuItems();

    /** Возвращает каталог сервисов, доступных текущему пользователю. */
    @GET("/api/system/v1/menu/services")
    Call<ResultResponse<MenuResponse>> getServices();

    // endregion system

    // region requests

    /** Возвращает краткий список заявок текущего пользователя. */
    @GET("/api/requests/my")
    Call<ResultResponse<List<RequestSummary>>> getMyRequests();

    // endregion requests

    // region finances

    /** Возвращает суммарные выплаты по категориям за всё доступное время. */
    @GET("/api/finances/scholarship/total")
    Call<ResultResponse<List<ScholarshipTotal>>> getScholarshipTotals();

    /** Возвращает суммарные выплаты по категориям за включительный диапазон дат. */
    @GET("/api/finances/scholarship/total")
    Call<ResultResponse<List<ScholarshipTotal>>> getScholarshipTotals(@Query("dateFrom") LocalDate dateFrom,
                                                                      @Query("dateTo") LocalDate dateTo);

    // endregion finances

    // region election

    /** Возвращает выбранные цепочки потоков в текущей кампании записи по выбору. */
    @GET("/api/election/students/selected_flow_chains")
    Call<ResultResponse<FlowChainsWrapper>> getSelectedFlowChains();

    /** Возвращает идентификаторы уже выбранных потоков. */
    @GET("/api/election/students/chosen_flows")
    Call<ResultResponse<List<Long>>> getChosenFlows();

    /** Заменяет выбранные потоки на переданный упорядоченный список. */
    @POST("/api/election/students/order/change")
    Call<ResultResponse<ChangeResult>> changeSelectedFlows(@Body List<Long> flowIds);

    /** Очищает выбор потоков текущего пользователя. */
    @POST("/api/election/students/order/clear")
    Call<ResultResponse<?>> clearAllSelectedFlows();

    /** Заменяет выбранные дисциплины на переданный список идентификаторов. */
    @POST("/api/election/students/order/")
    Call<ResultResponse<ChangeResult>> changeSelectedDisciplines(@Body List<String> disciplineIds);
    // endregion election

    // region other

    /**
     * Возвращает QR-пропуск пользователя через отдельный сервис {@code qr.itmo.su}.
     * Формат ответа отличается от остальных методов MyITMO.
     */
    @GET("https://qr.itmo.su/v1/user/pass")
    Call<SimpleResponse<QrData>> getQrCode();

    // endregion other
}
