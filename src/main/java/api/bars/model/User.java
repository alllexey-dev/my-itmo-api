package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Текущий пользователь БАРС ({@code GET users/current_user/}).
 * Вместе с профилем сервер хранит выбранный учебный период: каталоги и журналы
 * читаются в его контексте, отдельных query-параметров года и сезона у них нет.
 */
@Data
public class User {

    /** Идентификатор пользователя БАРС; не ISU. */
    private long id;

    /** Логин ITMO.ID; у студентов совпадает с ISU в виде строки. */
    private String login;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("middle_name")
    private String middleName;

    @SerializedName("last_name")
    private String lastName;

    /** Доступные роли, например «Обучающийся». */
    @SerializedName("user_roles")
    private List<UserRole> userRoles;

    @SerializedName("selected_role")
    private UserRole selectedRole;

    /** Выбранный учебный год в формате {@code 2025/2026}. */
    @SerializedName("selected_year")
    private String selectedYear;

    /** Выбранный сезон: см. {@link Term}. */
    @SerializedName("selected_term")
    private int selectedTerm;

    /** Личные настройки, включая {@code current_year} и {@code current_term}. */
    @SerializedName("personal_config")
    private List<Setting> personalConfig;

    @SerializedName("can_change_user")
    private boolean canChangeUser;

    @SerializedName("restricted_to_have_read_only_access")
    private boolean restrictedToHaveReadOnlyAccess;

    // TODO: created_by, updated_by, created_at, updated_at, super_user_personal_number наблюдались только как null.

    public Term getSelectedTermValue() {
        return Term.fromWire(selectedTerm);
    }
}
