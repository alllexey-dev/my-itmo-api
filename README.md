<h1 align="center">MyItmoApi</h1>

<p align="center"><strong>Неофициальная Kotlin Multiplatform библиотека для работы с <a href="https://my.itmo.ru">MyITMO</a> и <a href="https://bars.itmo.ru">БАРС</a></strong></p>

## Возможности

- Вход через ITMO.ID в браузере (OAuth с PKCE) и автоматическое обновление токенов.
- Личное расписание и временные слоты пар.
- Зачётка, специализации и дерево контрольных мероприятий.
- Учебный план.
- Просмотр и поиск персоналий.
- Спорт:
  - расписание, фильтры и временные слоты;
  - личный календарь и выбранные секции;
  - запись на занятия и отмена записи;
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
- Kotlin 2.2 или новее.

Основные зависимости: Ktor, kotlinx.serialization, kotlinx-datetime и kotlinx.coroutines.

## Подключение

```kotlin
implementation("dev.alllexey:my-itmo-api-kmp:2.0.0")
```

## Клиент

Все запросы - `suspend`-функции. Токены хранятся в вашей реализации `TokenStorage`;
пример ниже держит их только в памяти, для постоянного хранения используйте
Keychain, Keystore или свою базу:

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

## Аутентификация

Входа по логину и паролю в 2.x нет: пользователь входит в браузере.

1. Создайте `Pkce.newVerifier()` и `Pkce.newState()` и сохраните их до callback.
2. Откройте `client.identity.loginUrl(Pkce.challenge(verifier), state)`.
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

Дальше access token обновляется автоматически. Выход - `client.tokens.replaceTokens(null)`.

Не записывайте токены, коды и cookie в логи.

## Использование API

Разделы клиента: `schedule`, `recordBook`, `personalities`, `studyplan`, `qr`,
`sport`, `election`, `finances`, `requests`, `system`. Методы возвращают ответ
MyITMO; `requireResult()` достаёт результат или бросает ошибку.

```kotlin
suspend fun readSchedule(client: MyItmoClient): List<Schedule> =
    client.schedule.getPersonalSchedule(
        LocalDate(2026, 1, 5), LocalDate(2026, 1, 11),
    ).requireResult()
```

## БАРС

`BarsClient` работает с `https://bars.itmo.ru` - отдельным сервисом с собственной
сессией. Токен MyITMO для него не подходит. Вход: `BarsLogin.loginUrl(state)` в
браузере, затем `extractCode(callbackUrl, state)` и `bars.login(code)`.

Сессия живёт около 30 минут, refresh token не выдаётся. Для тихого продления
передайте `BarsCodeSupplier`: при HTTP 401 клиент один раз запросит новый код и
повторит запрос. Если сессия ITMO.ID живёт в cookie WebView, код можно получить
без браузера:

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

Каталоги и журналы читаются в контексте периода, сохранённого на сервере; эта
настройка общая с веб-версией БАРС.

```kotlin
suspend fun readBarsDisciplines(bars: BarsClient): List<Discipline> =
    bars.withPeriod("2026/2027", Term.AUTUMN) {
        getDisciplines(withCheckpointPlansOnly = true)
    }
```

Идентификаторы БАРС не совпадают с `discipline_id` и `est_id` MyITMO.

## Ошибки

Все методы бросают `MyItmoException`:

- `Auth` - сессия закончилась, нужен повторный вход;
- `Network`, `Http`, `Decode` - временный сбой, сохранённая сессия не удаляется;
- `Api` - MyITMO вернул код ошибки.

## Версия 1.x

Java-библиотека `dev.alllexey:my-itmo-api:1.8.2` остаётся в корне репозитория.
Переход на 2.x описан в [docs/migration.md](docs/migration.md).

## Разработка

```bash
./gradlew -p kmp jvmTest                 # 2.x, JDK 17
./gradlew -p kmp iosSimulatorArm64Test   # 2.x, нужен Xcode
./mvnw verify -Dgpg.skip=true            # 1.x
```

## Особенности

MyITMO не предоставляет публичную документацию для всех используемых сервисов.
Модели основаны на наблюдаемых ответах API, поэтому сервер может добавлять новые
поля и справочные значения. Неизвестные, но подтверждённо присутствующие поля
отмечены в моделях закомментированными объявлениями до уточнения их типов.
