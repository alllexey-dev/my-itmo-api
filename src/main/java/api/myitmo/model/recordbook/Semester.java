package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Учебный период в зачётке. */
@Data
public class Semester {

    /** Учебный год в формате {@code YYYY/YYYY}, например {@code 2025/2026}. */
    @SerializedName("study_year")
    private String studyYear;

    /** Сквозной номер семестра в учебном плане, начиная с 1. */
    private int semester;

    /** Номер курса, начиная с 1. */
    private int course;

    /** {@code true}, если этот период считается текущим для пользователя. */
    private boolean actual;
}
