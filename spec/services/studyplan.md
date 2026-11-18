# Учебный план

## Назначение

Учебный план образовательной программы студента: блоки, модули, дисциплины, зачётные единицы, часы по видам
работ, кафедры, язык, ссылки на РПД, семестры. Там же на сайте выбор дисциплин по выбору внутри плана и заявки
на замену дисциплины. Раздел сайта my.itmo.ru "Учебный план". В Android-приложении 4.13.0 ручек учебного плана
не найдено.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/eduPlanNew/...`, `https://my.itmo.ru/api/eduPlan/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |

См. [../hosts.md](../hosts.md), [../auth.md](../auth.md). Ответы обёрнуты в
`{error_code, error_message, result}` ([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/eduPlanNew/programs` | Планы, доступные пользователю: `planId`, `specializationId`, `name`, `isActive`; плюс `isu` | проверено |
| GET | `/api/eduPlanNew/study_plan/{planId}?spec_id` | Полный рекурсивный план; `spec_id` передаётся, если у программы есть `specializationId` | проверено |
| GET | `/api/eduPlan/choice/{planId}` | Текущий выбор студента внутри плана (загружается вместе с планом) | проверено |
| POST | `/api/eduPlan/{planId}/modules/{moduleId}/disciplines/{disciplineId}/sign` | Выбрать дисциплину по выбору; тело `[{dc_id, semester}]` из `contents` выбранного варианта | сайт |
| DELETE | `/api/eduPlan/{planId}/modules/{moduleId}/disciplines/{disciplineId}/sign` | Отказаться от выбранной дисциплины; тело то же | сайт |
| GET | `/api/eduPlan/{planId}/replaceable_discs` | Поданные замены дисциплин: `disciplineIdFrom` и новая дисциплина | сайт |
| GET | `/api/eduPlan/{planId}/modules/{moduleId}/disciplines/{disciplineId}/replaceable` | Чем можно заменить дисциплину | сайт |
| POST | `/api/eduPlan/{planId}/modules/{moduleId}/disciplines/{discId}/replace/{newDisciplineId}` | Подать замену дисциплины | сайт |
| DELETE | `/api/eduPlan/{planId}/replace/{id}` | Отозвать замену | сайт |

В каталоге `../data/web-api-calls.tsv` пути `eduPlan` обрезаны на первом `concat` (например
`/api/eduPlan/{id}/modules/`); полные пути выше восстановлены по бандлу.

Вне рамок документа: конструктор образовательных программ (`/api/constructor/*`, `/api/constructor-ep/*`,
около 200 ручек для методистов), индивидуальный план (`/api/individual-plan/*`) и ручки консультанта
`/api/services/adviser/services/replace/{isu}/{planId}`.

## Модели и правила

| Тема | Правило |
|---|---|
| Выбор программы | Активная программа - `isActive: true`; `specializationId` часто `null` |
| Шапка плана | `id`, `currentSemester`, `currentSemesterId`, `semestersCount`, `planInfo {directionCode, directionName, programName, levelQualification, planType, startYear}`, `semesters[] {semester, semesterId, semesterParity, studyYear}` |
| Дерево | `structure[]` из `StudyPlanNode` с `type` `block`, `module` или `discipline` и `children` |
| Дисциплина | `creditPoints`, `disciplineDuration`, `description`, `langCode`, `langName`, `rpdUrl`, `department {id, name, shortName}`, `rules` |
| Выборность | `choiceParameterId`, `choiceParameterName`, `choiceAvailable`, `flowSelectable`, `replaceable`, `startSemesterSelectable` |
| Нагрузка | `contents` - словарь "номер семестра строкой" -> `[{id, semester, creditPoints, activities[{name, volume, workTypeId}]}]`; `volume` в академических часах |
| Запись выбора | В тело `sign` уходит `contents[вариант семестра]` как `[{dc_id: content.id, semester}]`; при `startSemesterSelectable` вариант выбирает студент |

Полные схемы: `StudyPlanPrograms`, `StudyPlan`, `StudyPlanNode`, `StudyPlanContent` в
[../openapi/my-itmo.yaml](../openapi/my-itmo.yaml).

## Спецификация

`getStudyPlanPrograms` и `getStudyPlan` - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `studyplan`);
текущий выбор студента `getStudyPlanChoices` - в [../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml),
пример ответа - в [../openapi/examples/](../openapi/examples/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.studyPlan.getStudyPlanPrograms` | `GET /api/eduPlanNew/programs` |
| `itmo.studyPlan.getStudyPlan` | `GET /api/eduPlanNew/study_plan/{planId}?spec_id` |

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_study_plan` | `programs` (активная программа), затем `study_plan/{planId}`; дисциплины одного семестра, по умолчанию `currentSemester` |

Выбор и замена дисциплин в itmo-mcp не реализованы.

## Риски и открытые вопросы

- Ручки `/api/eduPlan/*` восстановлены по коду сайта и не вызывались; сроки, когда выбор открыт, и ошибки
  сервера не известны.
- Связь выбора внутри плана (`eduPlan/.../sign`) с кампанией выбора дисциплин (`/api/election/*`,
  [elections.md](elections.md)) не выяснена: возможно, это два поколения одного процесса.
- Замена дисциплины, вероятно, создаёт работу для сотрудников (согласование); побочные эффекты не проверены.
- Размер ответа `study_plan` большой (весь план со всеми семестрами); фильтровать на клиенте.
