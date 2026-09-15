package api.bars.model;

import lombok.Data;

/** Заголовок журнала: план целиком и адрес потока, по которому он получен. */
@Data
public class JournalHeaders {

    private CheckpointPlan plan;

    /** Совпадает с {@code type} из URL. */
    private String type;

    /** Совпадает с {@code identifier} из URL. */
    private String identifier;

    /** Название потока или группы. */
    private String name;

    // TODO: deadlines наблюдался только пустым массивом.
}
