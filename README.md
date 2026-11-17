<h1 align="center">MyItmoApi</h1>

<p align="center">
  <strong>Неофициальная Kotlin Multiplatform библиотека для работы с <a href="https://my.itmo.ru">My ITMO</a> и <a href="https://bars.itmo.ru">БАРС</a></strong>
  <br />
  Расписание, зачётка, спорт, QR-пропуск и БАРС из Kotlin, Java, Android и iOS
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/dev.alllexey/my-itmo-api-kmp"><img src="https://img.shields.io/maven-central/v/dev.alllexey/my-itmo-api-kmp?style=flat-square&color=blue" alt="Maven Central" /></a>
  <a href="https://github.com/alllexey-dev/my-itmo-api/actions/workflows/kmp-ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/alllexey-dev/my-itmo-api/kmp-ci.yml?branch=master&style=flat-square&label=CI" alt="CI" /></a>
  <img src="https://img.shields.io/badge/JVM%20%C2%B7%20Android%20%C2%B7%20iOS-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="JVM, Android, iOS" />
  <a href="LICENSE"><img src="https://img.shields.io/github/license/alllexey-dev/my-itmo-api?style=flat-square" alt="Лицензия" /></a>
</p>

<p align="center">
  <a href="#быстрый-старт"><strong>Быстрый старт</strong></a> ·
  <a href="#что-умеет">Что умеет</a> ·
  <a href="docs/advanced.md">Продвинутая настройка</a> ·
  <a href="#переход-с-1x">Переход с 1.x</a> ·
  <a href="CHANGELOG.md">История изменений</a>
</p>

## Быстрый старт

Два шага: зависимость, затем клиент и вызов.

### 1. Зависимость

Gradle (Kotlin, Android):

```kotlin
implementation("dev.alllexey:my-itmo-api-kmp:2.4.0")
```

Maven (Java):

```xml
<dependency>
    <groupId>dev.alllexey</groupId>
    <artifactId>my-itmo-api-kmp-jvm</artifactId>
    <version>2.4.0</version>
</dependency>
```

