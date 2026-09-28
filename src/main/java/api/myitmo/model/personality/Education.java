package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Краткие сведения об обучении человека.
 * У студента все три поля наблюдались как строки; null и отсутствие не наблюдались.
 * У сотрудника и служебной записи массив education был пустым.
 */
@Data
public class Education {

    /** Номер курса, переданный числом в строке, а не JSON-числом. */
    private String course;

    /** Название факультета или института. */
    @SerializedName("faculty_name")
    private String facultyName;

    /** Учебная группа. */
    private String group;
}
