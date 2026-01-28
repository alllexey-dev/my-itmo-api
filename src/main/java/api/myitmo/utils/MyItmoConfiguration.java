package api.myitmo.utils;

public interface MyItmoConfiguration {

    String getHost();

    String getUrl();

    String getAuthClientId();

    String getAuthRedirectUri();

    MyItmoConfiguration DEFAULT = new Default();

    MyItmoConfiguration DEV = new Dev();

    class Default implements MyItmoConfiguration {

        @Override
        public String getHost() {
            return "my.itmo.ru";
        }

        @Override
        public String getUrl() {
            return "https://" + getHost();
        }

        @Override
        public String getAuthClientId() {
            return "student-personal-cabinet";
        }

        @Override
        public String getAuthRedirectUri() {
            return "https://my.itmo.ru/login/callback";
        }
    }

    class Dev implements MyItmoConfiguration {

        @Override
        public String getHost() {
            return "dev.my.itmo.su";
        }

        @Override
        public String getUrl() {
            return "https://" + getHost();
        }

        @Override
        public String getAuthClientId() {
            return "student-personal-cabinet-dev";
        }

        @Override
        public String getAuthRedirectUri() {
            return "https://dev.my.itmo.su/login/callback";
        }
    }
}