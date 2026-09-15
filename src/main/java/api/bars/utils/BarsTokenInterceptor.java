package api.bars.utils;

import api.bars.Bars;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/** Подставляет сохранённый заголовок авторизации только в запросы к хосту БАРС. */
public class BarsTokenInterceptor implements Interceptor {

    private final Bars bars;

    public BarsTokenInterceptor(Bars bars) {
        this.bars = bars;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        if (!bars.getConfiguration().getHost().equals(chain.request().url().host())
                || chain.request().header("Authorization") != null) {
            return chain.proceed(chain.request());
        }
        String authorization = bars.getStorage().getAuthorization();
        if (authorization == null) {
            return chain.proceed(chain.request());
        }
        return chain.proceed(chain.request().newBuilder().header("Authorization", authorization).build());
    }
}
