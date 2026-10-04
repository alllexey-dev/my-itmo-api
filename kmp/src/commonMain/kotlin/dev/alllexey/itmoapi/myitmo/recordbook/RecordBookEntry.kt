package dev.alllexey.itmoapi.myitmo.recordbook

import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Final record for one discipline in the recordbook. Dates become UTC instants. */
@Serializable
public data class RecordBookEntry(
    /** Discipline name; surrounding whitespace has been observed and is preserved. */
    public val name: String = "",
    /** Discipline identifier in the study plan. */
    @SerialName("discipline_id")
    public val disciplineId: Long = 0,
    /** Recordbook entry identifier used to load ControlEntry details. */
    @SerialName("est_id")
    public val estId: Long = 0,
    /** Current total points; null when assessment has not started. */
    @SerialName("current_score")
    public val currentScore: Double? = null,
    /** Server grade: observed 5/A, 4/B, 4/C, 3/D, 3/E, 2/FX, Зачёт, Незачёт and null. */
    public val rate: String? = null,
    /** Attempt number; usually 0 or 1 before retakes. */
    public val attempt: Int = 0,
    /** Final assessment type, for example Экзамен or Зачёт. */
    @SerialName("control_type")
    public val controlType: String = "",
    /** Server identifier of the final assessment type. */
    @SerialName("control_type_id")
    public val controlTypeId: Long = 0,
    /** Final assessment date-time; may be absent. The wire offset is normalized to UTC. */
    @SerialName("exam_date")
    @Serializable(with = WireInstantSerializer::class)
    public val examDate: Instant? = null,
    /** Whether a detailed assessment tree is available. */
    @SerialName("have_tree")
    public val haveTree: Boolean = false,
    /** External LMS discipline link; may be absent. */
    @SerialName("lms_link")
    public val lmsLink: String? = null,
    /** Final assessment teacher; may be absent. */
    public val teacher: RecordBookTeacher? = null,
)
