package api.myitmo.model.election;

import lombok.Data;

import java.util.List;

/** Дисциплина, доступная студенту в кампании записи по выбору. */
@Data
public class AvailableDiscipline {

    private long dcId;

    private long discId;

    private long langId;

    private String depName;

    private String discName;

    private String langCode;

    private boolean required;

    private List<AvailableDisciplineSemester> semesters;

    private String description;

    private String depNameShort;

    private List<Long> notCompatibleWith;
}
