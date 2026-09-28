package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Полный публичный профиль человека в MyITMO.
 * В наблюдённых профилях студента, сотрудника и служебной записи все объявленные ключи
 * присутствовали. Это описание наблюдений, а не гарантия обязательности полей API.
 */
@Data
public class Personality {

    /** Уникальный номер ИСУ; во всех наблюдённых профилях — число, совпадающее с запросом, не null. */
    private long isu;

    /** Полное имя в серверном формате; наблюдалась строка, null и отсутствие не наблюдались. */
    private String fio;

    /** Гендер в текстовом представлении сервера; наблюдалась строка, в том числе {@code male}. */
    private String gender;

    /** URL фотографии: строка у студента и сотрудника, {@code null} у служебной записи. */
    @SerializedName("photo")
    private String photoUrl;

    /** Контакты по типам: массив объектов у сотрудника, пустой массив у студента и служебной записи. */
    private List<Contact> contacts;

    /** Аудитории сотрудника; во всех проверенных профилях — пустой массив, не null. */
    private List<Room> rooms;

    /** Должности: массив объектов у сотрудника, пустой массив у студента и служебной записи. */
    private List<Position> positions;

    // TODO: powers — наблюдались пустой массив и массив объектов со строками dep_name, dep_link, power_name;
    // остальные формы и nullability вложенных полей не подтверждены.
    // private ? powers;
    // TODO: levels — наблюдались null и объект со строками rank, degree; остальные формы не подтверждены.
    // private ? levels;

    /** Обучение: массив объектов у студента, пустой массив у сотрудника и служебной записи. */
    private List<Education> education;

    // TODO: activities — наблюдался только null; ненулевой JSON-тип и структура неизвестны.
    // private ? activities;

    /** Признак обучения по обмену; во всех наблюдённых профилях — boolean, null и отсутствие не наблюдались. */
    @SerializedName("exchange_training")
    private boolean exchangeTraining;
}
