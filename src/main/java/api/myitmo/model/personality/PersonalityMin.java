package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Сокращённый профиль из результатов поиска персоналий. */
@Data
public class PersonalityMin {

    /** Идентификатор персоналии; обычно совпадает с номером ИСУ. */
    private long id;

    private String fio;

    private String gender;

    private String phone;

    private String email;

    /** Краткое описание места работы или должности. */
    private String work;

    // Поле присутствует в ответе поиска, но его структура пока не подтверждена.
    // private ? education;

    /** URL фотографии профиля. */
    @SerializedName("photo")
    private String photoUrl;
}
