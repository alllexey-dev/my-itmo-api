# БАРС (балльно-рейтинговая система)

## Назначение

Журнал текущих баллов по дисциплинам: контрольные точки, баллы, экзамен, итоговая оценка. Студент видит свой
журнал; преподаватели и администраторы выставляют баллы, дедлайны, утверждают ведомости. Веб-клиент -
`https://bars.itmo.ru` (React SPA).

## Хосты и авторизация

| Что | Значение |
|---|---|
| REST | `https://bars.itmo.ru/backend/rest` (бандл обращается к `/rest/...` относительно своего префикса) |
| Вход | ITMO.ID, клиент `bars`, authorization code без PKCE, redirect `https://bars.itmo.ru/rest/login` |
| Сессия | GET `/login?code&customRedirectUri` возвращает заголовок ответа `authorization` со значением `Bearer <токен сессии>`. Клиент, написанный вручную, отправляет это значение обратно как `Authorization` целиком. Клиенту, сгенерированному из `bars.yaml` (схема `http`/`bearer`), передают значение без префикса `Bearer `: он допишет его сам |
| Срок | Около 30 минут, refresh-токена нет, токен my.itmo.ru на сессию БАРС не обменивается |
| Продление | Повторить авторизацию ITMO.ID с cookie `KEYCLOAK_IDENTITY` и отдать БАРС новый code |

Подробности входа и cookie - в [../auth.md](../auth.md). Ответы БАРС не обёрнуты в `{error_code, result}`:
тело - сразу массив или объект.

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

