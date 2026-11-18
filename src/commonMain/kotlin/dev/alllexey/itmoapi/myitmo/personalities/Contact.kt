package dev.alllexey.itmoapi.myitmo.personalities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Contacts grouped by type. Employee samples contain both keys, no nulls; student and
 * service profiles have an empty outer contacts list. Missing nested keys were not observed. */
@Serializable
public data class Contact(
    /** Contact values, observed as a nonempty string array. */
    public val contact: List<String> = emptyList(),
    /** Display name of the contact type, observed as a string. */
    @SerialName("contact_alias") public val contactAlias: String = "",
)
