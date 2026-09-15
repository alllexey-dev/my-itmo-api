package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Строка студента в журнале. */
@Data
public class StudentRecord {

    /** Идентификатор студента БАРС; не ISU. */
    @SerializedName("student_id")
    private long studentId;

    /** Логин ITMO.ID; сверяйте с ожидаемым владельцем сессии. */
    @SerializedName("student_login")
    private String studentLogin;

    @SerializedName("student_name")
    private String studentName;

    private StudentMarks marks;

    /** Права на редактирование; для чтения не нужны и оценками не являются. */
    private StudentAccessibility accessibility;

    @SerializedName("wants_to_increase_marks")
    private boolean wantsToIncreaseMarks;
}
