<h1 align="center">MyItmoApi</h1>

<p align="center">
  <strong>Неофициальная Kotlin Multiplatform библиотека для работы с <a href="https://my.itmo.ru">My ITMO</a> и <a href="https://bars.itmo.ru">БАРС</a></strong>
  <br />
  Расписание, зачётка, спорт, QR-пропуск и БАРС из Kotlin, Java, Android и iOS
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/dev.alllexey/my-itmo-api"><img src="https://img.shields.io/maven-central/v/dev.alllexey/my-itmo-api?style=flat-square&color=blue" alt="Maven Central" /></a>
  <a href="https://github.com/alllexey-dev/my-itmo-api/actions/workflows/ci.yml"><img src="https://img.shields.io/github/actions/workflow/status/alllexey-dev/my-itmo-api/ci.yml?branch=master&style=flat-square&label=CI" alt="CI" /></a>
  <img src="https://img.shields.io/badge/JVM%20%C2%B7%20Android%20%C2%B7%20iOS-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="JVM, Android, iOS" />
  <a href="LICENSE"><img src="https://img.shields.io/github/license/alllexey-dev/my-itmo-api?style=flat-square" alt="Лицензия" /></a>
</p>

<p align="center">
  <a href="#быстрый-старт"><strong>Быстрый старт</strong></a> ·
  <a href="#что-умеет">Что умеет</a> ·
  <a href="#документация">Документация</a> ·
  <a href="spec/README.md">Описание API ИТМО</a> ·
  <a href="CHANGELOG.md">История изменений</a>
</p>

В репозитории две вещи:

- **библиотека** для Kotlin и Java: вход через ITMO.ID, автоматическое обновление токенов и типизированные
  методы My ITMO и БАРС;
- **[описание API ИТМО](spec/README.md)**, не привязанное к языку: как устроен вход, какие есть ручки, спецификации
  OpenAPI и примеры ответов. По нему можно написать свой клиент на Python, Go, TypeScript или чём угодно.

## Быстрый старт

### 1. Зависимость

Gradle:

```kotlin
implementation("dev.alllexey:my-itmo-api:2.5.0")
```

Maven:

```xml
<dependency>
    <groupId>dev.alllexey</groupId>
    <artifactId>my-itmo-api</artifactId>
    <version>2.5.0</version>
</dependency>
```

Имя одно для всех проектов: Kotlin Multiplatform, Android, Kotlin и Java на JVM.

### 2. Вход и первый запрос

Kotlin: расписание на неделю вперёд.

```kotlin
import dev.alllexey.itmoapi.MyItmo
import dev.alllexey.itmoapi.core.MOSCOW
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

suspend fun main() {
    MyItmo.login("my_cool_id", "my_strong_password").use { itmo ->
        val today = Clock.System.todayIn(MOSCOW)
        val week = itmo.schedule.getPersonalSchedule(today, today.plus(6, DateTimeUnit.DAY))
        for (day in week) {
            println("${day.date}: " + day.lessons.joinToString { "${it.timeStart} ${it.subject}" })
        }
    }
}
```

