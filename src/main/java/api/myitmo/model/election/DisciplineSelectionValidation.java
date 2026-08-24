package api.myitmo.model.election;

import lombok.Data;

import java.util.List;

/** Результат серверной проверки выбранного набора дисциплин. */
@Data
public class DisciplineSelectionValidation {

    private List<Long> disciplines;

    private boolean validVariantSelection;

    private boolean validRequiredSelection;

    private int needSelectRequired;

    private int needSelectVariants;

    private int needSelectVariantsMax;

    private boolean scheduleAvailable;
}
