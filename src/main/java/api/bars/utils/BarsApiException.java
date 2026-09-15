package api.bars.utils;

import lombok.Getter;

/** Ошибка HTTP или сети при обращении к БАРС. Тело ответа и URL в сообщение не попадают. */
@Getter
public class BarsApiException extends RuntimeException {

    /** HTTP-код ответа или {@code null} для сетевой ошибки. */
    private final Integer httpCode;

    public BarsApiException(int httpCode) {
        super("BARS HTTP " + httpCode);
        this.httpCode = httpCode;
    }

    public BarsApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpCode = null;
    }

    /** Сессия отсутствует, истекла или отклонена; нужен новый вход. */
    public boolean isUnauthorized() {
        return httpCode != null && httpCode == 401;
    }
}
