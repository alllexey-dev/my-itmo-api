package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

/** Итоговая запись по одной дисциплине в зачётке. */
@Data
public class RecordBookEntry {

    /** Название дисциплины; сервер иногда возвращает пробелы по краям. */
    private String name;

    /** Идентификатор дисциплины в учебном плане. */
    @SerializedName("discipline_id")
    private long disciplineId;

    /** Идентификатор записи зачётки, используемый для загрузки {@link ControlEntry}. */
    @SerializedName("est_id")
    private long estId;

    /** Текущая сумма баллов; {@code null}, если оценивание ещё не началось. */
    @SerializedName("current_score")
    private Double currentScore;

    /**
     * Итоговая оценка в представлении сервера.
     * Наблюдаемые значения: {@code 5/A}, {@code 4/B}, {@code 4/C}, {@code 3/D},
     * {@code 3/E}, {@code 2/FX}, {@code Зачёт}, {@code Незачёт} и {@code null}.
     */
    private String rate;

    /** Номер использованной попытки сдачи; обычно 0 или 1 до пересдач. */
    private int attempt;

    /** Вид итогового контроля, например {@code Экзамен}, {@code Зачёт}. */
    @SerializedName("control_type")
    private String controlType;

    /** Серверный идентификатор вида итогового контроля. */
    @SerializedName("control_type_id")
    private long controlTypeId;

    /** Дата итогового контроля; может отсутствовать. */
    @SerializedName("exam_date")
    private OffsetDateTime examDate;

    /** Признак наличия детального дерева контрольных мероприятий. */
    @SerializedName("have_tree")
    private boolean haveTree;

    /** Ссылка на дисциплину во внешней LMS; может отсутствовать. */
    @SerializedName("lms_link")
    private String lmsLink;

    /** Преподаватель итогового контроля; может отсутствовать. */
    private RecordBookTeacher teacher;
}