Пути относительно `https://bars.itmo.ru/backend/rest`. Уточнение в скобках после "проверено" говорит, что именно
наблюдалось, если ответ не дал формы данных.

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/login?code&customRedirectUri` | Обмен кода ITMO.ID на сессию в заголовке `authorization`; выполняется в каждом прогоне живой проверки ([../sources.md](../sources.md)), тело не сверяется | проверено |
| GET | `/users/current_user/` | Пользователь, роли, выбранный период `selected_year`, `selected_term` | проверено |
| GET | `/config/` | Глобальные настройки `[{id, name, value}]`, в том числе `current_year`, `current_term` | проверено |
| GET | `/journal/disciplines?withCheckpointPlansOnly` | Дисциплины выбранного периода: `id`, `name`, `terms`, `checkpoint_plan_ids` | проверено |
| GET | `/journal/groups-and-flows?checkpointPlanId&disciplineId` | Группы и потоки журнала: `type`, `identifier`, `checkpoint_plan_ids` | проверено |
| GET | `/marks/{checkpointPlanId}/{type}/{identifier}/student` | Журнал студента: заголовки контрольных точек и свои баллы | проверено |
| GET | `/marks/{cp}/{type}/{identifier}/student/{studentId}/history?checkpointId` | История изменений баллов. Живой ответ - пустой массив, форма элемента не известна | проверено (пустой ответ) |
| GET | `/deadline` | Дедлайны пользователя. Живой ответ - пустой массив; поля элемента в `bars-extra.yaml` взяты из кода БАРС | проверено (пустой ответ) |
| GET | `/config/personal` | Личные настройки, переопределяющие глобальные (год и семестр). Живой ответ - пустой массив: настройки не переопределялись | проверено (пустой ответ) |
| POST | `/config/personal` | Смена личной настройки `{name: "current_year" или "current_term", value}`, то есть периода | сайт |
| DELETE | `/config/personal/{id}` | Удаление личной настройки | сайт |
| POST | `/marks/{cp}/student/agreement` | "Ознакомлен с системой оценивания", без тела, необратимо | сайт |
| GET | `/journal/checkpoint-plans?disciplineId` | Планы контрольных точек дисциплины: `id`, `name`, `terms`, `programs`; зачем они студенту, не выяснено | проверено |
| DELETE | `/journal/cache?term` | Судя по методу и имени, сброс кэша журнала, а не чтение: на `GET` сервер отвечает `405` с заголовком `Allow: DELETE` (проверено 2026-11-18). Сам `DELETE` не вызывался; в спецификациях ручки нет | сайт |

Преподавательские и административные ручки из бандла (не для студента, не описываются подробно): `/marks/final/`,
`/marks/additional/`, `/marks/{cp}/{type}/approval/{id}/history`, approve, `/deadline/{a}/{b}/{c}`,
`/deadline/single/...`, `/checkpoint_plans*`, `/checkpoint_types*`, `/disciplines*`, `/flows*`, `/groups`,
`/personal_plans*`, `/report*`, `/tests`, `/users*`, `/users/set_selected_role`, `/set_me_to/{id}`,
`/registration/token*`, `/google/import/sheet`, `/google/export/sheet`, `/educational_programs/`. Статус: сайт.

## Модели и правила

| Тема | Правило |
|---|---|
| Поиск журнала | `disciplines` -> каждый `checkpoint_plan_ids` -> `groups-and-flows?checkpointPlanId` -> `marks/{cp}/{type}/{identifier}/student` |
| `groups-and-flows` | Оба фильтра работают у студента: `checkpointPlanId` и `disciplineId` (проверено 2026-11-18 на всех дисциплинах периода). Неизвестный идентификатор даёт пустой массив, нечисловое значение - `400` |
| `type`, `identifier` | Непрозрачные сегменты пути, кодировать отдельно |
| Период | Определяется личной настройкой на сервере; смена через POST `/config/personal` видна и в веб-интерфейсе |
| Журнал | `headers.plan.regular_checkpoints` и `final_checkpoint` с `min_grade`, `max_grade`, `week`, `key`; у студента `marks.regular`, `regularSum`, `additional`, `final`, `total`, `active_approvals` |
| Права | В журнале есть флаги `can_edit_*`, `can_approve_*`; у студента ожидаемо ложны |
| Согласие | `setAgreement` отмечает, что студент ознакомлен с системой оценивания; отменить нельзя |
| Идентификаторы | id БАРС не совпадают с id my.itmo |

## Спецификация

Вход и основные ручки - в [../openapi/bars.yaml](../openapi/bars.yaml); планы контрольных точек, дедлайны, личные настройки и история
оценок - в [../openapi/bars-extra.yaml](../openapi/bars-extra.yaml), примеры ответов - в [../openapi/examples/](../openapi/examples/).
Синтетические ответы основных ручек - в [../../fixtures/bars/](../../fixtures/bars/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.bars.login` | `GET /login?code&customRedirectUri` |
| `itmo.bars.getCurrentUser` | `GET /users/current_user/` |
| `itmo.bars.getConfig` | `GET /config/` |
| `itmo.bars.setPersonalSetting` | `POST /config/personal` |
| `itmo.bars.selectPeriod`, `itmo.bars.withPeriod` | `GET /users/current_user/` и `POST /config/personal` только для изменившихся настроек; после смены период перечитывается и сверяется |
| `itmo.bars.getDisciplines` | `GET /journal/disciplines?withCheckpointPlansOnly` |
| `itmo.bars.getGroupsAndFlows` | `GET /journal/groups-and-flows` с необязательным `disciplineId` |
| `itmo.bars.getStudentJournal` | `GET /marks/{checkpointPlanId}/{type}/{identifier}/student` |

После входа по паролю библиотека сама получает и продлевает сессию БАРС по cookie `KEYCLOAK_IDENTITY`; при ответе
401 один раз запрашивает новый код и повторяет запрос. Если ответ БАРС содержит заголовок `authorization`,
сохраняется новое значение сессии.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `bars_get_scores` | `users/current_user/`, `journal/disciplines?withCheckpointPlansOnly=true`, `journal/groups-and-flows?checkpointPlanId`, `marks/.../student` (до 4 параллельно) |

Сессия получается автоматически через ITMO.ID и хранится в памяти; при отказе БАРС забывается и запрашивается
заново. Смена периода, история, дедлайны и согласие не используются: инструмент читает только текущий период.

## Риски и открытые вопросы

- Чтение другого семестра требует POST `/config/personal`, что меняет выбранный период и в веб-интерфейсе.
  ITMO.Widgets после чтения возвращает прежний период (статус: проекты); itmo-mcp так не делает.
- Форма ответов `history`, `deadline` и `config/personal` не известна: на исследуемом аккаунте пустые массивы.
  Схемы элементов в `bars-extra.yaml` для них взяты из кода БАРС и помечены "Observed empty".
- Что произойдёт без согласия `agreement` (скрытие баллов или только баннер), не выяснено.
- Время жизни сессии около 30 минут получено наблюдением; сервер может завершить её раньше.
- Роль `/journal/checkpoint-plans` для студента не изучена.
