package api.myitmo.model.election;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Результат изменения выбора дисциплины или потока. */
@Data
public class ChangeResult {

    /** Серверный статус операции; точный набор значений не документирован. */
    @Nullable
    private Long status;

    /** Название изменённого элемента; может отсутствовать. */
    @Nullable
    private String name;

    /** Итоговый список выбранных потоков. */
    private List<Long> flows;
}
