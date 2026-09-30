package api.bars.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Итог запроса authorization code по cookie сессии ITMO.ID
 * ({@link BarsAuthHelper#requestCodeWithCookies}).
 *
 * <p>Наблюдённое поведение: с живой SSO-сессией ITMO.ID отвечает на запрос авторизации
 * клиента {@code bars} кодом 302 на его callback с {@code code} и тем же {@code state}
 * (проверено 2026-09-15). Без сессии ITMO.ID показывает форму входа (200). Код ответа
 * 4xx/5xx означает сбой сервера, а не конец сессии.</p>
 *
 * <p>{@link #getSetCookies()} — заголовки {@code Set-Cookie} ответа ITMO.ID в порядке
 * ответа; библиотека их не сохраняет, что с ними делать, решает вызывающий (например,
 * записать обратно в хранилище cookie браузера). {@link #toString()} не содержит кода
 * и cookie.</p>
 */
public final class BarsSessionCode {

    public enum Outcome {
        /** ITMO.ID вернул точный callback с тем же {@code state} и кодом. */
        CODE,
        /** Сессии ITMO.ID нет: ответ — страница ITMO.ID или форма входа, либо cookie не переданы. */
        LOGIN_REQUIRED,
        /** Переход на чужой адрес или callback без годного кода (другой {@code state}, {@code error}). */
        REJECTED,
        /** Ответ 4xx/5xx или переход без {@code Location}. */
        HTTP_ERROR
    }

    private final Outcome outcome;
    private final String code;
    private final int httpCode;
    private final List<String> setCookies;

    private BarsSessionCode(Outcome outcome, String code, int httpCode, List<String> setCookies) {
        this.outcome = outcome;
        this.code = code;
        this.httpCode = httpCode;
        this.setCookies = setCookies == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(setCookies));
    }

    public static BarsSessionCode code(String code, int httpCode, List<String> setCookies) {
        if (code == null || code.isEmpty()) throw new IllegalArgumentException("Code must not be empty");
        return new BarsSessionCode(Outcome.CODE, code, httpCode, setCookies);
    }

    public static BarsSessionCode loginRequired(int httpCode, List<String> setCookies) {
        return new BarsSessionCode(Outcome.LOGIN_REQUIRED, null, httpCode, setCookies);
    }

    public static BarsSessionCode rejected(int httpCode, List<String> setCookies) {
        return new BarsSessionCode(Outcome.REJECTED, null, httpCode, setCookies);
    }

    public static BarsSessionCode httpError(int httpCode, List<String> setCookies) {
        return new BarsSessionCode(Outcome.HTTP_ERROR, null, httpCode, setCookies);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    /** Authorization code при {@link Outcome#CODE}, иначе {@code null}. */
    public String getCode() {
        return code;
    }

    /** HTTP-код ответа ITMO.ID или {@code 0}, если запроса не было. */
    public int getHttpCode() {
        return httpCode;
    }

    /** Неизменяемый список {@code Set-Cookie} ответа; пустой, если запроса не было. */
    public List<String> getSetCookies() {
        return setCookies;
    }

    @Override
    public String toString() {
        return "BarsSessionCode{outcome=" + outcome + ", httpCode=" + httpCode + "}";
    }
}
