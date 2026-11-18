# Запись на спорт

Путь целиком: найти занятие, записаться, разобрать итог, при необходимости отменить запись. Модели лежат в
пакете `dev.alllexey.itmoapi.myitmo.sport`.

## 1. Найти занятие

`sport.getSportSchedule(from, to)` возвращает дни, в каждом - список занятий (`lessons`, может отсутствовать).
Идентификатор занятия для записи - `SportLesson.id`.

```kotlin
suspend fun freeLessons(itmo: MyItmo, from: LocalDate, to: LocalDate): List<SportLesson> =
    itmo.sport.getSportSchedule(from, to)
        .flatMap { it.lessons.orEmpty() }
        .filter { it.canSignIn.canSignIn && it.available > 0 && !it.signed }
```

Поля, на которые стоит смотреть:

| Поле `SportLesson` | Что значит |
|---|---|
| `id` | Идентификатор занятия: его принимают `signInLesson` и `signOutLessons` |
| `sectionName`, `roomName`, `teacherFio` | Секция, зал и преподаватель |
| `date`, `timeSlotStart`, `timeSlotEnd` | Начало занятия и интервал `HH:mm` по Москве |
| `limit`, `available` | Всего мест и сколько свободно |
| `canSignIn.canSignIn` | Разрешает ли сервер запись; причины отказа - в `canSignIn.unavailableReasons` |
| `signed` | Вы уже записаны |
| `intersection` | Занятие пересекается с другим в вашем расписании |

В Java `getLessons()` у дня без занятий возвращает `null`: проверяйте его перед обходом. Примеры шагов 1 и 3
переносятся на Java напрямую через `itmo.getSport()`.

Расписание можно сузить параметрами `buildingId`, `sportTypeIds` и `teacherIsu`; их значения отдаёт
`sport.getSportFilters()`.

## 2. Записаться и разобрать итог

`sport.signInLesson(id)` записывает на одно занятие и вместо исключения возвращает `SportSignResult`:

```kotlin
suspend fun book(itmo: MyItmo, lessonId: Long): String = when (val result = itmo.sport.signInLesson(lessonId)) {
    SportSignResult.Booked -> "записан"
    SportSignResult.Full -> "мест нет, можно попробовать позже"
    is SportSignResult.Rejected -> "отказ: код ${result.refusal.errorCode}"
    is SportSignResult.RetryLater -> "решения нет, повторить позже"
}
```

- `Booked` - записали.
- `Full` - нет свободных мест; место может освободиться.
- `Rejected` - My ITMO отказал по другому правилу (лимит на день или неделю, пересечение и так далее), повтор не
  поможет. Причина в `refusal.serverMessage`; в логи её писать не стоит.
- `RetryLater` - решения по занятию нет (сеть, HTTP-статус, сессия). Причина в `cause`; если это
  `MyItmoException.Auth`, нужен новый вход.

То же из Java:

```java
static String book(BlockingMyItmo itmo, long lessonId) {
    SportSignResult result = itmo.getSport().signInLesson(lessonId);
    if (result instanceof SportSignResult.Booked) return "записан";
    if (result instanceof SportSignResult.Full) return "мест нет, можно попробовать позже";
    if (result instanceof SportSignResult.Rejected) {
        return "отказ: код " + ((SportSignResult.Rejected) result).getRefusal().getErrorCode();
    }
    return "решения нет, повторить позже";
}
```

`sport.signInLessons(ids)` записывает сразу на несколько занятий, возвращает идентификаторы тех, на которые
записали, и бросает `MyItmoException`, как остальные методы.

## 3. Отменить запись

```kotlin
suspend fun cancel(itmo: MyItmo, lessonId: Long): Boolean =
    lessonId in itmo.sport.signOutLessons(listOf(lessonId))
```

`signOutLessons` возвращает идентификаторы занятий, с которых запись снята. Если вы не были записаны, сервер
отвечает ошибкой: это `MyItmoException.Api` с кодом 130.

## Что ещё есть

Свои записи - `sport.getPersonalSportCalendar(from, to)`, баллы - `sport.getSportScore()`, оставшиеся попытки
записи - `sport.getSportAttempts()`. Все методы раздела - в [справочнике](api.md#спорт-itmosport).
