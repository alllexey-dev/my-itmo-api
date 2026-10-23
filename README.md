<h1 align="center">MyItmoApi</h1>

<p align="center"><strong>Неофициальная Kotlin Multiplatform библиотека для работы с <a href="https://my.itmo.ru">MyITMO</a> и <a href="https://bars.itmo.ru">БАРС</a></strong></p>

## Быстрый старт

Три шага: зависимость, клиент, вызов. Быстрый старт (`MyItmo`, `Blocking`,
`FileTokenStorage`) есть с 2.1.0; в 2.0.0 была только
[продвинутая настройка](#продвинутая-настройка).

### 1. Зависимость

Gradle (Kotlin, Android):

```kotlin
implementation("dev.alllexey:my-itmo-api-kmp:2.3.1")
```

Maven (Java):

```xml
<dependency>
    <groupId>dev.alllexey</groupId>
    <artifactId>my-itmo-api-kmp-jvm</artifactId>
    <version>2.3.1</version>
</dependency>
```

### 2-3. Клиент и вызов

Kotlin/JVM: расписание на неделю вперёд.

```kotlin
import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MOSCOW
import dev.alllexey.itmoapi.core.requireResult
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

suspend fun main() {
    MyItmo.login("my_cool_id", "my_strong_password").use { itmo ->
        val today = Clock.System.todayIn(MOSCOW)
        val week = itmo.schedule.getPersonalSchedule(today, today.plus(6, DateTimeUnit.DAY)).requireResult()
        for (day in week) {
            println("${day.date}: " + day.lessons.joinToString { "${it.timeStart} ${it.subject}" })
        }
    }
}
```

Кроме логина и пароля:

- `MyItmo.fromRefreshToken(token)` - из refresh token (например, сохранённого
  раньше); токен сразу проверяется;
- `MyItmo.fromStorage(storage)` - из сессии, уже лежащей в хранилище, без
  запросов к сети.

Разделы: `schedule`, `recordBook`, `personalities`, `studyplan`, `qr`, `sport`,
`election`, `finances`, `requests`, `system` и `bars`. Методы MyITMO возвращают
ответ-обёртку; `requireResult()` достаёт результат или бросает ошибку. `close()`
(или `use`) освобождает HTTP-клиент.

Java: любой `suspend`-метод вызывается через `Blocking.call`, последним
аргументом передаётся `c`. Результат присваивайте переменной с типом.

```java
import dev.alllexey.itmoapi.Blocking;
import dev.alllexey.itmoapi.MyItmo;
import dev.alllexey.itmoapi.core.DataResponse;
import dev.alllexey.itmoapi.myitmo.schedule.Schedule;
import kotlinx.datetime.LocalDate;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (MyItmo itmo = Blocking.call(c -> MyItmo.login("my_cool_id", "my_strong_password", c))) {
            LocalDate from = new LocalDate(2026, 1, 5);
            LocalDate to = new LocalDate(2026, 1, 11);
            DataResponse<List<Schedule>> week = Blocking.call(c -> itmo.getSchedule().getPersonalSchedule(from, to, c));
            for (Schedule day : week.getData()) {
                System.out.println(day.getDate() + ": " + day.getLessons().size() + " пар");
            }
        }
    }
}
```

Android: все методы `suspend` и не блокируют главный поток. Нужно разрешение
`android.permission.INTERNET`. В приложении лучше не спрашивать пароль самому:
пусть пользователь входит на странице ITMO.ID (см. [вход в браузере](#вход-в-браузере)),
а токены хранятся в своём `TokenStorage` поверх Keystore.

```kotlin
import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.myitmo.schedule.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

// scope - viewModelScope или lifecycleScope: запросы не блокируют главный поток.
// itmo - один на ViewModel (MyItmo.fromStorage(...)), itmo.close() в onCleared().
fun loadWeek(
    scope: CoroutineScope,
    itmo: MyItmo,
    monday: LocalDate,
    show: (List<Schedule>) -> Unit,
    fail: (MyItmoException) -> Unit,
) {
    scope.launch {
        try {
            show(itmo.schedule.getPersonalSchedule(monday, monday.plus(6, DateTimeUnit.DAY)).requireResult())
        } catch (error: MyItmoException) {
            fail(error) // MyItmoException.Auth - нужен повторный вход
        }
    }
}
```

## Сохранение токена

По умолчанию токены живут в памяти и пропадают с процессом. Чтобы скрипт или
сервер не входил заново при каждом запуске, сохраните их в файл:

```kotlin
suspend fun openSession(username: String, password: String): MyItmo {
    val storage = FileTokenStorage(Path("itmo-tokens.json"))
    return if (storage.read() != null) MyItmo.fromStorage(storage) else MyItmo.login(username, password, storage)
}
```

`FileTokenStorage` (только JVM) пишет файл атомарно и с правами только для
владельца, `null` удаляет его. В файле refresh token в открытом виде: не кладите
его в репозиторий. На Android и iOS реализуйте `TokenStorage` поверх Keystore или
Keychain (пример - в [продвинутой настройке](#продвинутая-настройка)).

Выход: `itmo.client.tokens.replaceTokens(null)`.

## Ошибки

Все методы бросают `MyItmoException`:

- `Auth` - нужен вход: неверный логин или пароль, ITMO.ID попросил
  дополнительный шаг (например, 2FA), сессия закончилась (HTTP 401, в MyITMO -
  после одной попытки обновить токен), её нет в хранилище или ITMO.ID отклонил
  refresh token;
- `Http` - неуспешный HTTP-статус, в том числе 403: сессия действительна, но
  доступа к ресурсу нет;
- `Network`, `Decode` - временный сбой;
- `Api` - MyITMO вернул код ошибки.

При `Network`, `Http` и `Decode` сохранённая сессия не удаляется. В сообщениях
исключений нет токенов, паролей и ответов сервера; сами тоже не пишите их в логи.

## Спорт

`sport.signInLesson(id)` записывает на одно занятие и не бросает `MyItmoException`,
а возвращает `SportSignResult`:

- `Booked` - записали;
- `Full` - нет свободных мест, можно попробовать позже;
- `Rejected` - MyITMO отказал по другому правилу (лимит, пересечение и т.п.),
  повтор не поможет; причина в `refusal.serverMessage`, её не логируйте;
- `RetryLater` - решения по занятию нет (сеть, HTTP-статус, сессия), причина в
  `cause`; `Auth` значит, что нужен новый вход.

`signInLessons` по-прежнему отдаёт ответ-обёртку и бросает ошибки как раньше.

## Время (Москва)

Время в ответах (`Instant`) абсолютное: показывайте его в часовом поясе ИТМО
`MOSCOW` (`dev.alllexey.itmoapi.core.MOSCOW`, `Europe/Moscow`), например
`instant.toLocalDateTime(MOSCOW)`, а не в поясе устройства. "Сегодня" по ИТМО -
`Clock.System.todayIn(MOSCOW)`. Строки `HH:mm` в расписании и спорте уже заданы
по Москве. Даты - `kotlinx.datetime.LocalDate`.

## БАРС

`https://bars.itmo.ru` - отдельный сервис с собственной сессией. После
`MyItmo.login` он доступен сразу как `itmo.bars`: сессия БАРС получается и
продлевается по cookie того же входа в ITMO.ID, без второго пароля. После
`fromRefreshToken` и `fromStorage` cookie нет, и `itmo.bars` бросает `Auth`, пока
вы не войдёте в БАРС сами (`BarsLogin` ниже).

Каталоги и журналы читаются в контексте периода, сохранённого на сервере; эта
настройка общая с веб-версией БАРС.

```kotlin
suspend fun readBarsDisciplines(bars: BarsClient): List<Discipline> =
    bars.withPeriod("2026/2027", Term.AUTUMN) {
        getDisciplines(withCheckpointPlansOnly = true)
    }
```

Вызов: `readBarsDisciplines(itmo.bars)`. Идентификаторы БАРС не совпадают с
`discipline_id` и `est_id` MyITMO.

## Продвинутая настройка

`MyItmo` - тонкая обёртка над `MyItmoClient` (`itmo.client`). Собирайте
`MyItmoClient` сами, если нужны вход в браузере, своё хранилище, свой движок
или общий замок обновления токенов.

### Клиент

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

### Вход в браузере

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

### Вызовы через клиент

```kotlin
suspend fun readSchedule(client: MyItmoClient): List<Schedule> =
    client.schedule.getPersonalSchedule(
        LocalDate(2026, 1, 5), LocalDate(2026, 1, 11),
    ).requireResult()
```

### БАРС без пароля

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

### Сессии ITMO.ID

Каждый вход по паролю - отдельная сессия ITMO.ID на 90 дней, она видна на
`https://id.itmo.ru/activity`. `ItmoIdClient.logout(refreshToken)` завершает
ровно сессию этого токена, остальные живут. `TokenSet.sessionId` - её `sid`.

Список и завершение любых сессий аккаунта идут через `ItmoIdSessions` с токеном
клиента `profile` (страницы id.itmo.ru), токен MyITMO для этого не подходит:
`profileToken(cookieHeader)` получает его по живой cookie ITMO.ID без пароля
(`null`, если cookie мертва), `list(bearer)` возвращает `ItmoIdSession`, а
`end(bearer, sid)` завершает сессию (уже завершённая - тоже успех). Cookie
ITMO.ID - это весь аккаунт: не передавайте её дальше устройства.

Подпись строки на `/activity` задаёт User-Agent входа, и потом она не меняется.
`ItmoSessionLabel.browserUserAgent(platform)` - обычный десктопный Chrome,
`ItmoSessionLabel.botUserAgent(client, name, version, platform)` - честная
подпись вида `Linux, Browser: ITMO Widgets agent nas bot/1.2`.

## Переход с 1.x

Java-библиотека `dev.alllexey:my-itmo-api:1.8.2` остаётся в корне репозитория и
получает только исправления.

| 1.x | 2.x |
|---|---|
| `new MyItmo()` + `myItmo.auth(login, password)` | `MyItmo.login(login, password)` |
| `getStorage().setRefreshToken(token)` + `forceRefreshTokens()` | `MyItmo.fromRefreshToken(token)` |
| `myItmo.setStorage(storage)` | `MyItmo.login(login, password, storage)`, `MyItmo.fromStorage(storage)` |
| `getApi().getPersonalSchedule(from, to).execute().body()` | `itmo.schedule.getPersonalSchedule(from, to).requireResult()` |
| То же из Java | `Blocking.call(c -> itmo.getSchedule().getPersonalSchedule(from, to, c))` |
| `getApi().getSpecializations()` и другие методы | `itmo.recordBook.getSpecializations()`: методы разнесены по разделам |
| `forceRefreshTokens()` | `itmo.client.tokens.forceRefresh()` |
| `new Bars()` + `bars.auth(login, password)`, `authWithSession()` | `itmo.bars` после `MyItmo.login` |
| `ApiException`, `TokenRefreshException`, `BarsApiException` | `MyItmoException.Api`, `Auth`, `Http`, `Network`, `Decode` |
| `java.time.LocalDate` | `kotlinx.datetime.LocalDate` (`toKotlinLocalDate()`) |

Подробности - в [docs/migration.md](docs/migration.md).

## Возможности

- Вход по логину и паролю, refresh token или в браузере через ITMO.ID (OAuth с
  PKCE) и автоматическое обновление токенов.
- Сессии ITMO.ID: выход из своей сессии, список и завершение сессий аккаунта,
  подпись сессии через User-Agent.
- Личное расписание и временные слоты пар.
- Зачётка, специализации и дерево контрольных мероприятий.
- Учебный план.
- Просмотр и поиск персоналий.
- Спорт:
  - расписание, фильтры и временные слоты;
  - личный календарь и выбранные секции;
  - запись на занятия и отмена записи, типизированный итог записи;
  - баллы, попытки, задолженность и медицинская группа;
  - отборы, экстернат и специальные проекты.
- Выбор дисциплин и потоков.
- Главный экран, меню и каталог сервисов MyITMO.
- Заявки пользователя.
- Суммарные выплаты по категориям.
- QR-пропуск в корпуса в HEX-формате.
- БАРС: вход через ITMO.ID, выбор учебного периода, каталоги дисциплин и потоков,
  журнал с баллами и подтверждёнными оценками.

## Требования

- JVM 11 или новее, Android с minSdk 26, iOS (`iosArm64`, `iosSimulatorArm64`).
- Kotlin 2.2 или новее; из Java - через `Blocking`.

Основные зависимости: Ktor, kotlinx.serialization, kotlinx-datetime и kotlinx.coroutines.

## Разработка

```bash
./gradlew -p kmp jvmTest                 # 2.x, JDK 17
./gradlew -p kmp iosSimulatorArm64Test   # 2.x, нужен Xcode
./mvnw verify -Dgpg.skip=true            # 1.x
```

Как добавить эндпоинт, фикстуру и как устроены релизы - в [CONTRIBUTING.md](CONTRIBUTING.md).

## Особенности

MyITMO не предоставляет публичную документацию для всех используемых сервисов.
Модели основаны на наблюдаемых ответах API, поэтому сервер может добавлять новые
поля и справочные значения. Неизвестные, но подтверждённо присутствующие поля
отмечены в моделях закомментированными объявлениями до уточнения их типов.
