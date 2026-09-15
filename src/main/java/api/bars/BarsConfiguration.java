package api.bars;

/**
 * Адреса и OIDC-параметры БАРС. Значения по умолчанию взяты из публичного
 * {@code https://bars.itmo.ru/runtime-config.json} и наблюдённого входа веб-клиента.
 */
public interface BarsConfiguration {

    /** Хост API, например {@code bars.itmo.ru}. */
    String getHost();

    /** REST base URL с завершающим слэшем, например {@code https://bars.itmo.ru/backend/rest/}. */
    String getRestUrl();

    /** OIDC issuer ITMO.ID. */
    String getIssuer();

    /** OIDC client id БАРС. */
    String getClientId();

    /** Зарегистрированный web callback клиента; на него ITMO.ID отдаёт {@code code}. */
    String getRedirectUri();

    BarsConfiguration DEFAULT = new Default();

    class Default implements BarsConfiguration {

        @Override
        public String getHost() {
            return "bars.itmo.ru";
        }

        @Override
        public String getRestUrl() {
            return "https://" + getHost() + "/backend/rest/";
        }

        @Override
        public String getIssuer() {
            return "https://id.itmo.ru/auth/realms/itmo";
        }

        @Override
        public String getClientId() {
            return "bars";
        }

        @Override
        public String getRedirectUri() {
            return "https://" + getHost() + "/rest/login";
        }
    }
}
