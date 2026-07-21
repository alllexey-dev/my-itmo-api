package api.myitmo.model.sport;

import lombok.Data;

/** Лимит и остаток записей для серверной группы ограничений. */
@Data
public class SportSignLimit {

    private int limit;

    private int available;
}
