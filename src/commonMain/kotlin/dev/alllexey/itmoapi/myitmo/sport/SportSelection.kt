package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.Serializable

/** Sports discipline for which the student can attempt a selection. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportSelection(
    /** Discipline identifier. */
    public val id: Long = 0,
    /** Discipline display name. */
    public val name: String = "",
    /** Available selection levels and qualification standards; no null was documented. Empty is a client fallback. */
    public val requisites: List<SportRequisite> = emptyList(),
)
