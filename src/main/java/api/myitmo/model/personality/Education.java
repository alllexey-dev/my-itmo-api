package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Краткие сведения об обучении человека. */
@Data
public class Education {

    /** Курс в текстовом представлении сервера. */
    private String course;

    /** Название факультета или института. */
    @SerializedName("faculty_name")
    private String facultyName;

    /** Учебная группа. */
    private String group;
}
