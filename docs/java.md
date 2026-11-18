# Java

Методы библиотеки - `suspend`-функции Kotlin. Для Java есть `BlockingMyItmo`
(`dev.alllexey.itmoapi.blocking`): те же разделы и методы, но каждый вызов ждёт результата в текущем потоке и
возвращает его или бросает `MyItmoException`.

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

## Что есть у BlockingMyItmo

| Kotlin | Java |
|---|---|
| `MyItmo.login(u, p)`, `loginOrResume(u, p, s)`, `fromRefreshToken(t)`, `fromStorage(s)` | `BlockingMyItmo.login(u, p)`, `loginOrResume(u, p, s)`, `fromRefreshToken(t)`, `fromStorage(s)` |
| `itmo.schedule`, `itmo.sport`, `itmo.bars` и остальные разделы | `itmo.getSchedule()`, `itmo.getSport()`, `itmo.getBars()` |
| `itmo.hasSession()`, `itmo.logout()` | Так же |
| `itmo.close()` или `use` | `close()` или try-with-resources |
| Уже созданный `MyItmo` | `BlockingMyItmo.of(myItmo)`; обратно - `itmo.getItmo()` |

Имена методов в разделах такие же, как в [справочнике](api.md). Параметры со значением по умолчанию можно не
передавать: `itmo.getSport().getSportScore()`.

Не вызывайте `BlockingMyItmo` из главного потока Android: он блокирует поток на время запроса.

## Даты

Параметры-даты у `BlockingMyItmo` - `java.time.LocalDate`. В моделях даты остаются
`kotlinx.datetime.LocalDate`; в `java.time` они переводятся так:

```java
static LocalDate dateOf(Schedule day) {
    return ConvertersKt.toJavaLocalDate(day.getDate());
}
```

`ConvertersKt` - это `kotlinx.datetime.ConvertersKt`.

## Хранение сессии

Чтобы не входить по паролю при каждом запуске, храните токены в файле. `loginOrResume` берёт сохранённую сессию,
а если её нет или она истекла, входит заново:

```java
static BlockingMyItmo openSession(String username, String password) throws MyItmoException {
    return BlockingMyItmo.loginOrResume(username, password, new FileTokenStorage(Path.of("itmo-tokens.json")));
}
```

`FileTokenStorage` - в пакете `dev.alllexey.itmoapi.itmoid`. Что делать, когда сохранённая сессия перестала
работать, и чем отличаются способы входа - в [auth.md](auth.md). Свой `TokenStorage` на Java писать неудобно,
потому что его методы - `suspend`: напишите его на Kotlin или возьмите файловый.

## Ошибки

`MyItmoException` (`dev.alllexey.itmoapi.core`) для Java - проверяемое исключение: его нужно поймать или объявить
в `throws`. Виды ошибок те же, что в [errors.md](errors.md), и ловятся как вложенные классы:

```java
static List<Schedule> weekOrNull(BlockingMyItmo itmo, LocalDate monday) {
    try {
        return itmo.getSchedule().getPersonalSchedule(monday, monday.plusDays(6));
    } catch (MyItmoException.Auth e) {
        return null; // сессия закончилась: нужен новый вход
    } catch (MyItmoException e) {
        return null; // Api, Http, Network, Decode
    }
}
```

Запись на спорт возвращает не исключение, а `SportSignResult`; пример разбора на Java - в
[sport.md](sport.md#2-записаться-и-разобрать-итог).

## Остальные suspend-методы

Всё, чего нет у `BlockingMyItmo` (например, `MyItmoClient`, `ItmoIdSessions`, `BarsLogin`), вызывается через
`Blocking.call` (класс `dev.alllexey.itmoapi.Blocking`): последним аргументом передаётся `c`, а результат присваивается переменной с типом.

```java
static String refreshNow(BlockingMyItmo itmo) throws Exception {
    return Blocking.call(c -> itmo.getItmo().getClient().getTokens().forceRefresh(c));
}
```
