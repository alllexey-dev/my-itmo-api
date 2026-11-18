package dev.alllexey.itmoapi.myitmo.personalities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Room associated with a profile. Checked profiles only had empty rooms arrays: individual
 * field types/nullability were not confirmed by that probe. String declarations follow the legacy model;
 * non-null defaults are not a claim that the API requires these keys. */
@Serializable
public data class Room(
    /** Room number; nonempty room observations are still needed. */
    @SerialName("room_number") public val roomNumber: String = "",
    /** Building name/address; nonempty room observations are still needed. */
    @SerialName("bld_name") public val bldName: String = "",
)
