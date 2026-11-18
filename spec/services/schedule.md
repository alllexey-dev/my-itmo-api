# Расписание занятий

## Назначение

Личное расписание студента (и преподавателя): лекции, практики, лабораторные, экзамены, консультации,
физкультура и брони аудиторий на календарные дни. Раздел сайта my.itmo.ru "Расписание" и главный экран
Android-приложения. В приложении дополнительно есть расписание группы и преподавателя.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/schedule/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://api.schedule.itmo.su/api/v3/...` | Приложение отправляет `Authorization: Bearer` токена клиента `is-app`; `/teacher/{isu}` вживую принял и токен my.itmo.ru |
| Старые API приложения | `https://api.itmostudents.ru`, `https://schedule.itmo.su`, `https://itmostudents.ru` | Не выяснено |

Сайт и приложение, судя по совпадению путей `schedule/personal` и `teacher/{isu}`, ходят в один бэкенд расписания,
но эквивалентность ответов не сверялась. Своему клиенту достаточно прокси портала: личное расписание и расписание преподавателя
доступны на `my.itmo.ru` с токеном my.itmo.ru (оба проверены). См. [../hosts.md](../hosts.md),
[../auth.md](../auth.md).

Ответы расписания обёрнуты не так, как в остальных сервисах: `{code, message, data}`, где `code` 0 - успех
(см. [../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

Пометка "(GET снят)" означает, что ответ получен вживую при исследовании, но со схемой не сверялся.

### Сайт: `/api/schedule/*`

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/schedule/schedule/personal?date_start&date_end` | Личное расписание на включительный диапазон дат, по элементу на каждый день | проверено |
| GET | `/api/schedule/meta/time_slots` | Стандартные интервалы пар: `id`, `order`, `time_start`, `time_end` | проверено |
| GET | `/api/schedule/teacher/{isu}?date_start&date_end` | Расписание преподавателя по ISU, та же модель дня, что у личного; читать может любой вошедший пользователь. Неизвестный ISU - успех с днями без пар, нечисловой - `404 page not found`. Так его вызывает my-itmo-api (`getTeacherSchedule`) | проверено |
| GET | `/api/schedule/subjects` | Список предметов для фильтра; вызывается сайтом без параметров (GET снят: ответ 404 `page not found`) | сайт |

### Приложение: `https://api.schedule.itmo.su/api/v3`

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/schedule/personal?date_start&date_end` | Личное расписание; путь совпадает с сайтом | приложение |
| ? | `/schedule/{group}?date_start&date_end` | Расписание учебной группы | приложение |
| GET | `/teacher/{isu}?date_start&date_end` | Расписание преподавателя по ISU, та же модель дня, что у личного; вызвано вживую с токеном my.itmo.ru | проверено |
| ? | `/teacher/{teacherId}?date_start&date_end` | Расписание преподавателя из поиска | приложение |

`?` - HTTP-метод в разборе приложения не установлен.

### Старые API в приложении

| Метод | URL | Что делает | Статус |
|---|---|---|---|
| ? | `https://api.itmostudents.ru/schedule/personal/` | Расписание для виджета | приложение |
| ? | `https://schedule.itmo.su/api/v1/userinfo` | Профиль для старого расписания | приложение |
| ? | `https://itmostudents.ru/api/v1/public/edu/schedule/groups` | Список групп | приложение |
| ? | `https://itmostudents.ru/api/v1/public/edu/schedule/teachers` | Список преподавателей | приложение |
| ? | `https://itmostudents.ru/api/v1/public/edu/teachers/{teacherId}` | Карточка преподавателя | приложение |

Выборочное расписание кампании выбора дисциплин (`/api/election/students/schedule/...`) описано в
[elections.md](elections.md).

## Модели и правила

| Тема | Правило |
|---|---|
| Диапазон | `date_start`, `date_end` в формате `YYYY-MM-DD`, обе границы включительно; в ответе день есть даже без пар (`lessons: []`) |
| День | `date`, `day_number` (ISO, 1 - понедельник), `week_number` (учебная неделя), `note`, `type`, `lessons` |
| Время | `time_start`, `time_end` - строки `HH:mm` по Москве, без даты и зоны |
| Пара | `pair_id`, `subject`, `subject_id`, `type`, `work_type`, `teacher_id` (ISU), `teacher_name`, `room`, `building`, `bld_id`, `main_bld_id`, `group`, `flow_id`, `note` |
| `work_type_id` | 1 лекция, 2 лабораторная, 3 практика, 5 экзамен, 6 зачёт, 10 консультация, 11 физкультура |
| `format_id` | 1 очно, 2 смешанно, 3 дистанционно; текст в `format` |
| `flow_type_id` | 2 учебный поток, 3 спорт, 5 бронь аудитории |
| Онлайн | `zoom_url`, `zoom_password`, `zoom_info` - ссылка и пароль на онлайн-занятие, если есть |
| Ошибки | Свой конверт и у ошибок: `{code, message, data}`. Без дат или с датой не в формате `YYYY-MM-DD` - HTTP 404 и `code` 102; непринятый токен - HTTP 401 и `code` 93; `date_end` раньше `date_start` - успех с `code` 0 (всё снято 2026-11-18). В `my-itmo.yaml` ответ `default` описан общим `Error` (`{error_code, error_message, result}`), поэтому считайте ошибкой ненулевой код в любом из двух конвертов: так делает my-itmo-api (`core/ItmoTransport.kt`) |

Полная схема: `ScheduleDay`, `Lesson`, `ScheduleTimeSlot` в
[../openapi/my-itmo.yaml](../openapi/my-itmo.yaml).

## Спецификация

Личное расписание и интервалы пар - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `schedule`). Расписание
преподавателя на бэкенде приложения: `getTeacherSchedule` в
[../openapi/itmo-services.yaml](../openapi/itmo-services.yaml), пример ответа - в
[../openapi/examples/](../openapi/examples/). Полный синтетический ответ личного расписания - в фикстуре библиотеки
[../../fixtures/schedule/personal.json](../../fixtures/schedule/personal.json).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.schedule.getPersonalSchedule` | `GET /api/schedule/schedule/personal` |
| `itmo.schedule.getTeacherSchedule` | `GET /api/schedule/teacher/{isu}` |
| `itmo.schedule.getTimeSlots` | `GET /api/schedule/meta/time_slots` |

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_schedule` | `schedule/personal`; по умолчанию 7 дней с сегодняшнего (Москва), не больше 62 дней, пустые дни отбрасываются |

`meta/time_slots` в спецификации есть и входит в живую проверку itmo-mcp (`npm run verify:live`, см.
[../sources.md](../sources.md)), но инструментом не используется. Расписание группы и преподавателя не покрыто.

## Риски и открытые вопросы

- `/api/schedule/subjects` отвечает 404 на запрос без параметров; нужные параметры или актуальность ручки не
  выяснены.
- Расписание группы найдено только на хосте приложения: `/api/v3/schedule/{group}` на название потока и на номер
  группы отвечает 404, формат идентификатора группы не найден. Пути для него на прокси портала не найдено.
- Ответы `/api/schedule/teacher/{isu}` на прокси и `/api/v3/teacher/{isu}` на хосте приложения между собой не
  сверялись.
- Максимальная длина диапазона на сервере не известна; ограничение 62 дня - решение itmo-mcp.
- `zoom_password` - секрет занятия: при выводе пользователю это нормально, но в логи и общие чаты попадать не
  должно.
- Брони аудиторий (`flow_type_id` 5) и физкультура приходят вперемешку с учебными парами; фильтровать их по
  `flow_type_id`.
- Старые хосты `itmostudents.ru`, `schedule.itmo.su` скорее всего не обслуживаются; живьём не проверялись.
