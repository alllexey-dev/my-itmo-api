package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Группа контактных данных одного типа.
 * У сотрудника наблюдались оба ключа без null; у студента и служебной записи
 * внешний массив contacts был пустым. Отсутствие вложенных ключей не наблюдалось.
 */
@Data
public class Contact {

    /** Значения контакта; в наблюдённом ответе — непустой массив строк. */
    private List<String> contact;

    /** Отображаемое название типа контакта; в наблюдённом ответе — строка. */
    @SerializedName("contact_alias")
    private String contactAlias;
}
