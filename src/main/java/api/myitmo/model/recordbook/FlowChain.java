package api.myitmo.model.recordbook;

import lombok.Data;

import java.util.List;

/** Дисциплина и доступные для неё потоки в записи по выбору. */
@Data
public class FlowChain {

    private int disciplineId;

    private String discName;

    private List<Flow> flows;
}
