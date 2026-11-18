# Вход и сессия

Все сервисы ИТМО пускают через ITMO.ID. Библиотека получает там токены, хранит их в `TokenStorage` и сама
обновляет access token перед запросом. Как это устроено на уровне HTTP, описано в [spec/auth.md](../spec/auth.md).

Классы этой страницы: `MyItmo` - в пакете `dev.alllexey.itmoapi`; `TokenStorage`, `TokenSet`,
`InMemoryTokenStorage`, `FileTokenStorage`, `Pkce`, `CallbackUrl` - в `dev.alllexey.itmoapi.itmoid`;
`MyItmoClient` и `MyItmoConfiguration` - в `dev.alllexey.itmoapi.myitmo`.

## Способы получить клиент

| Способ | Когда | Запросы при создании | БАРС |
|---|---|---|---|
| `MyItmo.login(username, password)` | Разовый запуск, свой аккаунт | Вход в ITMO.ID | Работает |
| `MyItmo.loginOrResume(username, password, storage)` | Скрипт или сервер с постоянным хранилищем | Нет, пока сохранённый access token действует; обновление токена, если он истёк; вход по паролю, если сессии нет или ITMO.ID её отклонил | Работает, только если в этом запуске был вход по паролю |
| `MyItmo.fromRefreshToken(token)` | Есть refresh token из другого места | Одно обновление токена: так он сразу проверяется | Нужен отдельный вход |
| `MyItmo.fromStorage(storage)` | Сессия уже лежит в хранилище | Нет | Нужен отдельный вход |

`login` отправляет пароль один раз в форму ITMO.ID и нигде его не хранит. Неверный логин или пароль,
заблокированный аккаунт и дополнительный шаг входа (например, 2FA) - это `MyItmoException.Auth`.

Почему у БАРС своя колонка: его сессия получается по cookie ITMO.ID, а они есть только после входа по паролю и
живут в памяти. Подробности и что делать скрипту - в [bars.md](bars.md).

