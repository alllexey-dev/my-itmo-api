package api.myitmo.utils;

import api.myitmo.MyItmo;
import api.myitmo.model.other.TokenResponse;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TokenInterceptor implements Interceptor {

    private final List<String> hostsToIntercept = Arrays.asList(MyItmo.MY_ITMO_HOST, "qr.itmo.su");

    private final MyItmo myItmo;

    public TokenInterceptor(MyItmo myItmo) {
        this.myItmo = myItmo;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        // we intercept only MyITMO (and some other) requests
        if (!hostsToIntercept.contains(chain.request().url().host())) {
            return chain.proceed(chain.request());
        }

        try {
            TokenResponse tokens = myItmo.getValidTokens();
            return chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer " + tokens.getAccessToken()).build());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
