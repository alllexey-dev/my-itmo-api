package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Роль пользователя БАРС. Числовой {@code id} роли не является ISU. */
@Data
public class UserRole {

    private long id;

    /** Название роли, например «Обучающийся». */
    private String name;

    /** Флаги ниже присутствуют только у {@code selected_role}. */
    private Boolean locked;

    private Boolean selected;

    @SerializedName("allows_white_list")
    private Boolean allowsWhiteList;

    @SerializedName("allows_multiple")
    private Boolean allowsMultiple;

    // TODO: white_list наблюдался только пустым массивом; created_*/updated_*/main_user/requires_main_user_user_role только null.
}
