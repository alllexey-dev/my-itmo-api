# БАРС

`https://bars.itmo.ru` - отдельный сервис с собственной сессией. Она живёт около 30 минут и не обновляется
токеном My ITMO: продлить её можно только новым входом через ITMO.ID. Библиотека делает это сама, когда у неё есть
cookie ITMO.ID.

## После входа по паролю

После `MyItmo.login` БАРС доступен сразу как `itmo.bars`: сессия получается и продлевается по cookie того же
входа, второй раз пароль не нужен.

После `MyItmo.fromRefreshToken`, `MyItmo.fromStorage` и после `MyItmo.loginOrResume`, который взял сохранённую
сессию, cookie нет. Тогда `itmo.bars` бросает `MyItmoException.Auth`, хотя остальные разделы работают: нужен вход
именно в БАРС.

Что делать:

- **Скрипту без браузера**: входите через `MyItmo.login` в каждом запуске, где нужен БАРС, и вызывайте
  `itmo.logout()` в конце, чтобы сессии ITMO.ID не копились.
- **Приложению с WebView**: войдите в БАРС сами, см. [вход без пароля](#вход-без-пароля).

## Учебный период

Каталоги и журналы читаются в контексте периода, выбранного на сервере. Эта настройка общая с веб-версией БАРС:
если её поменять, она поменяется и на сайте.

```kotlin
suspend fun readBarsDisciplines(bars: BarsClient): List<Discipline> =
    bars.withPeriod("2026/2027", Term.AUTUMN) {
        getDisciplines(withCheckpointPlansOnly = true)
    }
```

Вызов: `readBarsDisciplines(itmo.bars)`. `withPeriod` выбирает период и не даёт другим вызовам этого клиента
поменять его, пока блок не закончится. Внутри блока период менять нельзя.

Идентификаторы БАРС не совпадают с `discipline_id` и `est_id` My ITMO.

## Свои баллы

Журнал читается в три шага: дисциплины периода дают идентификаторы планов контрольных точек, группы и потоки
говорят, через какую группу читать каждый план, и по этой паре запрашивается журнал.

```kotlin
suspend fun readJournals(bars: BarsClient): List<StudentJournal> {
    val groups = bars.getGroupsAndFlows()
    return bars.getDisciplines(withCheckpointPlansOnly = true)
        .flatMap { it.checkpointPlanIds }.distinct()
        .mapNotNull { plan ->
            val group = groups.firstOrNull { plan in it.checkpointPlanIds } ?: return@mapNotNull null
            bars.getStudentJournal(plan, group.type, group.identifier)
        }
}
```

Чтобы читать нужный семестр, вызывайте внутри `withPeriod`:
`itmo.bars.withPeriod("2026/2027", Term.AUTUMN) { readJournals(this) }`. В `StudentJournal` заголовки контрольных точек лежат
в `headers`, а строка с вашими баллами - в `students`. Модели - в пакете `dev.alllexey.itmoapi.bars.model`.

Все методы перечислены в [справочнике](api.md#барс-itmobars).

## Вход без пароля

Вход в БАРС - это код ITMO.ID, обменянный на сессию. Код выдаёт `BarsLogin`
(`dev.alllexey.itmoapi.bars.auth`), сессию создаёт `BarsClient`: это может быть `itmo.bars` или отдельный
`BarsClient(engine)`.

1. Создайте `val login = BarsLogin(defaultEngine())` и `val state = login.newState()`.
2. Откройте `login.loginUrl(state)` в браузере или WebView.
3. Достаньте код из адреса, на который вернул ITMO.ID: `login.extractCode(callbackUrl, state)`.
4. Вызовите `bars.login(code)`.

Чтобы сессия продлевалась сама, передайте в `BarsClient` свой `BarsCodeSupplier`: при HTTP 401 клиент один раз
запросит у него новый код и повторит запрос. Если сессия ITMO.ID живёт в cookie WebView, код можно получить без
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

ITMO.ID при каждом таком запросе выдаёт новые cookie: сохраняйте их (`saveCookies`) и в следующий раз
отправляйте уже новые.

Как это выглядит на уровне HTTP - в [spec/auth.md](../spec/auth.md).
