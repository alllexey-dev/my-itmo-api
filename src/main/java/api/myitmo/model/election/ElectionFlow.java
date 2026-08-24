package api.myitmo.model.election;

import lombok.Data;

import java.util.List;

/** Узел рекурсивного дерева потоков дисциплины. */
@Data
public class ElectionFlow {

    private long id;

    private String name;

    private Long limitMax;

    private List<String> teachers;

    private List<ElectionFlow> variants;

    private int workType;

    private boolean available;

    private List<Integer> selections;
}
