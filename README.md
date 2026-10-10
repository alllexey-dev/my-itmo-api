# MyItmoApi 2.x

Неофициальный Kotlin Multiplatform клиент для ITMO.ID, MyITMO и БАРС.
2.x находится в `kmp/` рядом с совместимой Java-веткой 1.x.

## Подключение и платформы

Координаты 2.x: `dev.alllexey:my-itmo-api-kmp:2.0.0`, версия исходников
`2.0.0`. Зависимость в `commonMain`:

```kotlin
implementation("dev.alllexey:my-itmo-api-kmp:2.0.0")
```

Цели: `jvm`, `iosArm64`, `iosSimulatorArm64`. JVM-артефакт также используется
на Android с minSdk 26; байткод JVM 11. Все сетевые операции - `suspend`,
без Retrofit `Call` и блокирующего фасада. Пакеты начинаются с
`dev.alllexey.itmoapi`. Для сборки исходников нужен JDK 17.

## Клиент и хранение сессии

`MyItmoClient` принимает неизменяемую `MyItmoConfiguration`, `TokenStorage`,
движок Ktor и `kotlin.time.Clock`. `MyItmoConfiguration.DEFAULT` и `.DEV`
задают официальные окружения; `baseUrl`, `itmoId`, `acceptLanguage` (по
умолчанию `ru`) и `clockSkew` (30 секунд) можно передать явно.
`ItmoIdConfiguration` задаёт `issuer`, `clientId` и точный HTTPS `redirectUri`.

`core.defaultEngine()` создаёт OkHttp на JVM/Android и Darwin на Apple.
Движок принадлежит вызывающему: после завершения запросов вызовите
`client.close()`, затем `engine.close()`. Переданный движок сам должен
отключать cookie, кеш и автоматические переходы; для БАРС это обязательное
условие. В библиотеке нет глобального клиента, cookie jar или сессии.

Ниже - функции из [ReadmeSamplesTest](kmp/src/jvmTest/kotlin/dev/alllexey/itmoapi/ReadmeSamplesTest.kt),
реально компилируемые и вызываемые тестами с `MockEngine` и синтетическими
адресами. Общие импорты для этих примеров:

```kotlin
import dev.alllexey.itmoapi.bars.BarsClient
import dev.alllexey.itmoapi.bars.auth.BarsLogin
import dev.alllexey.itmoapi.bars.auth.BarsSessionCode
import dev.alllexey.itmoapi.bars.model.Discipline
import dev.alllexey.itmoapi.bars.model.Term
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.itmoid.CallbackUrl
import dev.alllexey.itmoapi.itmoid.TokenSet
import dev.alllexey.itmoapi.itmoid.TokenStorage
import dev.alllexey.itmoapi.myitmo.MyItmoClient
import dev.alllexey.itmoapi.myitmo.MyItmoConfiguration
import dev.alllexey.itmoapi.myitmo.schedule.Schedule
import io.ktor.client.engine.HttpClientEngine
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
```

`TokenStorage` атомарно читает и заменяет весь `TokenSet`: access token,
refresh token, ID token и два срока действия типа `kotlin.time.Instant`.
`null` означает отсутствие сессии. Этот минимальный пример хранит сессию
только в памяти и предназначен для одного владельца, не для постоянного хранения:

```kotlin
class MemoryTokens : TokenStorage {
    private var snapshot: TokenSet? = null
    override suspend fun read(): TokenSet? = snapshot
    override suspend fun write(tokens: TokenSet?) { snapshot = tokens }
    override fun toString(): String = "MemoryTokens(redacted)"
}
```

Для приложения реализуйте защищённое хранение самостоятельно (Keychain,
Keystore или серверное хранилище). После создания клиента единственный
писатель - `client.tokens` (`TokenManager`): вход через `replaceTokens`, выход
через `replaceTokens(null)`. Не запускайте рядом старый механизм обновления.
Для общего хранилища нескольких клиентов или процессов передайте общий
`TokenRefreshGuard`; локальный Mutex по умолчанию защищает только один менеджер.

```kotlin
fun createClient(
    storage: TokenStorage,
    engine: HttpClientEngine,
    clock: Clock,
    configuration: MyItmoConfiguration = MyItmoConfiguration.DEFAULT,
): MyItmoClient = MyItmoClient(configuration, storage, engine, clock)
```

## Вход через браузер

Создайте verifier и state через `Pkce.newVerifier()` и `Pkce.newState()`,
постройте URL через `client.identity.loginUrl(Pkce.challenge(verifier), state)`
и откройте в браузере. Сохраните verifier/state до callback. Проверяйте callback
перед обменом одноразового кода; не выводите эти значения в логи:

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

