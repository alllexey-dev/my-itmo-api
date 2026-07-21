package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Образовательная программа, для которой доступна зачётка пользователя. */
@Data
public class Specialization {

    /** Идентификатор основного учебного плана; используется как {@code specialization_id}. */
    @SerializedName("main_plan")
    private long mainPlan;

    /** Отображаемое название образовательной программы. */
    @SerializedName("specialization_name")
    private String specializationName;

    /** Семестры, для которых сервер позволяет запросить зачётку. */
    private List<Semester> semesters;
}
