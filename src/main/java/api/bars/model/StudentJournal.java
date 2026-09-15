package api.bars.model;

import lombok.Data;

import java.util.List;

/**
 * Собственный журнал ({@code GET marks/{plan}/{type}/{identifier}/student}).
 * Сегмент {@code /student} ограничивает ответ текущим обучающимся: в наблюдённых
 * ответах {@code students} содержит ровно один элемент, но клиенту стоит проверять логин.
 */
@Data
public class StudentJournal {

    private List<StudentRecord> students;

    private JournalHeaders headers;
}