`ItmoIdClient.exchange` и `.refresh` возвращают полный снимок, сами не меняют
хранилище. `TokenManager.validAccessToken()` обновляет истекающий access token,
`forceRefresh()` принудительно обновляет его, `isRefreshTokenExpired()` проверяет
срок refresh token. Успешные конкурентные обновления объединяются; неудача
оставляет снимок нетронутым. Парольного входа и общего SSO-клиента в 2.x нет.

## MyITMO

Области клиента: `schedule`, `recordBook`, `personalities`, `studyplan`, `qr`,
`sport`, `election`, `finances`, `requests`, `system`. Методы возвращают
типизированные конверты; `requireResult()` проверяет код и извлекает результат:

```kotlin
suspend fun readSchedule(client: MyItmoClient): List<Schedule> =
    client.schedule.getPersonalSchedule(
        LocalDate(2026, 1, 5), LocalDate(2026, 1, 11),
    ).requireResult()
```

`client.sport` реализует `SportApi`, включая наследуемый `SportRemainingApi`:
личный календарь, медицинскую группу, долг, отборы и проекты. Фильтры спорта
сохраняют повторяющиеся query-параметры. Полный список перенесённых моделей
и членов находится в [руководстве миграции](docs/migration.md).

## БАРС

`BarsClient` принимает движок, `BarsConfiguration`, `BarsStorage` (по умолчанию
`RuntimeBarsStorage`, только память) и необязательный `BarsCodeSupplier`.
Конфигурация задаёт `restUrl`, `clientId`, `redirectUri` и общий ITMO.ID issuer.
Сессия БАРС - полный заголовок `Authorization`, не токен MyITMO; refresh token нет.
`BarsLogin` строит `loginUrl(state)`, проверяет `isCallback`/`isAllowedPage`
и извлекает код через `extractCode(callbackUrl, state)`. Передайте код в
`bars.login(code)`. `hasSession()` проверяет только наличие сохранённой сессии.

При HTTP 401 клиент может один раз получить код через `BarsCodeSupplier` и
повторить запрос. `null` от поставщика означает необходимость входа; сбой
сети или сервера - исключение, а не причина удаления сессии. Cookie вызывающий
читает и сохраняет сам. Для тихого входа без общего SSO:

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

Для `LOGIN_REQUIRED` показывайте вход, для `REJECTED` отклоняйте callback,
для `HTTP_ERROR` откладывайте повтор (`result.httpCode` содержит статус).
Транспортный сбой бросает `MyItmoException.Network`, не возвращает исход
`LOGIN_REQUIRED`. Все `Set-Cookie` возвращаются даже при неуспешном исходе;
их нельзя логировать. Переходы не выполняются, тело страницы не читается.

Каталоги и журналы используют период, сохранённый на сервере и общий с
веб-версией. `withPeriod` сериализует изменения только через этот экземпляр;
другой клиент всё ещё может поменять период. Не вкладывайте в блок другие
операции изменения периода:

```kotlin
suspend fun readBarsDisciplines(bars: BarsClient): List<Discipline> =
    bars.withPeriod("2026/2027", Term.AUTUMN) {
        getDisciplines(withCheckpointPlansOnly = true)
    }
```

Также доступны `getCurrentUser`, `getConfig`, `setPersonalSetting`,
`getGroupsAndFlows` и `getStudentJournal`. Идентификаторы БАРС не совпадают
с идентификаторами дисциплин/контрольных мероприятий MyITMO.

## Ошибки

`MyItmoException` имеет варианты `Network` (сетевая причина), `Http` (status),
`Api` (status, errorCode, необработанный serverMessage), `Auth` (status) и `Decode`.
Refresh HTTP 5xx - `Http`, не `Auth`. На `Http`/`Network`/`Decode` не удаляйте
сохранённую сессию. OAuth-отказ, отсутствующая или истёкшая refresh-сессия
классифицируются как `Auth`; решение о повторном входе принимает потребитель.
`CancellationException` пробрасывается без преобразования.

Диагностика редактирована; `Network.cause` и `Api.serverMessage` могут содержать
недоверенные данные и не предназначены для логов. Не логируйте коды, токены,
cookie, verifier, URL входа/callback и тела ответов. Локализация ошибок,
`DemoMode` и согласие на дополнительные сервисы принадлежат потребителю.

## Разработка

```bash
scripts/verify.sh kmp-jvm
```

Проверка использует JDK 17 и общий JVM build slot, запускает примеры и
паритетные тесты без обращений к университетским сервисам. Проверки iOS требуют
Xcode и отдельного разрешённого Native slot; инструкции - в [AGENTS.md](AGENTS.md).

## 1.x

Совместимая Java-библиотека остаётся в корне: `dev.alllexey:my-itmo-api:1.8.2`.
Для существующих потребителей смотрите [README версии 1.8.2](https://github.com/alllexey-dev/my-itmo-api/blob/1.8.2/README.md).
2.x не является бинарно совместимой заменой: используйте [руководство миграции](docs/migration.md).
