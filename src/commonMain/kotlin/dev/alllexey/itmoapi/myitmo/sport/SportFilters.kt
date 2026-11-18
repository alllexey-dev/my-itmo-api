package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.IdValuePair
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Filter options, not a venue catalog. Observed 2026-09-09: -1 is Online, 0 is Other venues; off-site positive venue IDs may be absent here. Never replace a lesson venue with 0. */
@Serializable
public data class SportFilters(
    /** Available building id options. */
    @SerialName("building_id") public val buildingId: List<IdValuePair> = emptyList(),
    /** Available section id options. */
    @SerialName("section_id") public val sectionId: List<IdValuePair> = emptyList(),
    /** Available sport type id options. */
    @SerialName("sport_type_id") public val sportTypeId: List<IdValuePair> = emptyList(),
    /** Available teacher isu options. */
    @SerialName("teacher_isu") public val teacherIsu: List<IdValuePair> = emptyList(),
)
