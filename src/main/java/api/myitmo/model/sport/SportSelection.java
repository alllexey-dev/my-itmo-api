package api.myitmo.model.sport;

import lombok.Data;

import java.util.List;

/** Спортивная дисциплина, для которой пользователю доступен отбор. */
@Data
public class SportSelection {

    private long id;

    private String name;

    /** Доступные уровни и нормативы отбора. */
    private List<SportRequisite> requisites;
}
