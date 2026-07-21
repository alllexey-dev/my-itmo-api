package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Группа контактных данных одного типа. */
@Data
public class Contact {

    /** Значения контакта; сервер может вернуть несколько телефонов, адресов или ссылок. */
    private List<String> contact;

    /** Отображаемое название типа контакта. */
    @SerializedName("contact_alias")
    private String contactAlias;
}
