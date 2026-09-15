package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Балл за одну контрольную точку. Отсутствие записи означает «не выставлено», а не 0. */
@Data
public class Mark {

    /** Идентификатор записи оценки; не идентификатор работы. */
    private long id;

    /** Связь с {@code id} точки ({@link Checkpoint}); {@code null} у дополнительных баллов. */
    @SerializedName("checkpoint_id")
    private Long checkpointId;

    @SerializedName("checkpoint_plan_id")
    private long checkpointPlanId;

    /** Полученные баллы; дробные значения наблюдались. */
    private Double mark;

    /** {@code current} у обычных работ, {@code final} у итоговой; не путать с типом точки. */
    private String type;

    /** Неявка; нельзя выводить из нулевого балла. */
    @SerializedName("is_absent")
    private boolean absent;

    @SerializedName("is_not_bigger_than_max")
    private boolean notBiggerThanMax;

    /** Unix-время в миллисекундах. */
    @SerializedName("created_at")
    private Long createdAt;

    @SerializedName("updated_at")
    private Long updatedAt;

    /** Автор записи; это не преподаватель дисциплины. */
    @SerializedName("created_by_name")
    private String createdByName;

    @SerializedName("updated_by_name")
    private String updatedByName;
}