В приложении для других людей пароль лучше не спрашивать вообще: пусть пользователь входит на странице ITMO.ID
([вход в браузере](#вход-в-браузере)).

## Хранение токенов

По умолчанию токены живут в памяти (`InMemoryTokenStorage`) и пропадают вместе с процессом. Каждый вход по
паролю при этом создаёт новую сессию ITMO.ID на 90 дней (их видно на `https://id.itmo.ru/activity`), так что
скрипт, который входит заново при каждом запуске, копит сессии. Поэтому либо храните токены, либо вызывайте
`logout()` в конце работы.

На JVM есть готовое файловое хранилище, а `loginOrResume` сам решает, нужен ли вход:

```kotlin
suspend fun openSession(username: String, password: String): MyItmo =
    MyItmo.loginOrResume(username, password, FileTokenStorage(Path("itmo-tokens.json")))
```

Здесь `Path` - это `kotlin.io.path.Path`. `FileTokenStorage` пишет файл атомарно и с правами только для
владельца, запись `null` удаляет его. В файле лежит refresh token в открытом виде: не кладите его в репозиторий.

Сроки жизни: access token - 30 минут, refresh token - 30 дней и меняется при каждом обновлении. Библиотека
сохраняет новую пару в хранилище сама; записывать токены мимо неё не нужно.

### Когда сохранённая сессия перестала работать

Сессия заканчивается, если ею не пользовались 30 дней, если прошло 90 дней со входа или если её завершили на
`https://id.itmo.ru/activity`. Что происходит дальше:

- Если сохранённый access token истёк (после последнего запуска прошло больше 30 минут), `loginOrResume`
  обновляет его сразу при создании. Истёкшую или отклонённую ITMO.ID сессию он при этом видит сам и входит по
  паролю заново. Из-за этого запроса `loginOrResume` может бросить `Network` и при сохранённой сессии.
- Если access token ещё действует, запросов при создании нет. Сессию, которую за это время завершили на сервере,
  заметит первый запрос и бросит `MyItmoException.Auth`.
- `Auth` не очищает хранилище: старая запись остаётся, пока её не заменят. Чтобы восстановиться, закройте
  старый объект и войдите заново с тем же хранилищем - новая сессия перезапишет старую:

```kotlin
suspend fun signInAgain(old: MyItmo, username: String, password: String, storage: TokenStorage): MyItmo {
    old.close()
    return MyItmo.login(username, password, storage)
}
```

### Своё хранилище

На Android и iOS реализуйте `TokenStorage` поверх Keystore или Keychain. Интерфейс - два метода, которые читают
и пишут сессию целиком:

```kotlin
class MemoryTokens : TokenStorage {
    private var snapshot: TokenSet? = null
    override suspend fun read(): TokenSet? = snapshot
    override suspend fun write(tokens: TokenSet?) { snapshot = tokens }
    override fun toString(): String = "MemoryTokens(redacted)"
}
```

В настоящем хранилище вместо поля в памяти сохраняются пять значений `TokenSet`: `accessToken`,
`accessExpiresAt`, `refreshToken`, `refreshExpiresAt` и `idToken`. Обратно сессия собирается конструктором
`TokenSet(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, idToken)`. Запись `null` означает выход:
удалите сохранённое.

## Проверка сессии и выход

```kotlin
suspend fun signOut(itmo: MyItmo) {
    if (itmo.hasSession()) itmo.logout()
}
```

- `hasSession()` - есть ли в хранилище сессия с непросроченным refresh token. В сеть не ходит.
- `logout()` завершает эту сессию в ITMO.ID и очищает хранилище и сессию БАРС. Локальная сессия очищается, даже
  если запрос к ITMO.ID не удался; ошибка при этом пробрасывается. Остальные сессии аккаунта продолжают жить.

Обновить access token принудительно: `itmo.client.tokens.forceRefresh()`. Это нужно редко, например когда QR-пропуск
не выдаётся со старым токеном.

## Вход в браузере

Пользователь вводит пароль только на странице ITMO.ID, приложение его не видит. Для этого нужен `MyItmoClient` -
клиент уровнем ниже `MyItmo`, которому хранилище, HTTP-движок и часы передаются явно:

```kotlin
fun createClient(storage: TokenStorage): MyItmoClient =
    MyItmoClient(MyItmoConfiguration.DEFAULT, storage, defaultEngine(), Clock.System)
```

`defaultEngine()` (`dev.alllexey.itmoapi.core`) - HTTP-движок по умолчанию, `Clock` - `kotlin.time.Clock`.

Дальше три шага:

1. Создайте `Pkce.newVerifier()` и `Pkce.newState()` и сохраните их до возврата из браузера.
2. Откройте `client.identity.loginUrl(Pkce.challenge(verifier), state)` в WebView и перехватите переход на
   `https://my.itmo.ru/login/callback`. Свой адрес возврата подставить нельзя: ITMO.ID принимает только
   зарегистрированный.
3. Обменяйте адрес возврата на токены:

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

После этого разделы доступны как `client.schedule`, `client.sport` и так далее, а токен обновляется сам:

```kotlin
suspend fun readSchedule(client: MyItmoClient): List<Schedule> =
    client.schedule.getPersonalSchedule(
        LocalDate(2026, 1, 5), LocalDate(2026, 1, 11),
    )
```

Выход для `MyItmoClient`: `client.tokens.logout()`. БАРС при таком входе подключается отдельно, см.
[bars.md](bars.md#вход-без-пароля).

## Сессии ITMO.ID

Каждый вход по паролю или в браузере - отдельная сессия ITMO.ID. `TokenSet.sessionId` - её идентификатор (`sid`).

Список и завершение любых сессий аккаунта идут через `ItmoIdSessions` с токеном клиента `profile` (страницы
id.itmo.ru); токен My ITMO для этого не подходит:

- `profileToken(cookieHeader)` получает такой токен по живой cookie ITMO.ID без пароля (`null`, если cookie
  мертва);
- `list(bearer)` возвращает список `ItmoIdSession`;
- `end(bearer, sid)` завершает сессию; уже завершённая - тоже успех.

Cookie ITMO.ID - это весь аккаунт: не передавайте её дальше устройства.

Подпись строки на `/activity` задаёт User-Agent входа, и потом она не меняется. Библиотека только собирает
строку: поставьте её заголовком `User-Agent` в своём HTTP-движке ([свой движок](advanced.md#свой-http-движок)).
`ItmoSessionLabel.browserUserAgent(platform)` - обычный десктопный Chrome,
`ItmoSessionLabel.botUserAgent(client, name, version, platform)` - честная подпись вида
`Linux, Browser: ITMO Widgets agent nas bot/1.2`.
