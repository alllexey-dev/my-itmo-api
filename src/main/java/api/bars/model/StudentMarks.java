package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Баллы и подтверждения студента. {@code regularSum} и {@code total} считает сервер;
 * клиенту не следует пересчитывать их из отдельных работ.
 */
@Data
public class StudentMarks {

    /** Баллы обычных контрольных точек; пустой список у ещё не заполненного журнала. */
    private List<Mark> regular;

    /** Балл итоговой точки (экзамен, зачёт); отсутствует, пока не выставлен. */
    @SerializedName("final")
    private Mark finalMark;

    /** Дополнительные баллы; у них {@code checkpoint_id = null}. */
    private Mark additional;

    /** Серверная сумма текущей аттестации. */
    private Double regularSum;

    /** Серверный итог. У пустого журнала {@code 0.0}: это «ничего не выставлено», а не оценка. */
    private Double total;

    /** Подтверждённые результаты попыток; последняя по номеру попытки — актуальная. */
    @SerializedName("active_approvals")
    private List<Approval> activeApprovals;

    /** Есть ли хоть одна выставленная работа. */
    public boolean hasAnyMark() {
        return (regular != null && !regular.isEmpty()) || finalMark != null || additional != null;
    }
}
