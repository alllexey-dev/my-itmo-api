package api.myitmo.model.system;

import lombok.Data;

import java.util.List;

/** Обёртка навигационного меню MyITMO. */
@Data
public class MenuResponse {

    private List<MenuItem> menu;
}
