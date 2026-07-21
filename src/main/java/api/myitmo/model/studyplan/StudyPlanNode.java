package api.myitmo.model.studyplan;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Узел рекурсивной структуры учебного плана.
 * В зависимости от {@link #type} представляет блок, модуль или дисциплину, поэтому часть полей может отсутствовать.
 */
@Data
public class StudyPlanNode {

    private long id;

    private String name;

    /** Серверный тип узла. Набор открыт для расширения; наблюдались блоки, модули и дисциплины. */
    private String type;

    private Long moduleId;

    private Long blockId;

    private String blockName;

    /** Параметр выбора вариативной части; отсутствует для обязательных элементов. */
    private Long choiceParameterId;

    private String choiceParameterName;

    /** Разрешён ли пользователю выбор этого элемента. */
    private Boolean choiceAvailable;

    /** Можно ли выбрать поток дисциплины. */
    private Boolean flowSelectable;

    /** Можно ли заменить дисциплину через сервис учебного плана. */
    private Boolean replaceable;

    /** Можно ли выбрать семестр начала дисциплины. */
    private Boolean startSemesterSelectable;

    /** Трудоёмкость в зачётных единицах. */
    private Integer creditPoints;

    /** Продолжительность дисциплины в семестрах. */
    private Integer disciplineDuration;

    private String description;

    private String langCode;

    private String langName;

    /** Ссылка на рабочую программу дисциплины. */
    private String rpdUrl;

    private StudyPlanDepartment department;

    private List<Long> rules;

    /** Дочерние блоки, модули или дисциплины. */
    private List<StudyPlanNode> children;

    /**
     * Реализации дисциплины по семестрам.
     * Ключ — номер семестра в строковом виде, значение — нагрузки дисциплины в этом семестре.
     */
    private Map<String, List<StudyPlanContent>> contents;
}
