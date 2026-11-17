# Продвинутая настройка

Быстрый старт, хранение токенов и ошибки описаны в [README](../README.md).

`MyItmo` - тонкая обёртка над `MyItmoClient` (`itmo.client`). Собирайте
`MyItmoClient` сами, если нужны вход в браузере, своё хранилище, свой движок
или общий замок обновления токенов.

## Клиент

```kotlin
class MemoryTokens : TokenStorage {
    private var snapshot: TokenSet? = null
    override suspend fun read(): TokenSet? = snapshot
    override suspend fun write(tokens: TokenSet?) { snapshot = tokens }
    override fun toString(): String = "MemoryTokens(redacted)"
}
```

```kotlin
fun createClient(
    storage: TokenStorage,
    engine: HttpClientEngine,
    clock: Clock,
    configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
): MyItmoClient = MyItmoClient(configuration, storage, engine, clock)
```

Движок `defaultEngine()` создаёт OkHttp на JVM и Android и Darwin на iOS. Движок
принадлежит вызывающему: закройте сначала клиент (`client.close()`), потом движок.

## Вход в браузере

Пользователь вводит пароль только на странице ITMO.ID:

1. Создайте `Pkce.newVerifier()` и `Pkce.newState()` и сохраните их до callback.
2. Откройте `client.identity.loginUrl(Pkce.challenge(verifier), state)` в WebView
   и перехватите переход на `https://my.itmo.ru/login/callback`.
3. Обменяйте callback на токены:

```kotlin
suspend fun completeLogin(
    client: MyItmoClient,
    callbackUrl: String,
    expectedState: String,
    verifier: String,
): Boolean {
    val configuration = client.configuration.itmoId
    val callback = CallbackUrl(configuration.redirectUri, configuration.issuer)
    val code = callback.extractCode(callbackUrl, expectedState) ?: return false
    client.tokens.replaceTokens(client.identity.exchange(code, verifier))
    return true
}
```

Дальше access token обновляется автоматически.

Если одно хранилище токенов делят несколько клиентов или процессов (например,
приложение и виджет или расширение iOS), передайте им всем один
`TokenRefreshGuard` (параметр `refreshGuard` у `MyItmoClient`) с межпроцессной
блокировкой, например файловой в App Group. Без неё ITMO.ID может отклонить
второе одновременное обновление; `TokenManager` тогда перечитывает хранилище и
берёт сессию, сохранённую соседом, но это страховка, а не замена блокировке.

## Вызовы через клиент

```kotlin
suspend fun readSchedule(client: MyItmoClient): List<Schedule> =
    client.schedule.getPersonalSchedule(
        LocalDate(2026, 1, 5), LocalDate(2026, 1, 11),
    ).requireResult()
```

## БАРС без пароля

`BarsClient` можно создать отдельно. Вход: `BarsLogin.loginUrl(state)` в
браузере, затем `extractCode(callbackUrl, state)` и `bars.login(code)`. Сессия
живёт около 30 минут, refresh token не выдаётся. Для тихого продления передайте
`BarsCodeSupplier`: при HTTP 401 клиент один раз запросит новый код и повторит
запрос. Если сессия ITMO.ID живёт в cookie WebView, код можно получить без
браузера:

```kotlin
suspend fun replayBarsLogin(
    login: BarsLogin,
    bars: BarsClient,
    state: String,
    cookieHeader: String?,
    saveCookies: suspend (List<String>) -> Unit,
): BarsSessionCode.Outcome {
    val result = login.requestCodeWithCookies(state, cookieHeader)
    saveCookies(result.setCookies)
    when (result.outcome) {
        BarsSessionCode.Outcome.CODE -> bars.login(requireNotNull(result.code))
        BarsSessionCode.Outcome.LOGIN_REQUIRED -> Unit
        BarsSessionCode.Outcome.REJECTED -> Unit
        BarsSessionCode.Outcome.HTTP_ERROR -> Unit
    }
    return result.outcome
}
```

## Сессии ITMO.ID

Каждый вход по паролю - отдельная сессия ITMO.ID на 90 дней, она видна на
`https://id.itmo.ru/activity`. `ItmoIdClient.logout(refreshToken)` завершает
ровно сессию этого токена, остальные живут. `TokenSet.sessionId` - её `sid`.

Список и завершение любых сессий аккаунта идут через `ItmoIdSessions` с токеном
клиента `profile` (страницы id.itmo.ru), токен My ITMO для этого не подходит:
`profileToken(cookieHeader)` получает его по живой cookie ITMO.ID без пароля
(`null`, если cookie мертва), `list(bearer)` возвращает `ItmoIdSession`, а
`end(bearer, sid)` завершает сессию (уже завершённая - тоже успех). Cookie
ITMO.ID - это весь аккаунт: не передавайте её дальше устройства.

Подпись строки на `/activity` задаёт User-Agent входа, и потом она не меняется.
`ItmoSessionLabel.browserUserAgent(platform)` - обычный десктопный Chrome,
`ItmoSessionLabel.botUserAgent(client, name, version, platform)` - честная
подпись вида `Linux, Browser: ITMO Widgets agent nas bot/1.2`.
