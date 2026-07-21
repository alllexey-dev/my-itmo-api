package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Должность сотрудника в подразделении Университета ИТМО. */
@Data
public class Position {

    /** Название подразделения. */
    @SerializedName("department_name")
    private String departmentName;

    /** Ссылка на страницу подразделения. */
    @SerializedName("department_link")
    private String departmentLink;

    /** Название должности. */
    @SerializedName("position_name")
    private String positionName;

    // Поля присутствуют в ответе API, но их типы и формат дат пока не подтверждены.
    // private ? vacation;
    // private ? startVacation;
    // private ? endVacation;
}
