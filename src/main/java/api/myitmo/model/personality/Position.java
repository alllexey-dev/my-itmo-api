package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Должность сотрудника в подразделении Университета ИТМО.
 * В наблюдённых объектах все объявленные строковые поля присутствовали и не были null.
 */
@Data
public class Position {

    /** Название подразделения, строка в наблюдённом ответе. */
    @SerializedName("department_name")
    private String departmentName;

    /** Ссылка на страницу подразделения, строка в наблюдённом ответе. */
    @SerializedName("department_link")
    private String departmentLink;

    /** Название должности, строка в наблюдённом ответе. */
    @SerializedName("position_name")
    private String positionName;

    // TODO: vacation, start_vacation, end_vacation присутствовали только как null;
    // ненулевые JSON-типы и формат дат не подтверждены. Имена ключей API — snake_case.
    // private ? vacation;
    // private ? startVacation;
    // private ? endVacation;
}
