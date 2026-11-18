# Справочник методов

Все вызовы библиотеки по разделам. `itmo` - объект `MyItmo`; те же разделы есть у `MyItmoClient` и, без
корутин, у `BlockingMyItmo` ([Java](java.md)).

Общие правила:

- каждый метод - `suspend`, возвращает данные и бросает `MyItmoException` ([ошибки](errors.md));
- даты - `kotlinx.datetime.LocalDate`, диапазоны включают обе границы;
- время в ответах московское, см. [время](advanced.md#время-москва);
- поля моделей описаны в KDoc: откройте класс в IDE.

В колонке "Запрос" - то, что уходит на `https://my.itmo.ru`. Те же ручки подробно, с полями и причудами, описаны
в [spec/services/](../spec/services/).

## Клиент: `MyItmo`

| Вызов | Что делает |
|---|---|
| `MyItmo.login(username, password)` | Вход по паролю |
| `MyItmo.loginOrResume(username, password, storage)` | Берёт сессию из хранилища, а без неё входит по паролю |
| `MyItmo.fromRefreshToken(token)` | Клиент из refresh token, сразу проверяет его |
| `MyItmo.fromStorage(storage)` | Клиент из сохранённой сессии, без запросов |
| `itmo.hasSession()` | Есть ли сохранённая сессия с непросроченным refresh token |
| `itmo.logout()` | Завершает сессию в ITMO.ID и очищает хранилище |
| `itmo.close()` | Освобождает HTTP-клиент |

Подробности - в [auth.md](auth.md).

## Расписание: `itmo.schedule`

| Метод | Возвращает | Запрос |
|---|---|---|
| `schedule.getPersonalSchedule(dateStart, dateEnd)` | `List<Schedule>`: по элементу на каждый день, пустые дни тоже | `GET /api/schedule/schedule/personal` |
| `schedule.getTeacherSchedule(isu, dateStart, dateEnd)` | `List<Schedule>`: пары, которые ведёт преподаватель | `GET /api/schedule/teacher/{isu}` |
| `schedule.getTimeSlots()` | `List<ExtendedTimeSlot>`: стандартные интервалы пар | `GET /api/schedule/meta/time_slots` |

## Зачётка: `itmo.recordBook`

| Метод | Возвращает | Запрос |
|---|---|---|
| `recordBook.getSpecializations()` | `List<Specialization>`: программы и их семестры | `GET /api/record_book/specializations` |
| `recordBook.getRecordBook(specializationId, semester)` | `List<RecordBookEntry>`: дисциплины и оценки семестра. `specializationId` - это `Specialization.mainPlan`, семестры нумеруются с 1 | `GET /api/record_book/{specialization_id}/{semester}` |
| `recordBook.getControlEntries(recordBookEntryId)` | `List<ControlEntry>`: дерево контрольных мероприятий дисциплины (связь через `parentId`). Идентификатор - `RecordBookEntry.estId` | `GET /api/record_book/{record_book_entry_id}` |

## Учебный план: `itmo.studyPlan`

| Метод | Возвращает | Запрос |
|---|---|---|
| `studyPlan.getStudyPlanPrograms()` | `StudyPlanPrograms`: доступные планы и текущая программа | `GET /api/eduPlanNew/programs` |
| `studyPlan.getStudyPlan(planId, specializationId = null)` | `StudyPlan`: полное дерево плана | `GET /api/eduPlanNew/study_plan/{plan_id}` |

## Люди: `itmo.personalities`

| Метод | Возвращает | Запрос |
|---|---|---|
| `personalities.getPersonality(isu)` | `Personality`. Неизвестный номер ИСУ - `MyItmoException.Api` с кодом 100 | `GET /api/personalities/persons/{isu}` |
| `personalities.searchPersonalities(limit, offset, query)` | `CountWrapper<List<PersonalityMin>>`: страница и общее число результатов в `count` | `GET /api/personalities/persons` |

## Спорт: `itmo.sport`

Как записаться на занятие по шагам - в [sport.md](sport.md).

| Метод | Возвращает | Запрос |
|---|---|---|
| `sport.getSportSchedule(dateStart, dateEnd, buildingId = null, sportTypeIds = null, teacherIsu = null)` | `List<SportSchedule>`: занятия, на которые можно записаться | `GET /api/sport/sign/schedule` |
| `sport.getSportFilters()` | `SportFilters`: значения фильтров расписания | `GET /api/sport/sign/schedule/filters` |
| `sport.getSportTimeSlots()` | `List<TimeSlot>`: интервалы спортивных занятий | `GET /api/sport/time_slots` |
| `sport.getSportTypes()` | `List<IdValuePair>`: виды спорта | `GET /api/sport/sport_types` |
| `sport.getSportSignLimits()` | `Map<Long, Map<Long, SportSignLimit>>`: лимиты записи; пустая карта - особых ограничений нет | `GET /api/sport/sign/schedule/limits` |
| `sport.signInLesson(lessonId)` | `SportSignResult`: записан, мест нет, отказ или решения нет. Не бросает `MyItmoException`. `lessonId` - это `SportLesson.id` из расписания ([как записаться](sport.md)) | `POST /api/sport/sign/schedule/lessons` |
| `sport.signInLessons(lessonIds)` | `List<Long>`: занятия, на которые записали. Отказ - `MyItmoException.Api` | `POST /api/sport/sign/schedule/lessons` |
| `sport.signOutLessons(lessonIds)` | `List<Long>`: занятия, с которых сняли запись | `DELETE /api/sport/sign/schedule/lessons` |
| `sport.getChosenSportSections()` | `List<ChosenSportSection>`: выбранные секции и регулярные слоты | `GET /api/sport/sign/chosen` |
| `sport.getPersonalSportCalendar(dateStart, dateEnd)` | `List<SportSchedule>`: личный календарь, включая прошедшие занятия | `GET /api/sport/personal/calendar` |
| `sport.getSportScore(semesterId = null)` | `SportScore`: баллы и история начислений | `GET /api/sport/personal/score` |
| `sport.getSportAttempts()` | `SportAttempts`: использованные и оставшиеся попытки записи | `GET /api/sport/personal/have_attempts` |
| `sport.getSportSignAttempts()` | `Int`: отдельный серверный счётчик попыток; смысл не документирован, для интерфейса берите `getSportAttempts` | `GET /api/sport/personal/sign_attempts` |
| `sport.getSportDebt()` | `SportDebt`: задолженность по физкультуре | `GET /api/sport/personal/debt` |
| `sport.getSportHealthLevel()` | `SportHealthLevelResponse`: медицинская группа | `GET /api/sport/personal/health_level` |
| `sport.getSportExternat()` | `SportExternat`: статус заявки на экстернат | `GET /api/sport/personal/externat` |
| `sport.getSportSelections()` | `List<SportSelection>`: отборы в секции и их нормативы | `GET /api/sport/personal/selections` |
| `sport.getSportProjects()` | `List<SportProject>`: специальные форматы зачёта | `GET /api/sport/projects/list` |
| `sport.getSportSemesters()` | `List<SportSemesterOption>`: семестры для выбора истории | `GET /api/sport/semesters/list` |
| `sport.getCurrentSportSemester()` | `SportSemester`: текущий семестр и даты записи | `GET /api/sport/semesters/current` |

## Выбор дисциплин: `itmo.election`

При закрытой кампании все методы, кроме `getElectionAvailability`, бросают `MyItmoException.Api` с кодом 112.

| Метод | Возвращает | Запрос |
|---|---|---|
| `election.getElectionAvailability()` | `ElectionAvailability`: состояние кампании и даты | `GET /api/election/students/availability` |
| `election.getAvailableDisciplines()` | `List<AvailableDiscipline>` | `GET /api/election/students/available_disciplines` |
| `election.validateSelectedDisciplines(groupFlowIds)` | `DisciplineSelectionValidation`: проверка набора, ничего не сохраняет | `POST /api/election/students/group_flow_available_disciplines` |
| `election.changeSelectedDisciplines(groupFlowIds)` | Ничего. Заменяет весь выбранный набор дисциплин | `POST /api/election/students/order/` |
| `election.getOrderedFlowChains()` | `List<ElectionFlowChain>`: выбранные дисциплины и дерево их потоков | `GET /api/election/students/ordered_flow_chains` |
| `election.getFlowLimits()` | `Map<String, FlowLimit>`: заполненность потоков | `GET /api/election/students/limits/flows` |
| `election.getChosenFlows()` | `List<Long>`: выбранные потоки | `GET /api/election/students/chosen_flows` |
| `election.changeSelectedFlows(flowIds)` | Ничего. Заменяет весь выбранный набор потоков | `POST /api/election/students/order/change` |
| `election.clearAllSelectedFlows()` | Ничего. Сбрасывает выбор потоков | `POST /api/election/students/order/clear` |

## Финансы: `itmo.finances`

| Метод | Возвращает | Запрос |
|---|---|---|
| `finances.getScholarshipTotals(dateFrom = null, dateTo = null)` | `List<ScholarshipTotal>`: суммы выплат по категориям, в рублях | `GET /api/finances/scholarship/total` |
| `finances.getTuitionAvailability()` | `Boolean`: есть ли договоры на платное обучение | `GET /api/finances/edupayments/availability` |
| `finances.getTuitionContracts()` | `List<TuitionContract>`; у бюджетников пусто | `GET /api/finances/edupayments/contracts` |
| `finances.getTuitionPayments()` | `List<TuitionPaymentContract>`: договоры с балансом и графиком платежей; у бюджетников пусто | `GET /api/finances/edupayments/payments` |

## Общежитие: `itmo.dormitory`

Только чтение: заселение, отмена и оплата намеренно не реализованы.

| Метод | Возвращает | Запрос |
|---|---|---|
| `dormitory.getDormitoryStatus()` | `DormitoryStatus?`: этап, место в очереди, назначенное общежитие. `null` - заявки на общежитие нет | `GET /api/dormitory/status/full` |
| `dormitory.getDormitoryDocuments()` | `List<DormitoryDocument>`: документы для заселения и их статус | `GET /api/dormitory/documents` |
| `dormitory.getDormitoryContractPeriods()` | `List<DormitoryContractPeriod>`: учебные годы, за которые есть договоры найма | `GET /api/dormitory/payments/contracts/periods` |
| `dormitory.getDormitoryContracts(dateFrom, dateTo)` | `List<DormitoryContract>`: договоры периода с балансом и графиком платежей | `GET /api/dormitory/payments/contracts` |

## Заявки: `itmo.requests`

| Метод | Возвращает | Запрос |
|---|---|---|
| `requests.getMyRequests()` | `List<RequestSummary>`: заявки пользователя | `GET /api/requests/my` |

## Портал: `itmo.system`

| Метод | Возвращает | Запрос |
|---|---|---|
| `system.getDashboard()` | `List<DashboardItem>`: сетка главного экрана | `GET /api/system/dashboard` |
| `system.getMenuItems()` | `MenuResponse`: меню с вложенными разделами | `GET /api/system/v1/menu/items` |
| `system.getServices()` | `MenuResponse`: каталог сервисов пользователя | `GET /api/system/v1/menu/services` |

## QR-пропуск: `itmo.qr`

| Метод | Возвращает | Запрос |
|---|---|---|
| `qr.getQrCode()` | `QrData`: пропуск в HEX-формате (`qrHex`) | `GET https://qr.itmo.su/v1/user/pass` |

## БАРС: `itmo.bars`

Отдельный сервис `https://bars.itmo.ru` со своей сессией, подробности - в [bars.md](bars.md). Запросы идут на
`https://bars.itmo.ru/backend/rest/`.

| Метод | Возвращает | Запрос |
|---|---|---|
| `bars.getCurrentUser()` | `User`: пользователь и выбранный на сервере период | `GET users/current_user/` |
| `bars.getConfig()` | `List<Setting>`: глобальные настройки, включая текущие год и семестр | `GET config/` |
| `bars.getDisciplines(withCheckpointPlansOnly = null)` | `List<Discipline>`: дисциплины выбранного периода | `GET journal/disciplines` |
| `bars.getGroupsAndFlows(disciplineId = null)` | `List<GroupOrFlow>`: группы и потоки периода | `GET journal/groups-and-flows` |
| `bars.getStudentJournal(checkpointPlanId, type, identifier)` | `StudentJournal`: свой журнал с баллами и оценками. Аргументы берутся из дисциплин и групп ([пример](bars.md#свои-баллы)) | `GET marks/{checkpointPlanId}/{type}/{identifier}/student` |
| `bars.selectPeriod(year, term)` | `User`. Выбирает учебный период на сервере | `POST config/personal` |
| `bars.withPeriod(year, term) { ... }` | Результат блока. Выбирает период и выполняет в нём запросы | `POST config/personal` |
| `bars.setPersonalSetting(setting)` | `Setting`: сохранённая личная настройка | `POST config/personal` |
| `bars.login(code)` | Ничего. Создаёт сессию по коду ITMO.ID ([вход без пароля](bars.md#вход-без-пароля)) | `GET login` |
| `bars.hasSession()` | `Boolean`: есть ли сохранённая сессия, без запроса в сеть | - |
| `bars.logout()` | Ничего. Очищает локальную сессию | - |
