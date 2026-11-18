# Android, iOS и тонкая настройка

Быстрый старт - в [README](../README.md), вход и хранение токенов - в [auth.md](auth.md).

## Android

Все методы - `suspend` и не блокируют главный поток. Нужно разрешение `android.permission.INTERNET`. Пароль
лучше не спрашивать самому: пусть пользователь входит на странице ITMO.ID
([вход в браузере](auth.md#вход-в-браузере)), а токены хранятся в своём `TokenStorage` поверх Keystore.

```kotlin
import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MyItmoException
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
            show(itmo.schedule.getPersonalSchedule(monday, monday.plus(6, DateTimeUnit.DAY)))
        } catch (error: MyItmoException) {
            fail(error) // MyItmoException.Auth - нужен повторный вход
        }
    }
}
```

## iOS

Цели `iosArm64` и `iosSimulatorArm64` подключаются из общего Kotlin-модуля (`commonMain` или `iosMain`) той же
зависимостью `dev.alllexey:my-itmo-api`. HTTP-движок по умолчанию - Darwin. `FileTokenStorage` есть только на
JVM: на iOS храните токены в Keychain через свой `TokenStorage`.

## MyItmo и MyItmoClient

`MyItmo` (`dev.alllexey.itmoapi`) - короткий путь: сам создаёт HTTP-движок, умеет вход по паролю и подключает БАРС. Под ним лежит
`MyItmoClient` (`dev.alllexey.itmoapi.myitmo`, доступен как `itmo.client`), которому всё передаётся явно: конфигурация, хранилище, движок, часы.
Собирайте `MyItmoClient` сами, если нужны вход в браузере, свой движок или общий замок обновления токенов.
Разделы у обоих одни и те же.

`MyItmoConfiguration.DEFAULT` - боевой My ITMO, `MyItmoConfiguration.DEV` - тестовое окружение ИТМО. Язык
ответов задаёт `acceptLanguage` (по умолчанию `ru`).

## Свой HTTP-движок

`defaultEngine()` создаёт OkHttp на JVM и Android и Darwin на iOS. Свой движок Ktor передаётся в конструктор
`MyItmoClient`. Он принадлежит вызывающему: закройте сначала клиент (`client.close()`), потом движок.

## Общий замок обновления токенов

ITMO.ID меняет refresh token при каждом обновлении, и старый перестаёт работать. Если одно хранилище делят
несколько клиентов или процессов (приложение и виджет, расширение iOS), передайте им всем один
`TokenRefreshGuard` (параметр `refreshGuard` у `MyItmoClient`): замок, общий для всех процессов, например
файловый в App Group.

Без общего замка ITMO.ID может отклонить второе одновременное обновление. `TokenManager` в таком случае
перечитывает хранилище и берёт сессию, которую сохранил сосед, но это страховка, а не замена замку.

## Время (Москва)

Время в ответах (`Instant`) абсолютное. Показывайте его в часовом поясе ИТМО `MOSCOW`
(`dev.alllexey.itmoapi.core.MOSCOW`, `Europe/Moscow`), а не в поясе устройства:
`instant.toLocalDateTime(MOSCOW)`. "Сегодня" по ИТМО - `Clock.System.todayIn(MOSCOW)`. Строки `HH:mm` в
расписании и спорте уже заданы по Москве. Даты - `kotlinx.datetime.LocalDate`.