На Kotlin 2.2 этому примеру нужен opt-in для `kotlin.time.Clock`, см. [требования](#требования).

Java: то же самое через `BlockingMyItmo`, без корутин.

```java
import dev.alllexey.itmoapi.blocking.BlockingMyItmo;
import dev.alllexey.itmoapi.myitmo.schedule.Lesson;
import dev.alllexey.itmoapi.myitmo.schedule.Schedule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (BlockingMyItmo itmo = BlockingMyItmo.login("my_cool_id", "my_strong_password")) {
            LocalDate today = LocalDate.now(ZoneId.of("Europe/Moscow"));
            List<Schedule> week = itmo.getSchedule().getPersonalSchedule(today, today.plusDays(6));
            for (Schedule day : week) {
                System.out.println(day.getDate() + ":");
                for (Lesson lesson : day.getLessons()) {
                    System.out.println("  " + lesson.getTimeStart() + " " + lesson.getSubject());
                }
            }
        }
    }
}
```

Что важно знать сразу:

- Чтобы не входить заново при каждом запуске, замените `login` на
  `MyItmo.loginOrResume(логин, пароль, FileTokenStorage(...))`: токены сохранятся в файл. Остальные способы входа,
  хранение токенов и выход - в [docs/auth.md](docs/auth.md).
- Все методы в Kotlin - `suspend`. Метод возвращает данные или бросает `MyItmoException`: виды ошибок и что с
  ними делать - в [docs/errors.md](docs/errors.md).
- `close()` (или `use`) освобождает HTTP-клиент.

## Что умеет

| Раздел | Свойство | Что внутри |
|---|---|---|
| Расписание | `itmo.schedule` | Личное расписание, расписание преподавателя, временные слоты пар |
| Зачётка | `itmo.recordBook` | Специализации, оценки по семестрам, дерево контрольных мероприятий |
| Учебный план | `itmo.studyPlan` | Программы и полный план с дисциплинами |
| Люди | `itmo.personalities` | Профиль по номеру ИСУ и поиск |
| Спорт | `itmo.sport` | Расписание и фильтры, запись и отмена, личный календарь, баллы, попытки, задолженность, отборы |
| Выбор дисциплин | `itmo.election` | Состояние кампании, доступные дисциплины и потоки, сохранение выбора |
| Финансы | `itmo.finances` | Суммы выплат, оплата обучения: договоры, баланс, график платежей |
| Общежитие | `itmo.dormitory` | Этап и место в очереди, назначенное общежитие, документы, договоры найма. Только чтение |
| Заявки | `itmo.requests` | Список заявок пользователя |
| Портал | `itmo.system` | Главный экран, меню и каталог сервисов |
| QR-пропуск | `itmo.qr` | Пропуск в корпуса в HEX-формате |
| БАРС | `itmo.bars` | Дисциплины, потоки и журнал с баллами за выбранный учебный период |

Кроме этого: вход в браузере через ITMO.ID (OAuth с PKCE), выход из сессии, список и завершение сессий аккаунта.
Все методы с запросами, которые они отправляют, перечислены в [docs/api.md](docs/api.md).

## Документация

| Документ | О чём |
|---|---|
| [Вход и сессия](docs/auth.md) | Способы входа, хранение токенов, что делать с истёкшей сессией, выход, вход в браузере |
| [Справочник методов](docs/api.md) | Все разделы и методы: что возвращают и какой запрос отправляют |
| [Ошибки](docs/errors.md) | `MyItmoException` и его виды, что делать с каждым |
| [Запись на спорт](docs/sport.md) | Как найти занятие, записаться, разобрать итог и отменить запись |
| [БАРС](docs/bars.md) | Сессия БАРС, учебный период, свои баллы, вход без пароля |
| [Java](docs/java.md) | `BlockingMyItmo`, хранение сессии, ошибки, даты, вызов остальных `suspend`-методов |
| [Android, iOS и тонкая настройка](docs/advanced.md) | Пример для Android, свой HTTP-движок, общий замок обновления токенов, время |

## Свой клиент на другом языке

Библиотека не обязательна. В [spec/](spec/README.md) лежит всё, что нужно для собственной реализации:

- [вход и токены](spec/auth.md): ITMO.ID, PKCE, обновление токена, сессия БАРС, с запросами по шагам;
- [общие правила ответов](spec/conventions.md): конверты ответов, коды ошибок, даты;
- [документы по сервисам](spec/services/) и [спецификации OpenAPI](spec/openapi/) с примерами ответов.

## Требования

- JVM 11 или новее, Android с minSdk 26, iOS (`iosArm64`, `iosSimulatorArm64`).
- Kotlin 2.2 или новее; из Java - через `BlockingMyItmo`.
- На Kotlin 2.2 `kotlin.time.Clock` из примеров ещё экспериментальный: добавьте в сборку опцию компилятора
  `-opt-in=kotlin.time.ExperimentalTime` или `@OptIn(ExperimentalTime::class)` у функции.

Основные зависимости: Ktor, kotlinx.serialization, kotlinx-datetime и kotlinx.coroutines.

## Стабильность

My ITMO не публикует документацию своих сервисов. Модели основаны на наблюдаемых ответах, поэтому сервер может
добавлять поля и справочные значения без предупреждения: неизвестные поля библиотека пропускает. Поля, которые
в ответах есть, но тип которых пока не ясен, отмечены в моделях закомментированными объявлениями.

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
./gradlew jvmTest                 # JDK 17
./gradlew iosSimulatorArm64Test   # нужен Xcode
```

Как добавить ручку и фикстуру и как устроены релизы - в [CONTRIBUTING.md](CONTRIBUTING.md).

## Обратная связь

Ошибки и идеи: [Issues](https://github.com/alllexey-dev/my-itmo-api/issues) или
[@itmowidgets](https://t.me/itmowidgets).

## Лицензия

[MIT](LICENSE). Неофициальный проект, не связан с Университетом ИТМО.
