# Персоналии (люди ИТМО)

## Назначение

Справочник студентов и сотрудников: поиск по ФИО, номеру ИСУ и другим атрибутам, публичная карточка человека
(должности, подразделения, контакты, аудитории, учёба, звания) и лента активностей. Раздел сайта my.itmo.ru
"Персоналии" и поиск людей в Android-приложении. Доступно любому авторизованному пользователю.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/personalities/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://api.itmo.su/person/v1/...` | `Authorization: Bearer` токена ITMO.ID клиента `is-app` |

Пути `persons` и `persons/{isu}` совпадают после префикса. См. [../hosts.md](../hosts.md),
[../auth.md](../auth.md). Ответы обёрнуты в `{error_code, error_message, result}`
([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/personalities/persons?q&limit&offset` | Поиск людей; `result {count, data[]}`, `count` - всего совпадений | проверено |
| GET | `/api/personalities/persons/{isu}` | Публичная карточка по номеру ИСУ | проверено |
| GET | `/api/personalities/persons/{isu}/activities?type&...` | Активности человека постранично (`count`, `data`), фильтр `type` | проверено |
| GET | `https://api.itmo.su/person/v1/persons?limit&q&offset` | Поиск в приложении | приложение |
| GET | `https://api.itmo.su/person/v1/persons/{isu}` | Карточка в приложении | приложение |

Расписание преподавателя из карточки в приложении берётся с `api.schedule.itmo.su/api/v3/teacher/{isu}`, см.
[schedule.md](schedule.md).

## Модели и правила

| Тема | Правило |
|---|---|
| Поиск | Все три параметра обязательны (`q`, `limit`, `offset`); элемент `PersonShort`: `id` (обычно ISU), `fio`, `gender`, `phone`, `email`, `work`, `photo`, `education` |
| Карточка | `Person`: `isu`, `fio`, `gender`, `photo`, `contacts[] {contact[], contact_alias}`, `rooms[] {room_number, bld_name}`, `positions[] {department_name, department_link, position_name, vacation...}`, `education[] {course, faculty_name, group}`, `powers`, `levels {rank, degree}`, `activities`, `exchange_training` |
| Пустые поля | У студентов `contacts`, `rooms`, `positions` обычно пустые массивы, `levels` - `null` |
| Неизвестный ISU | HTTP 400, `error_code` 100, `result: null` |
| Своя карточка | ISU текущего пользователя есть в claim `isu` токена ITMO.ID и в `eduPlanNew/programs.isu` |

Полные схемы: `Person`, `PersonShort` в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml).

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `personalities`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.personalities.getPersonality` | `GET /api/personalities/persons/{isu}` |
| `itmo.personalities.searchPersonalities` | `GET /api/personalities/persons?q&limit&offset` |

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_profile` | `persons/{isu}` для ISU из токена |
| `itmo_get_person` | `persons/{isu}` для произвольного ISU |
| `itmo_search_people` | `persons?q&limit&offset`; `limit` до 50 |

`activities` не покрыты.

## Риски и открытые вопросы

- Ответы содержат персональные данные других людей (телефоны, почты, группы, аудитории). Сохранять их в
  фикстуры, логи и отчёты нельзя; при выводе брать только то, что нужно для ответа.
- Набор полей, видимых студенту и сотруднику, может отличаться; проверено только под студентом.
- Параметры `activities` (кроме `type`), значения типов и форма элементов не выяснены.
- Какие атрибуты, кроме ФИО и ISU, ищет `q`, не проверено.
- Хост приложения `api.itmo.su/person/v1` не вызывался; совместимость с токеном my.itmo.ru не известна.
