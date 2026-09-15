package api.bars.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Настройка вида имя–значение ({@code GET config/}, {@code POST config/personal}).
 * Значения всегда строки, даже у логических и числовых настроек.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Setting {

    /** Присутствует в ответах; в теле {@code POST config/personal} не передаётся. */
    private Long id;

    /** Например {@code current_year}, {@code current_term}, {@code daily_message}. */
    private String name;

    /** Может быть {@code null} в общем конфиге. */
    private String value;

    public Setting(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
