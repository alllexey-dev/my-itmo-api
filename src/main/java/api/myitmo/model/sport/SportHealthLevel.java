package api.myitmo.model.sport;

import lombok.Data;

/** Медицинская группа здоровья пользователя для занятий спортом. */
@Data
public class SportHealthLevel {

    private long id;

    private long isu;

    /** Локализованное название, например {@code Основная группа здоровья}. */
    private String name;
}
