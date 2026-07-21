package api.myitmo.model.studyplan;

import lombok.Data;

/** Отдельный вид учебной работы в нагрузке дисциплины. */
@Data
public class StudyPlanActivity {

    private long id;

    private long contentId;

    /** Название, например {@code Лекции}, {@code Практические занятия}, {@code Экзамен}. */
    private String name;

    /** Объём в академических часах; у формы контроля может быть {@code null}. */
    private Double volume;

    /**
     * Идентификатор вида работы. Наблюдаемые значения:
     * 1 — лекции, 3 — практические занятия, 4 — самостоятельная работа,
     * 5 — экзамен, 6 — зачёт. Справочник может расширяться.
     */
    private long workTypeId;
}
