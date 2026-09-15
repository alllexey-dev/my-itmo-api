package api.bars;

import api.bars.model.Discipline;
import api.bars.model.GroupOrFlow;
import api.bars.model.Setting;
import api.bars.model.StudentJournal;
import api.bars.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/**
 * Retrofit-описание REST API БАРС ({@code https://bars.itmo.ru/backend/rest/}).
 *
 * <p>Ответы приходят без обёрток: списки — голые массивы, сущности — голые объекты.
 * Каталоги и журналы читаются в контексте периода, сохранённого на сервере для
 * пользователя ({@code User#getSelectedYear()}, {@code User#getSelectedTerm()}, см. {@link User});
 * query-параметров года и сезона у них нет. Описаны только операции чтения
 * собственного журнала и выбора периода.</p>
 */
public interface BarsApi {

    /**
     * Обменивает OIDC authorization code на сессию. Тело ответа пустое, заголовок
     * {@code authorization} содержит полное значение {@code Bearer ...}.
     */
    @GET("login")
    Call<Void> login(@Query("code") String code, @Query("customRedirectUri") String customRedirectUri);

    /** Текущий пользователь и выбранный на сервере период. */
    @GET("users/current_user/")
    Call<User> getCurrentUser();

    /** Общие настройки сервера, в том числе {@code current_year} и {@code current_term}. */
    @GET("config/")
    Call<List<Setting>> getConfig();

    /**
     * Сохраняет личную настройку. Смена периода — два вызова: {@code current_year}
     * со значением {@code 2025/2026} и {@code current_term} со значением {@code 0} или {@code 1}.
     * Настройка общая с веб-версией БАРС.
     */
    @POST("config/personal")
    Call<Setting> setPersonalSetting(@Body Setting setting);

    /** Дисциплины выбранного периода; {@code withCheckpointPlansOnly=true} оставляет только дисциплины с планами. */
    @GET("journal/disciplines")
    Call<List<Discipline>> getDisciplines(@Query("withCheckpointPlansOnly") Boolean withCheckpointPlansOnly);

    /** Потоки и группы выбранного периода; {@code disciplineId} необязателен. */
    @GET("journal/groups-and-flows")
    Call<List<GroupOrFlow>> getGroupsAndFlows(@Query("disciplineId") Long disciplineId);

    /**
     * Собственный журнал по плану и потоку. Сегмент {@code /student} без идентификатора
     * ограничивает ответ текущим обучающимся. План должен принадлежать выбранному периоду,
     * иначе сервер отвечает 401.
     */
    @GET("marks/{checkpointPlanId}/{type}/{identifier}/student")
    Call<StudentJournal> getStudentJournal(@Path("checkpointPlanId") long checkpointPlanId,
                                           @Path("type") String type,
                                           @Path("identifier") String identifier);
}
