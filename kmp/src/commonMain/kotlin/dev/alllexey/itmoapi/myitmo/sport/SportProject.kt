package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Special format for obtaining sports credit, such as an externship. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportProject(
    /** Project identifier. */
    public val id: Long = 0,
    /** Project display name. */
    public val name: String = "",
    /** Project description. */
    public val description: String = "",
    /** Whether the student is enrolled in this project. */
    public val signed: Boolean = false,
    /** Total participant capacity, in people. */
    public val limit: Int = 0,
    /** Available places reported by the server, in people. */
    public val available: Int = 0,
    /** Link to project rules or instructions. */
    @SerialName("instruction_link")
    public val instructionLink: String = "",
    /** Text confirming that the student has read the rules. */
    @SerialName("instruction_description")
    public val instructionDescription: String = "",
    /** Whether prerequisites for enrollment are satisfied. */
    @SerialName("requisite_available")
    public val requisiteAvailable: Boolean = false,
    /** Student-provided supporting-document link; may be absent. */
    public val link: String? = null,
)
