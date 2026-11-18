# Электронная очередь

## Назначение

Запись на приём в подразделения университета (деканат, студенческий офис, приёмная комиссия и другие) на
конкретное время. Студент выбирает очередь, слот, оставляет телефон и комментарий; для части очередей сначала
подаётся заявка. Раздел сайта my.itmo.ru "Мои заявки и очереди" и экран очередей Android-приложения.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/queues/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://queue.itmo.pro/api/v1/queue...` | `Authorization: Bearer` токена ITMO.ID клиента `is-app` |

Сайт `/api/queues/X` соответствует `/api/v1/queue/X` приложения. См. [../hosts.md](../hosts.md),
[../auth.md](../auth.md). Ответы обёрнуты в `{error_code, error_message, result}`
([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/queues/current` | Свои предстоящие записи | проверено |
| GET | `/api/queues/archive` | Свои прошедшие записи | проверено |
| GET | `/api/queues/` | Доступные очереди | проверено |
| GET | `/api/queues/?table_id&type_id` | Одна очередь; сайт берёт `result[0]`, в том числе `template_id` | проверено |
| GET | `/api/queues/slots?table_id&type_id` | Свободные слоты `[{time_table_id, date}]` | проверено |
| POST | `/api/queues/` | Записаться: `{time_table_id, phone, comment, request_id}` | сайт |
| DELETE | `/api/queues/` | Отменить запись: тело `{type_id, application_id}` | сайт |
| GET, POST, DELETE | `https://queue.itmo.pro/api/v1/queue` | Список очередей, запись, отмена в приложении | приложение |
| GET | `https://queue.itmo.pro/api/v1/queue/current` | Предстоящие записи в приложении | приложение |
| GET | `https://queue.itmo.pro/api/v1/queue/archive` | Прошедшие записи в приложении | приложение |
| GET | `https://queue.itmo.pro/api/v1/queue/slots?type_id&table_id` | Слоты в приложении | приложение |

## Модели и правила

| Тема | Правило |
|---|---|
| Запись | `QueueEntry`: `table_id`, `table_name` (точка обслуживания), `table_address`, `queue_comment`, `application_id`, `type_id`, `date` (date-time), `phone`, `comment`, `consult_id`, `consult_fio` |
| Ключи | Очередь задаётся парой `table_id` + `type_id`; слот - `time_table_id`; запись - `application_id` + `type_id` |
| Порядок записи | Очередь (`?table_id&type_id`) -> слоты -> если у очереди есть `template_id`, сначала форма заявки и `POST /api/requests/send` ([requests.md](requests.md)) -> `POST /api/queues/` с `request_id` = полученный `reqId`, иначе `null` |
| Тело POST | Сайт отправляет `JSON.stringify(...)`, то есть строку JSON; с каким `Content-Type` она уходит и примет ли сервер обычный объект, не проверено |
| Телефон | Клиентская проверка: не меньше 11 цифр |
| Тело DELETE | JSON в теле DELETE-запроса (`axios.delete(url, {data})`) |
| Успех | `error_code` 0; текст ошибки сервера сайт показывает из `error_message` |

Полная схема `QueueEntry` в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml).

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `queues`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/).

## Реализации

В my-itmo-api сервиса нет.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_queue_appointments` | `current`, при `include_past` ещё `archive` |

Запись и отмена в itmo-mcp не реализуются: решение владельца проекта.

## Риски и открытые вопросы

- Список очередей и одна очередь вернули пустой массив на проверочном аккаунте: неясно, нет ли открытых очередей
  или нужны параметры. Форма элемента очереди (кроме `template_id`, `table_id`, `type_id`) не известна.
- Запись занимает реальный слот у сотрудника; отмена есть, но правила (за сколько до приёма, лимиты записей)
  не известны.
- Связка очередь + заявка: если заявка подана, а запись в очередь не удалась, заявка остаётся; сайт не откатывает.
- В записях есть телефон пользователя; в логи и фикстуры не сохранять.
- Хост приложения `queue.itmo.pro` не вызывался.
