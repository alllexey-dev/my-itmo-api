package api.myitmo.model.election;

import lombok.Data;

import java.util.List;

/** Выбранная дисциплина и дерево доступных для неё потоков. */
@Data
public class ElectionFlowChain {

    private String groupFlow;

    private long disciplineId;

    private String disciplineName;

    private List<ElectionFlow> flows;
}
