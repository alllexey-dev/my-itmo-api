# Выбор дисциплин, факультативы и "Вселенная ИТМО"

## Назначение

Три похожие кампании выбора на my.itmo.ru, открывающиеся по расписанию на несколько дней в семестр:

| Кампания | Префикс | Что выбирают |
|---|---|---|
| Выбор дисциплин (выборность) | `/api/election/students/*` | Элективные дисциплины следующего семестра, затем потоки (группы занятий) по ним |
| Факультативы | `/api/facultative/*` | Потоки факультативных курсов |
| "Вселенная ИТМО" | `/api/intro/*` | Потоки курсов "Вселенная ИТМО" (модуль `intro` на сайте) |

Места в потоках ограничены, поэтому в момент открытия кампании идёт гонка за места. В Android-приложении 4.13.0
ручек этих кампаний не найдено.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/election/...`, `/api/facultative/...`, `/api/intro/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Dev-стенд | `https://dev.my.itmo.su` | Клиент `student-personal-cabinet-dev` |

См. [../hosts.md](../hosts.md), [../auth.md](../auth.md). Ответы обёрнуты в `{error_code, error_message, result}`;
HTTP 2xx не означает успех, смотреть `error_code` ([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

### Выбор дисциплин: `/api/election/students/*`

Источник "проекты" - itmo-fastpick (HAR-записи dev-стенда) и KMP-клиент `ElectionApi` из my-itmo-api.

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/availability` | Кампания: статус, даты и время открытия и закрытия, семестр | проверено |
| GET | `/available_disciplines` | Доступные дисциплины с непрозрачными `groupFlow` по семестрам | проекты |
| POST | `/group_flow_available_disciplines` | Проверка полного набора: тело `["groupFlow", ...]`; ничего не сохраняет | проекты |
| POST | `/order/` | Сохранить дисциплины: тело `["groupFlow", ...]` заменяет весь набор | проекты |
| GET | `/ordered_flow_chains` | Выбранные дисциплины с рекурсивными деревьями потоков | проекты |
| GET | `/chosen_flows` | Выбранные потоки: `[flowId]` | проекты |
| POST | `/order/change` | Сохранить потоки: тело `[flowId, ...]` заменяет весь набор | проекты |
| POST | `/order/clear` | Сбросить выбранные потоки, без тела | проекты |
| GET | `/limits/flow_groups` | Вместимость карточек дисциплин | проекты |
| GET | `/limits/flows` | Вместимость потоков: словарь id -> `{limitMax, occupied, free}` | проекты |
| POST | `/schedule/flows/intersections` | Пересечения в расписании для набора потоков (тело - текущий выбор) | проекты |
| POST | `/schedule/combined?date_start&date_end` | Расписание набора потоков на период (тело - текущий выбор) | проекты |
| GET | `/schedule/base/timeline?date_start&date_end` | Базовое (обязательное) расписание семестра | сайт |
| GET | `/schedule/flows/{id}/timeline` | Расписание одного потока | проекты |
| GET | `/selected_flow_chains` | Устаревшая; отвечает 404 | проверено |

### Факультативы и "Вселенная ИТМО": `/api/facultative/*` и `/api/intro/*`

API одинаковое, отличается только префикс. `{p}` ниже - `facultative` или `intro`.

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/{p}/status/` | Состояние кампании: `semester {choice_status, date_start, date_end, semester, study_year}` | проверено |
| GET | `/api/{p}/json/` | Дерево модулей и потоков: `result.json` (GET снят: код 97) | сайт |
| GET | `/api/{p}/current/` | Текущий выбор: `flow_id[]`, `intersections` (GET снят: код 97) | сайт |
| GET | `/api/{p}/description/`, `/api/{p}/description/start/` | Описания курсов: список и структурированный вид | проверено |
| GET | `/api/{p}/limits/` | Места: `limits`, `start_time`, `end_time` (GET снят: код 97) | сайт |
| GET | `/api/{p}/schedule/user/` | Базовое расписание студента: `result.json` (GET снят: код 97) | сайт |
| GET | `/api/{p}/schedule/flow/{flowId}/` | Расписание потока | сайт |
| POST | `/api/{p}/booking/` | Занять и освободить места сразу: `{selected_flows: [id], canceled_flows: [id]}` | сайт |
| POST | `/api/{p}/commit/` | Подтвердить выбор: `{status: 2, flow_id: [все выбранные]}`; вернуть в редактирование: `status: 1` | сайт |
| GET | `/api/{p}/json/{isu}`, `/api/{p}/current/{isu}`, `/api/{p}/limits/{id}` | Варианты с ISU или id: модуль проверки выбора (`electionChecker`) и часть вызовов `intro` | сайт |

## Модели и правила

### Выбор дисциплин

| Тема | Правило |
|---|---|
| Кампания | `availability`: `id`, `status` (текст), `semesterStart`, `semesterEnd`, `dateStart`, `dateEnd`, `timeStart`, `timeEnd`, `studyYear`, `semesterId`, `semester`; по наблюдениям проектов `id` 1 - кампания открыта |
| Дисциплина | `dcId`, `discId`, `langId`, `discName`, `depName`, `depNameShort`, `langCode`, `required`, `description`, `notCompatibleWith[]`, `semesters[] {semester, statusId, groupFlow, statusName}` |
| `groupFlow` | Непрозрачная строка, не парсить как число; на dev и production разная, сопоставлять по `discId` |
| Проверка | `group_flow_available_disciplines` -> `disciplines`, `validVariantSelection`, `validRequiredSelection`, `needSelectRequired`, `needSelectVariants`, `needSelectVariantsMax`, `scheduleAvailable` |
| Дерево потоков | `ordered_flow_chains[] {groupFlow, disciplineId, disciplineName, flows[]}`, поток - `{id, name, limitMax, teachers[], variants[] (рекурсивно), workType, available, selections[]}` |
| Замена целиком | `order/` и `order/change` заменяют весь набор, а не добавляют элемент; отправлять полный список |
| Порядок | availability и disciplines -> локальный выбор -> одна проверка -> `order/` -> chains и limits -> локальный выбор потоков -> `order/change` -> контроль через `ordered_flow_chains` и `chosen_flows` |

### Факультативы и "Вселенная ИТМО"

| Тема | Правило |
|---|---|
| `choice_status` | 97 - выбор ещё не открыт; остальные значения не наблюдались |
| Закрытая кампания | Все GET, кроме `status/`, отвечают `error_code` 97 "Выборность еще не открыта", `result: null` |
| `booking/` | Места занимаются или освобождаются сразу, без подтверждения; `error_code` 0 или 109 - успех (109 - успех с пересечениями в расписании) |
| `commit/` | `status` 2 - подтвердить, 1 - вернуть в редактирование; `flow_id` - все выбранные потоки; сайт не даёт подтвердить при пересечениях |

## Спецификация

Статус кампании `getElectionAvailability` - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `election`);
остальное чтение выбора дисциплин (тег `election`) и
факультативы с "Вселенной ИТМО" (тег `choice`) - в [../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml),
примеры ответов - в [../openapi/examples/](../openapi/examples/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.election.getElectionAvailability` | `GET /api/election/students/availability` |
| `itmo.election.getAvailableDisciplines` | `GET /api/election/students/available_disciplines` |
| `itmo.election.validateSelectedDisciplines` | `POST /api/election/students/group_flow_available_disciplines` |
| `itmo.election.getFlowLimits` | `GET /api/election/students/limits/flows` |
| `itmo.election.getOrderedFlowChains` | `GET /api/election/students/ordered_flow_chains` |
| `itmo.election.getChosenFlows` | `GET /api/election/students/chosen_flows` |
| `itmo.election.changeSelectedDisciplines` | `POST /api/election/students/order/` |
| `itmo.election.changeSelectedFlows` | `POST /api/election/students/order/change` |
| `itmo.election.clearAllSelectedFlows` | `POST /api/election/students/order/clear` |

Факультативов и "Вселенной ИТМО" (`/api/facultative/*`, `/api/intro/*`) в библиотеке нет.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_election_status` | `election/students/availability` |

Остальные ручки, включая все записи, не покрыты. Быстрый выбор реализован отдельно в itmo-fastpick.

## Риски и открытые вопросы

- На момент исследования все кампании закрыты: выбор дисциплин отвечает `error_code` 112 "выборность закрыта",
  факультативы и "Вселенная ИТМО" - 97, часть ручек - 403. Формы ответов открытой кампании известны только из
  проектов (dev-стенд) и кода сайта.
- `order/`, `order/change`, `booking/` меняют реальное распределение мест; при гонке за места ошибка или повтор
  могут стоить места. Живьём на production не проверялись.
- `order/clear` сбрасывает все потоки; восстановить можно только повторным `order/change`, если места ещё есть.
- Форма ответов `order/*` не гарантирована (в KMP - произвольный JSON, иногда `{status, name, flows}`).
- Права на варианты с `{isu}` (просмотр выбора другого студента) не выяснены; скорее всего, только для сотрудников.
- Связь с выбором дисциплин внутри учебного плана (`/api/eduPlan/.../sign`, [studyplan.md](studyplan.md)) не
  выяснена.
- Сайт делает много лишних запросов (десятки `limits` и `intersections` за сессию); клиенту стоит
  дебаунсить и не перегружать сервер в момент открытия.
