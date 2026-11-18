# Заявки и справки

## Назначение

Электронные заявления и справки студента: справка об обучении, заявления в деканат и другие подразделения
(около 80-90 шаблонов в 15 категориях). Студент выбирает шаблон, заполняет форму, отправляет, следит за статусом,
скачивает PDF заявления и готовые документы, может отозвать необработанную заявку. Раздел сайта my.itmo.ru
"Заявки" и одноимённый экран Android-приложения.

Есть два поколения:

| Поколение | Что это | Состояние |
|---|---|---|
| Legacy `/api/requests/*` | Шаблоны с полями `fields_data`, видимость полей считает сервер | Основной каталог, работает |
| v2 `/api/requests/v2/*` | Формы SurveyJS (`schemaJson`) и процессы на стороне сервера | Для студента живая только форма `p100` (электронная справка с места учёбы) |

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/requests/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://applications.itmo.pro/api/v1/requests/...` | `Authorization: Bearer` токена ITMO.ID клиента `is-app` |

Приложение знает только legacy-ручки; пути после префикса совпадают с сайтом. См. [../hosts.md](../hosts.md),
[../auth.md](../auth.md). Ответы обёрнуты в `{error_code, error_message, result}`; в v2 при успехе
`error_code` - `null`, ошибки строковые (`access_denied`) ([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

Пометка "(GET снят)" означает, что ответ получен вживую при исследовании, но со схемой не сверялся.

### Legacy: `/api/requests/*`

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/requests/all` | Каталог шаблонов по категориям: `{id, name, color, icon, order, requests[{id, name, category_id, type}]}` | проверено |
| GET | `/api/requests/{templateId}` | Схема формы шаблона, ответственные подразделения, можно ли подать сейчас | проверено |
| GET | `/api/requests/dict/{dictionaryId}?field&q&dep` | Варианты поля-справочника; `dep` - значение поля, от которого он зависит | проверено |
| POST | `/api/requests/form_update` | Пересчёт видимых полей после изменения одного поля, без побочных эффектов | проверено |
| POST | `/api/requests/upload` | Загрузка вложения, multipart-поле `file`; `result.name` идёт значением поля типа `file` | сайт |
| POST | `/api/requests/send` | Подать заявку; проверено 2026-10-05 на шаблоне справки 2706 и откатано отменой | проверено |
| GET | `/api/requests/my` | Свои заявки: `id`, `name`, `notice`, `status`, `status_name`, `template_id`, `created_at`, `updated_at` | проверено |
| GET | `/api/requests/my/{requestId}` | Детали заявки: введённые поля, статус, файлы для печати | проверено |
| GET | `/api/requests/my/{requestId}/download` | PDF поданного заявления | проверено |
| DELETE | `/api/requests/my/{requestId}` | Отозвать заявку, если она не обработана и не отклонена; необратимо; проверено 2026-10-05 | проверено |
| GET | `/api/requests/file/{fileId}` | Файл-приложение шаблона (`template_files[].file_id`), ссылка в форме | сайт |
| GET | `/api/requests/files/my/{data}` | Файл, загруженный в поле поданной заявки (`entered_fields[].data`) | сайт |

Приложение: те же пути на `https://applications.itmo.pro/api/v1/requests/...` (`all`, `{id}`, `dict/{id}`,
`form_update`, `upload`, `send`, `my`, `my/{id}` GET и DELETE, `my/{id}/download`, `file/{templateId}`,
`files/my/{data}`). Статус: приложение.

### v2 (SurveyJS): `/api/requests/v2/*`, ручки студента

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/requests/v2/forms` | Доступные формы по категориям: `[{id, code, name, icon, color, sortOrder, forms[{id, name, description, version, alias}]}]` | проверено |
| GET | `/api/requests/v2/forms/{alias}` | Форма: `name`, `description`, `htmlNote`, `category`, `version`, `schemaJson` (строка), `alias`, `accessCodes` | проверено |
| GET | `/api/requests/v2/categories` | Категории форм | проверено |
| GET | `/api/requests/v2/dictionaries` | Список справочников `[{code, name}]` | проверено |
| POST | `/api/requests/v2/dictionaries/resolve?code&...` | Варианты справочника для вопроса формы; POST только для чтения, тело - контекст формы, в параметрах код, фильтр, `skip`, `take` | сайт |
| GET | `/api/requests/v2/prefills` | Описания предзаполнений (GET снят: пустой массив) | сайт |
| POST | `/api/requests/v2/prefills/resolve` | Значение предзаполнения для вопроса формы; только чтение | сайт |
| POST | `/api/requests/v2/requests` | Подать заявку: `{alias, payloadJson}`, `payloadJson` - JSON-строка данных SurveyJS; в ответе `id` | сайт |
| GET | `/api/requests/v2/requests/mine` | Свои заявки v2 | проверено |
| GET | `/api/requests/v2/requests/{id}` | Детали заявки v2: `status`, `statusCode`, `createdAt`, `files` | сайт |
| GET | `/api/requests/v2/requests/{id}/files/{variable}` | Готовый файл (подписанная справка): `{base64Content, contentType, filename}` | сайт |
| GET | `/api/requests/v2/platform-roles/me` | Роль в платформе: `{userId, role, explicit}` (GET снят) | сайт |
| GET | `/api/requests/v2/requests/task-counts` | Счётчики задач: `{ownRequestTasks, assignedRequestTasks}` | проверено |
| GET | `/api/requests/v2/tasks/mine/request-ids` | Заявки, где у пользователя есть задача | проверено |
| GET | `/api/requests/v2/tasks/mine/by-request/{id}` | Задача пользователя по заявке | сайт |
| POST | `/api/requests/v2/tasks/{id}/complete` | Выполнить задачу процесса (исполнитель или согласующий) | сайт |

Отзыва заявки студентом в v2 нет. Вне рамок документа - администрирование v2 (роль `EDITOR` и выше):
`processes*` (GET вернул `access_denied`, "Role EDITOR is required"), `access-definitions*`, `dictionaries/admin*`,
`dictionaries/test`, `platform-roles*`, `context-enrichments`, `users`, `tasks`, `requests/my-tasks`,
`requests/observed*`, PUT `categories`, POST и PUT `forms`. Статус: сайт.

## Модели и правила

### Форма legacy

| Тема | Правило |
|---|---|
| Шаблон | `template_name`, `template_description` (HTML), `template_files`, `responsible_units[] {dep_name, dep_link}`, `unique_flag`, `user_can_apply_now`, `user_cannot_apply_now_reason`, `fields_data[]` |
| Поле | `field_id`, `field_name`, `field_type`, `required_field_flag`, `disabled_field_flag`, `show_condition_flag` (видно изначально), `dictionary_id`, `dependent_field_id`, `multiple_choice`, `default_value`, `init_dictionary {id, text}`, `field_note` |
| Типы | `text`, `number`, `date`, `date_time`, `dictionary`, `file`, `multiple_field` |
| Начальное состояние | Значение справочника - `init_dictionary.id`, остальных - `default_value` |
| Справочник | Варианты `{id, text}`, `id` строкой; зависимый справочник запрашивается с `dep` = значение поля `dependent_field_id` |

### form_update

- Тело `{changed_field, current_values[]}`; `changed_field` - числовой id изменённого поля.
- `current_values` должен содержать каждое поле формы: `{field_id: "строка", field_type, value}`, где `field_type`
  указывается только для полей не-справочников, пустое значение - `""`.
- Если чего-то не хватает, сервер отвечает HTTP 400, `error_code` 101 "Can not parse JSON". Формат снят с живого
  трафика браузера.
- Ответ `{show[], hide[]}` - id полей, которые стали видимыми или скрытыми.

### send

- Тело `{request_id: templateId, values[{field_id, field_type, value}]}`.
- Кодирование `value`: справочник - id варианта (через запятую при множественном выборе), `date` - `DD.MM.YYYY`,
  `date_time` - `DD.MM.YYYY HH:mm`, остальное - строка.
- Значения скрытых полей не отправлять.
- Ошибки валидации приходят как `result.error_list[{field_id, error_text}]` (HTTP-код при этом не зафиксирован,
  itmo-mcp читает конверт и из ошибочного ответа); успех - `result.reqId`.
- `reqId` используется в записи в электронную очередь для очередей с шаблоном ([queues.md](queues.md)) и в смене
  категории общежития ([dormitory.md](dormitory.md), шаблон 5587).

### Статусы

| Где | Поля |
|---|---|
| Список `my` | `status` (число) и `status_name` (текст), `notice` - комментарий подразделения |
| Детали `my/{id}` | `request_status`, `request_state`, `request_status_tag`: `processed`, `rejected` или другое; `entered_fields[] {field_id, name, type, data}`, `print_files[] {print_file_name, print_file_path}` |
| Отмена | Разрешена, пока `request_status_tag` не `processed` и не `rejected` |
| v2 | Финальные `status`: `PROCESSED`, `REJECTED` |

Полные схемы legacy: `RequestTemplate`, `RequestField`, `RequestOption`, `RequestSummary`, `RequestDetails` в
[../openapi/my-itmo.yaml](../openapi/my-itmo.yaml). v2 в спецификацию не входит.

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `requests`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.requests.getMyRequests` | `GET /api/requests/my` |

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_requests_catalog` | `all`, фильтр по подстроке |
| `itmo_request_form` | `{templateId}`, `dict/{id}` для справочников (до 40 вариантов на поле) |
| `itmo_get_requests` | `my` |
| `itmo_get_request_details` | `my/{id}` |
| `itmo_request_submit_preview` | `{templateId}`, `all`, `dict/{id}`, `form_update` на каждое заданное поле, затем после `itmo_confirm_action` - POST `send` |
| `itmo_request_cancel_preview` | `my/{id}` и проверка статуса, затем после `itmo_confirm_action` - DELETE `my/{id}` |

Правила itmo-mcp:

- Шаблоны и категории, совпадающие с отчислением, академическим отпуском, переводом или восстановлением,
  отклоняются: такие заявления студент подаёт сам.
- Заполняются только поля `text`, `number`, `date`, `date_time`, `dictionary`; обязательные `file` и
  `multiple_field` - отказ с предложением подать на сайте.
- Шаблон с `user_can_apply_now: false` не подаётся.

Не покрыты: загрузка файлов, скачивание PDF и готовых документов, вся v2.

## Риски и открытые вопросы

- `send` создаёт реальную работу подразделению; отзыв возможен только до обработки. Любой новый живой тест -
  только на безобидном шаблоне с немедленной отменой и согласием владельца.
- Поведение `send` для полей `file` и `multiple_field` (формат значения, `multiple_field_data`,
  `multiple_field_max_block`) не исследовано.
- Значения `status` в списке `my` не сопоставлены с `request_status_tag`; справочник статусов не найден.
- `print_files[].print_file_path` не используется сайтом как ссылка; как скачать эти файлы, не выяснено.
- v2: формат `payloadJson` зависит от `schemaJson` конкретной формы; для `p100` подача живьём не проверялась.
  Форма `files/{variable}`: имя переменной берётся из деталей заявки, точный ключ поля (`variableName`) не
  проверен. Набор форм v2 будет расти; отзыва студентом нет.
- Связь legacy и v2 (переедут ли шаблоны) не известна.
