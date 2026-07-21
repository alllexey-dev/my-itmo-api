package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

/** Контрольное мероприятие внутри дисциплины зачётки. */
@Data
public class ControlEntry {

    private long id;

    /** Отображаемое название контрольного мероприятия. */
    @SerializedName("control_name")
    private String controlName;

    /** Идентификатор родителя в дереве; {@code null} для корневых элементов. */
    @SerializedName("parent_id")
    private Long parentId;

    /** Нижняя граница диапазона баллов; это не фактически полученный балл. */
    @SerializedName("lower_value")
    private Double lowerValue;

    /** Максимально возможный балл. */
    @SerializedName("max_value")
    private Double maxValue;

    /** Минимальный балл, заданный для мероприятия. */
    @SerializedName("min_value")
    private Double minValue;

    /** Признак обязательности мероприятия. */
    private boolean required;

    /** Фактически полученный балл; {@code null}, если результата ещё нет. */
    private Double rate;

    /** Дата выставления или проведения; может отсутствовать. */
    private OffsetDateTime date;

    /** Преподаватель, выставивший результат; может отсутствовать. */
    private RecordBookTeacher teacher;
}
