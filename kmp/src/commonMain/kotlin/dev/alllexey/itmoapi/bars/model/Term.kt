package dev.alllexey.itmoapi.bars.model

/** Academic-year season: SPRING 0 / AUTUMN 1, not a continuous semester number. */
public enum class Term(public val wireValue: Int) {
    SPRING(0),
    AUTUMN(1);

    public companion object {
        /** Rejects values not observed on the wire. */
        public fun fromWire(value: Int): Term = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("Unknown BARS term $value")
    }
}
