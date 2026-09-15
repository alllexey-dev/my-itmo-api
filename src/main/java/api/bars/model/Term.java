package api.bars.model;

/**
 * Сезон учебного года в поле {@code selected_term}: веб-клиент подписывает
 * {@code 0} как весенний, {@code 1} как осенний. Это не сквозной номер семестра.
 */
public enum Term {
    SPRING(0),
    AUTUMN(1);

    private final int wireValue;

    Term(int wireValue) {
        this.wireValue = wireValue;
    }

    public int getWireValue() {
        return wireValue;
    }

    /** @throws IllegalArgumentException для значений, не наблюдавшихся у сервера */
    public static Term fromWire(int value) {
        for (Term term : values()) {
            if (term.wireValue == value) return term;
        }
        throw new IllegalArgumentException("Unknown BARS term " + value);
    }
}
