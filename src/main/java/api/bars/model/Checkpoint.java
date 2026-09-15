package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Контрольная точка плана. Баллы студента лежат отдельно в {@link Mark} и связываются по {@code id}. */
@Data
public class Checkpoint {

    private long id;

    private String gid;

    /** У итоговой точки {@code null}; тогда описанием служит {@code type}. */
    private String name;

    /** Вид работы, например «Тест», «Лабораторная работа», «Экзамен», «Зачет». */
    private String type;

    @SerializedName("type_id")
    private long typeId;

    /** Неделя семестра; у итоговой точки {@code null}. */
    private Integer week;

    private boolean group;

    /** Ключевая точка. */
    private boolean key;

    @SerializedName("min_grade")
    private double minGrade;

    @SerializedName("max_grade")
    private double maxGrade;

    /** Вложенные точки; веб-клиент поддерживает рекурсию, в наблюдённых планах список пуст. */
    @SerializedName("sub_checkpoints")
    private List<Checkpoint> subCheckpoints;

    @SerializedName("parent_checkpoint_id")
    private Long parentCheckpointId;

    // TODO: test_id, test_name, max_sub_checkpoints_fillable наблюдались только как null.
}
