package api.myitmo.model.system;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Положение и ограничения одного виджета на главном экране MyITMO. */
@Data
public class DashboardItem {

    /** Строковый идентификатор элемента раскладки. */
    private String i;

    private int x;

    private int y;

    private int w;

    private int h;

    /** Имя виджета, которое клиент сопоставляет с UI-компонентом. */
    private String widget;

    /** Зафиксирован ли элемент в раскладке. */
    @SerializedName("static")
    private boolean isStatic;

    /** Необязательная минимальная ширина в единицах сетки. */
    @SerializedName("min_w")
    private Integer minWidth;

    /** Необязательная максимальная ширина в единицах сетки. */
    @SerializedName("max_w")
    private Integer maxWidth;

    /** Необязательная минимальная высота в единицах сетки. */
    @SerializedName("min_h")
    private Integer minHeight;

    /** Необязательная максимальная высота в единицах сетки. */
    @SerializedName("max_h")
    private Integer maxHeight;

    @SerializedName("is_draggable")
    private Boolean draggable;

    @SerializedName("is_resizable")
    private Boolean resizable;

    @SerializedName("preserve_aspect_ratio")
    private Boolean preserveAspectRatio;

    @SerializedName("drag_allow_from")
    private String dragAllowFrom;

    @SerializedName("drag_ignore_from")
    private String dragIgnoreFrom;

    @SerializedName("resize_ignore_from")
    private String resizeIgnoreFrom;
}
