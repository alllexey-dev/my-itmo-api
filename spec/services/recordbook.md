# Электронная зачётная книжка

## Назначение

Итоговые результаты студента по дисциплинам за каждый семестр (баллы, оценка, форма контроля, дата) и
разбивка баллов по контрольным мероприятиям. Раздел сайта my.itmo.ru "Зачётка" и экран "Баллы" Android-приложения.
Текущий журнал баллов преподавателей ведётся в БАРС (отдельный документ [bars.md](bars.md)); зачётка показывает
то, что попало в ведомости и ИСУ.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/record_book/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://recordbook.itmo.su/api/record_book/...` | Приложение отправляет `Authorization: Bearer` токена клиента `is-app`; `history` вживую приняла и токен my.itmo.ru |

Пути после хоста совпадают, вероятно это один бэкенд за прокси сайта. См. [../hosts.md](../hosts.md),
[../auth.md](../auth.md). Ответы обёрнуты в `{error_code, error_message, result}`
([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/record_book/specializations` | Образовательные программы студента с перечнем семестров | проверено |
| GET | `/api/record_book/{specializationId}/{semester}` | Итоги по дисциплинам одного семестра; `specializationId` = `main_plan` | проверено |
| GET | `/api/record_book/{entryId}` | Дерево контрольных мероприятий одной записи; `entryId` = `est_id` | проверено |
| GET | `https://recordbook.itmo.su/api/record_book/history?offset&count` | Лента изменений баллов с пагинацией: `discipline_name`, `name`, `date`, `old_value`, `new_value`, `teacher` | проверено |

Остальные ручки приложения (`specializations`, `{specializationId}/{semester}`, `{entryId}`) совпадают с сайтом по
пути (статус приложения - приложение, сайта - проверено).

## Модели и правила

| Тема | Правило |
|---|---|
| Программа | `main_plan`, `specialization_name`, `semesters[]` |
| Семестр | `semester` - сквозной номер в плане с 1; `study_year` (`2025/2026`), `course`, `sem_id`, `actual` - текущий период |
| Запись | `name`, `discipline_id`, `est_id`, `current_score`, `rate` (например `5/A`, `Зачёт`), `attempt`, `control_type`, `control_type_id`, `exam_date`, `have_tree`, `lms_link`, `teacher {surname, name, patronymic}` |
| Разбивка | Плоский список `ControlEntry`, связанный через `parent_id`: `control_name`, `lower_value`, `min_value`, `max_value`, `required`, `rate` (null, если не оценено), `date`, `teacher` |
| `have_tree` | Если `false`, разбивка, скорее всего, пустая; запрашивать `{entryId}` имеет смысл только при `true` |
| Неоднозначность пути | `/{specializationId}/{semester}` и `/{entryId}` различаются только числом сегментов |

Полные схемы: `Specialization`, `RecordBookEntry`, `ControlEntry` в
[../openapi/my-itmo.yaml](../openapi/my-itmo.yaml).

## Спецификация

Ручки сайта - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `recordbook`). История изменений зачётки на
бэкенде приложения: `getRecordBookHistory` в [../openapi/itmo-services.yaml](../openapi/itmo-services.yaml), пример
ответа - в [../openapi/examples/](../openapi/examples/). Синтетические ответы ручек сайта - в
[../../fixtures/recordbook/](../../fixtures/recordbook/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.recordBook.getSpecializations` | `GET /api/record_book/specializations` |
| `itmo.recordBook.getRecordBook` | `GET /api/record_book/{specializationId}/{semester}` |
| `itmo.recordBook.getControlEntries` | `GET /api/record_book/{entryId}` |

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_grades` | `specializations`, затем `{specializationId}/{semester}`; по умолчанию текущий семестр (`actual`) |
| `itmo_get_grade_details` | `{entryId}`; разбивка баллов одной записи |

Лента изменений `history` из приложения не покрыта.

## Риски и открытые вопросы

- `history?offset&count` на `recordbook.itmo.su` принимает токен my.itmo.ru (проверено, модель `getRecordBookHistory` в
  [../openapi/itmo-services.yaml](../openapi/itmo-services.yaml)); есть ли она за прокси сайта
  (`/api/record_book/history`), не проверялось. На сайте путь `/api/record_book/{entryId}`
  с `history` вместо числа может конфликтовать с маршрутом разбивки.
- Поведение для студентов с несколькими программами (`specializations` из нескольких элементов) живьём не
  проверено: инструмент itmo-mcp `itmo_get_grades` берёт программу с текущим семестром.
- Смысл `attempt` (номер пересдачи) и полный набор значений `control_type_id` не задокументированы.
- Задержка между БАРС и зачёткой не известна: свежие баллы могут быть в БАРС раньше.