### 2. Клиент и вызов

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
`election`, `finances`, `dormitory`, `requests`, `system` и `bars`. Методы My ITMO
возвращают ответ-обёртку; `requireResult()` достаёт результат или бросает
ошибку. `close()` (или `use`) освобождает HTTP-клиент.

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
пусть пользователь входит на странице ITMO.ID (см. [вход в браузере](docs/advanced.md#вход-в-браузере)),
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

## Что умеет

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
- Главный экран, меню и каталог сервисов My ITMO.
- Заявки пользователя.
- Суммарные выплаты по категориям.
- Оплата обучения: договоры, баланс и график платежей.
- Общежитие: этап и место в очереди, назначенное общежитие с инструкцией по
  заселению, документы для заселения, договоры найма с балансом и графиком
  платежей. Только чтение.
- QR-пропуск в корпуса в HEX-формате.
- БАРС: вход через ITMO.ID, выбор учебного периода, каталоги дисциплин и потоков,
  журнал с баллами и подтверждёнными оценками.

## Требования

- JVM 11 или новее, Android с minSdk 26, iOS (`iosArm64`, `iosSimulatorArm64`).
- Kotlin 2.2 или новее; из Java - через `Blocking`.

Основные зависимости: Ktor, kotlinx.serialization, kotlinx-datetime и kotlinx.coroutines.

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
Keychain (пример - в [продвинутой настройке](docs/advanced.md#клиент)).

Выход: `itmo.client.tokens.replaceTokens(null)`.

## Ошибки

Все методы бросают `MyItmoException`:

- `Auth` - нужен вход: неверный логин или пароль, ITMO.ID попросил
  дополнительный шаг (например, 2FA), сессия закончилась (HTTP 401, в My ITMO -
  после одной попытки обновить токен), её нет в хранилище или ITMO.ID отклонил
  refresh token;
- `Http` - неуспешный HTTP-статус, в том числе 403: сессия действительна, но
  доступа к ресурсу нет;
- `Network`, `Decode` - временный сбой;
- `Api` - My ITMO вернул код ошибки.

При `Network`, `Http` и `Decode` сохранённая сессия не удаляется. В сообщениях
исключений нет токенов, паролей и ответов сервера; сами тоже не пишите их в логи.

## Спорт

`sport.signInLesson(id)` записывает на одно занятие и не бросает `MyItmoException`,
а возвращает `SportSignResult`:

- `Booked` - записали;
- `Full` - нет свободных мест, можно попробовать позже;
- `Rejected` - My ITMO отказал по другому правилу (лимит, пересечение и т.п.),
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
вы не войдёте в БАРС сами ([БАРС без пароля](docs/advanced.md#барс-без-пароля)).

Каталоги и журналы читаются в контексте периода, сохранённого на сервере; эта
настройка общая с веб-версией БАРС.

```kotlin
suspend fun readBarsDisciplines(bars: BarsClient): List<Discipline> =
    bars.withPeriod("2026/2027", Term.AUTUMN) {
        getDisciplines(withCheckpointPlansOnly = true)
    }
```

Вызов: `readBarsDisciplines(itmo.bars)`. Идентификаторы БАРС не совпадают с
`discipline_id` и `est_id` My ITMO.

## Продвинутая настройка

`MyItmo` - тонкая обёртка над `MyItmoClient` (`itmo.client`). Вход в браузере через ITMO.ID, своё
хранилище токенов, свой HTTP-движок, общий замок обновления токенов, БАРС без пароля и управление
сессиями ITMO.ID описаны в [docs/advanced.md](docs/advanced.md).

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

## Стабильность

My ITMO не предоставляет публичную документацию для всех используемых сервисов.
Модели основаны на наблюдаемых ответах API, поэтому сервер может добавлять новые
поля и справочные значения. Неизвестные, но подтверждённо присутствующие поля
отмечены в моделях закомментированными объявлениями до уточнения их типов.

## Экосистема

На этой библиотеке работает [ITMO.Widgets](https://github.com/alllexey-dev/ITMO.Widgets): все данные
My ITMO и БАРС приложение получает через неё.

| Репозиторий | Что это |
|---|---|
| [ITMO.Widgets](https://github.com/alllexey-dev/ITMO.Widgets) | Приложение для Android и iPhone |
| [itmo-widgets-backend](https://github.com/alllexey-dev/itmo-widgets-backend) | Сервер: друзья, приватность, очереди на спорт, отзывы, уведомления |
| [itmo-widgets-web](https://github.com/alllexey-dev/itmo-widgets-web) | Сайт и веб-версия |
| [itmo-widgets-agent](https://github.com/alllexey-dev/itmo-widgets-agent) | Агент для компьютера или сервера: автозапись на спорт без телефона |
| [itmo-mcp](https://github.com/alllexey-dev/itmo-mcp) | MCP-сервер ИТМО для Claude, Codex, Cursor и других ассистентов |
| **my-itmo-api** (этот репозиторий) | Библиотека для My ITMO и БАРС: Kotlin Multiplatform и Java |

## Разработчикам

```bash
./gradlew -p kmp jvmTest                 # 2.x, JDK 17
./gradlew -p kmp iosSimulatorArm64Test   # 2.x, нужен Xcode
./mvnw verify -Dgpg.skip=true            # 1.x
```

Как добавить эндпоинт, фикстуру и как устроены релизы - в [CONTRIBUTING.md](CONTRIBUTING.md).

## Обратная связь

Ошибки и идеи: [Issues](https://github.com/alllexey-dev/my-itmo-api/issues) или
[@itmowidgets](https://t.me/itmowidgets).

## Лицензия

[MIT](LICENSE). Неофициальный проект, не связан с Университетом ИТМО.
