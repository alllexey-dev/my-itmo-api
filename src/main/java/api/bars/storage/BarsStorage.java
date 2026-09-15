package api.bars.storage;

/**
 * Хранилище сессии БАРС. В отличие от MyITMO у БАРС нет refresh token: хранится
 * только полное значение заголовка авторизации вида {@code Bearer ...},
 * которое сервер возвращает в response header {@code authorization}.
 */
public interface BarsStorage {

    /** Полный заголовок {@code Bearer ...} или {@code null}, если сессии нет. */
    String getAuthorization();

    /** Сохраняет заголовок; {@code null} удаляет сессию. */
    void setAuthorization(String authorization);
}
