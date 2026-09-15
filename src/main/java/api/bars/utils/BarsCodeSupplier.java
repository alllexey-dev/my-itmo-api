package api.bars.utils;

/**
 * Источник нового authorization code для тихого продления сессии.
 *
 * <p>Access token БАРС живёт около 30 минут и сервером не продлевается. Если клиенту
 * задан поставщик кода, при HTTP 401 он вызывается один раз, код обменивается на новый
 * заголовок и запрос повторяется. Реализация обычно проходит OIDC-вход через уже
 * существующую сессию ITMO.ID (cookie в WebView или в cookie jar OkHttp).</p>
 */
@FunctionalInterface
public interface BarsCodeSupplier {

    /**
     * @param state значение {@code state}, которое должно вернуться в callback
     * @return authorization code или {@code null}, если без участия пользователя код получить нельзя
     */
    String obtainCode(String state);
}
