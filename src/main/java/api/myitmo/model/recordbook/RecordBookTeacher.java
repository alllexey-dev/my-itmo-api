package api.myitmo.model.recordbook;

import lombok.Data;

/** ФИО преподавателя в ответах сервиса зачётки. Все части имени могут отсутствовать. */
@Data
public class RecordBookTeacher {

    private String surname;

    private String name;

    private String patronymic;
}
