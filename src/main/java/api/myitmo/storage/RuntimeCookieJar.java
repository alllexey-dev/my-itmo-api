package api.myitmo.storage;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RuntimeCookieJar implements CookieJar {

    private final HashMap<String, HashMap<String, Cookie>> allCookies = new HashMap<>();

    @Override
    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
        HashMap<String, Cookie> hostCookies = allCookies.computeIfAbsent(httpUrl.host(), s ->  new HashMap<>());
        list.forEach(cookie -> hostCookies.put(cookie.name(), cookie));
    }

    @NotNull
    @Override
    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
        String host = httpUrl.host();
        return allCookies.containsKey(host) ? new ArrayList<>(allCookies.get(host).values()) : Collections.emptyList();
    }
}
