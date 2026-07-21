package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Полный публичный профиль человека в MyITMO. */
@Data
public class Personality {

    /** Уникальный номер ИСУ. */
    private long isu;

    /** Полное имя в серверном формате. */
    private String fio;

    /** Гендер в текстовом представлении сервера; поле может содержать локализованное значение. */
    private String gender;

    /** URL фотографии профиля. */
    @SerializedName("photo")
    private String photoUrl;

    /** Контакты, сгруппированные по типам. */
    private List<Contact> contacts;

    /** Аудитории, связанные с сотрудником; для студента список обычно пуст. */
    private List<Room> rooms;

    /** Должности сотрудника; для студента список обычно пуст. */
    private List<Position> positions;

    // Поля присутствуют в ответе API, но структура их значений пока не подтверждена.
    // private ? powers;
    // private ? levels;

    /** Образовательные статусы пользователя. */
    private List<Education> education;

    // Поле присутствует в ответе API, но структура его значения пока не подтверждена.
    // private ? activities;

    /** Признак обучения по обмену. */
    @SerializedName("exchange_training")
    private boolean exchangeTraining;
}
