package dev.alllexey.itmoapi.myitmo.recordbook

import kotlinx.serialization.Serializable

/** Teacher name in recordbook responses. Every name part may be absent. */
@Serializable
public data class RecordBookTeacher(
    /** Family name; may be absent. */
    public val surname: String? = null,
    /** Given name; may be absent. */
    public val name: String? = null,
    /** Patronymic; may be absent. */
    public val patronymic: String? = null,
)
