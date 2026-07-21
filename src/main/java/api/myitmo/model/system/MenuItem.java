package api.myitmo.model.system;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Навигационный раздел или карточка сервиса MyITMO. */
@Data
public class MenuItem {

    private String title;

    private String description;

    /** Внутренний маршрут или внешняя ссылка. */
    private String to;

    /** Имя монохромной иконки в дизайн-системе сайта. */
    private String icon;

    /** Имя многоцветной иконки; может быть пустым. */
    @SerializedName("multicolor_icon")
    private String multicolorIcon;

    /** Требует ли маршрут точного совпадения при определении активного пункта. */
    private boolean exact;

    /** Цвет карточки сервиса; для пунктов основного меню может отсутствовать. */
    private String color;

    /** Вложенные пункты; отсутствуют у конечного маршрута. */
    private List<MenuItem> children;
}
