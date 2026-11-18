package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Recursive block, module or discipline. Depending on type, fields may be absent. */
@Serializable
public data class StudyPlanNode(
    /** Node identifier. */
    public val id: Long = 0,
    /** Display name of the block, module or discipline. */
    public val name: String = "",
    /** Open server node type; blocks, modules and disciplines have been observed. */
    public val type: String = "",
    /** Parent module identifier; may be absent. */
    public val moduleId: Long? = null,
    /** Parent block identifier; may be absent. */
    public val blockId: Long? = null,
    /** Parent block name; may be absent. */
    public val blockName: String? = null,
    /** Variable-part selection parameter; absent on mandatory items. */
    public val choiceParameterId: Long? = null,
    /** Selection parameter name; may be absent. */
    public val choiceParameterName: String? = null,
    /** Whether the user may select this item; may be absent. */
    public val choiceAvailable: Boolean? = null,
    /** Whether the discipline flow can be selected; may be absent. */
    public val flowSelectable: Boolean? = null,
    /** Whether the discipline can be replaced through the study-plan service; may be absent. */
    public val replaceable: Boolean? = null,
    /** Whether the starting semester may be selected; may be absent. */
    public val startSemesterSelectable: Boolean? = null,
    /** Workload in academic credits; may be absent. */
    public val creditPoints: Int? = null,
    /** Discipline duration in semesters; may be absent. */
    public val disciplineDuration: Int? = null,
    /** Discipline description; may be absent. */
    public val description: String? = null,
    /** Teaching language code; may be absent. */
    public val langCode: String? = null,
    /** Teaching language name; may be absent. */
    public val langName: String? = null,
    /** Discipline syllabus URL; may be absent. */
    public val rpdUrl: String? = null,
    /** Responsible department; may be absent. */
    public val department: StudyPlanDepartment? = null,
    /** Server rule identifiers; empty list is a client fallback. */
    public val rules: List<Long> = emptyList(),
    /** Child blocks, modules or disciplines; empty list is a client fallback. */
    public val children: List<StudyPlanNode> = emptyList(),
    /** Semester implementations: string semester numbers map to workloads; empty map is a client fallback. */
    public val contents: Map<String, List<StudyPlanContent>> = emptyMap(),
)
