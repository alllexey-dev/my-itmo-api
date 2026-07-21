package api.myitmo.model.recordbook;

import lombok.Data;

import java.util.List;

/** Выбранные цепочки потоков записи по выбору. */
@Data
public class FlowChainsWrapper {

    /** Идентификатор субъекта выбора; семантика сервера пока не подтверждена. */
    private Long selectedBy;

    private List<FlowChain> flowChains;
}
