# Спорт (физическая культура)

## Назначение

Запись на занятия по физкультуре, баллы, попытки записи, долги, соревнования и спортивные проекты.
Доступно студентам, у которых в текущем семестре есть физкультура. Раздел сайта my.itmo.ru "Спорт" и
одноимённый экран Android-приложения.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/sport/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `/sport/students/api/v2/...` на отдельном хосте приложения | `Authorization: Bearer` токена ITMO.ID клиента `is-app` |

Сайт проксирует тот же бэкенд: пути после префикса совпадают (`/api/sport/X` на сайте и
`/sport/students/api/v2/X` в приложении). Хост `dev.*` стоит в release-сборке как значение по умолчанию;
подробнее в [../hosts.md](../hosts.md) и [../auth.md](../auth.md).

Ответы обёрнуты в `{error_code, error_message, result}`; `error_code` 0 - успех, ненулевой код при HTTP 2xx -
ошибка (общие правила в [../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

### Сайт: `/api/sport/*`

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/sport/semesters/current` | Текущий спортивный семестр и даты контрольных точек записи | проверено |
| GET | `/api/sport/semesters/list` | Список семестров для истории | проверено |
| GET | `/api/sport/personal/score` | Баллы за семестр и история начислений (`semester_id` опционален) | проверено |
| GET | `/api/sport/personal/have_attempts` | Попытки записи на занятия свободного посещения: `total_attempts`, `used_attempts`, `free_attempts`, `can_sign_in` | проверено |
| GET | `/api/sport/personal/debt` | Долг по физкультуре: `is_having_debt`, `needed_score`, `free_attempts` | проверено |
| GET | `/api/sport/personal/health_level` | Медицинская группа | проверено |
| GET | `/api/sport/personal/calendar?date_start&date_end` | Занятия, на которые записан студент, включая прошедшие | проверено |
| GET | `/api/sport/sign/schedule?date_start&date_end` | Занятия, доступные для записи (фильтры `building_id`, `sport_type_id`, `teacher_isu`) | проверено |
| GET | `/api/sport/sign/schedule/filters` | Значения фильтров: корпуса, секции, виды спорта, преподаватели | проверено |
| GET | `/api/sport/sign/chosen` | Выбранные секции, группы занятий и постоянные слоты | проверено |
| GET | `/api/sport/sport_types` | Справочник видов спорта | проверено |
| GET | `/api/sport/competitions/list` | Соревнования и статус регистрации (`sport_type_id` опционален) | проверено |
| GET | `/api/sport/competitions/list/limits` | Места по дисциплинам соревнований: `{id: {competition_id, limit, available, date_start}}` | проверено |
| POST | `/api/sport/sign/schedule/lessons` | Запись на разовые занятия свободного посещения, тело `[lessonId]` | проверено |
| DELETE | `/api/sport/sign/schedule/lessons` | Отписка от разовых занятий, тело `[lessonId]` | проверено |
| POST | `/api/sport/sign/schedule/lesson_groups/{id}` | Запись в группу на весь семестр (уровни 2-4); для открытых занятий тело с анкетой | сайт |
| DELETE | `/api/sport/sign/schedule/lesson_groups/{id}` | Выход из семестровой группы, место может уйти другому | сайт |
| GET | `/api/sport/sign/schedule/lessons/{id}/other` | Для уровня 1 - id уже записанных занятий группы, иначе `{signed}` | проверено |
| GET | `/api/sport/sign/schedule/limits` | Вместимость: `{lesson_group_id: {lesson_id: {limit, available}}}` | проверено |
| POST | `/api/sport/sign/competitions/{id}` | Регистрация на соревнование; тело `[disciplineId]` заменяет выбор | сайт |
| DELETE | `/api/sport/sign/competitions/{id}` | Отказ от соревнования | сайт |
| GET | `/api/sport/projects/list` | Спортивные проекты: `limit`, `available`, `signed`, ссылки на инструкцию | проверено |
| POST | `/api/sport/sign/projects/{id}` | Запись в проект, тело `{link}` | сайт |
| DELETE | `/api/sport/sign/projects/{id}` | Выход из проекта | сайт |
| GET | `/api/sport/personal/selections` | Отборочные секции и требования к ним | проверено |
| GET | `/api/sport/personal/open_form?section` | Сохранённая анкета открытого занятия, часто `null` | проверено |
| GET | `/api/sport/personal/open_form/ranks` | Справочник спортивных разрядов для анкеты `[{id, value}]` | проверено |
| GET | `/api/sport/personal/sign_attempts` | Число попыток, одно число | проверено |
| GET | `/api/sport/personal/externat` | Статус экстерната: `signed`, `externat_status_id`, `decline_reason` | проверено |
| GET | `/api/sport/time_slots` | Пары: `[{id, time_start, time_end}]` | проверено |
| GET | `/api/sport/personal/briefing/list` | Инструктаж по технике безопасности: файлы и признак `signed` | проверено |
| GET | `/api/sport/personal/briefing/{id}/signed` | Подписан ли инструктаж | сайт |
| POST | `/api/sport/personal/briefing` | Подписание инструктажа | сайт |
| POST | `/api/sport/briefing/{id}/sign` | Подписание инструктажа (второй вариант вызова в бандле) | сайт |

### Приложение: `/sport/students/api/v2` (отдельный бэкенд спорта)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/semesters/current` | Как на сайте | приложение |
| GET | `/personal/have_attempts` | Как на сайте | приложение |
| GET | `/personal/debt` | Как на сайте | приложение |
| GET | `/personal/health_level` | Как на сайте | приложение |
| GET | `/personal/selections` | Как на сайте | приложение |
| GET | `/personal/open_form?section` | Как на сайте | приложение |
| GET | `/personal/open_form/ranks` | Как на сайте | приложение |
| GET | `/projects/list` | Как на сайте | приложение |
| GET | `/sport_types/available` | Доступные виды спорта; на сайте используется `/sport_types` | приложение |
| GET | `/time_slots` | Как на сайте | приложение |
| GET | `/sign/schedule/limits` | Как на сайте | приложение |
| GET | `/sign/chosen/mobile` | Мобильный вариант `/sign/chosen` | приложение |
| GET | `/sign/schedule/mobile?date_start&date_end` | Мобильный вариант расписания для записи | приложение |
| GET | `/sign/schedule/filters/mobile?building_id&sport_id&level` | Мобильные фильтры, зависят от уже выбранных значений | приложение |
| POST | `/sign/schedule/lessons` | Запись на разовое занятие | приложение |
| DELETE | `/sign/schedule/lessons` | Отписка от разового занятия | приложение |
| POST | `/sign/schedule/lesson_groups/{lessonGroupId}` | Запись в семестровую группу; id дописывается к базовому URL | приложение |
| DELETE | `/sign/schedule/lesson_groups/{lessonGroupId}` | Выход из семестровой группы | приложение |

В каталоге приложения 4.13.0 не найдены баллы, календарь, соревнования, запись в проекты, экстернат и
инструктаж.

## Модели и правила

| Тема | Правило |
|---|---|
| Виды занятий | `section_level` 1 - свободное посещение, запись на одно занятие; 2-4 - группа на весь семестр (`lesson_group_id`) |
| Попытки | Свободное посещение тратит попытку: не больше 2 в неделю и `total_attempts` за семестр (`have_attempts`) |
| Должники | Занятия для должников (`type_id` 5) тратят `personal/debt.free_attempts`, не больше 2 в день |
| Пропуск | Пропущенное занятие сжигает попытку; отписка до начала возвращает её (проверено на живой записи) |
| Открытые занятия | `type_id` 1 в отборочной секции требует анкету: `{school_name, rank_id, comment, achievements, section_id, isu}` |
| `can_sign_in` | Объект `{can_sign_in, unavailable_reasons}`; не учитывает вместимость и квоты, свободные места смотреть в `available` |
| `unavailable_reasons` | Бывает массивом строк, объектом с ключами-индексами (`{"0": "..."}`) или `null` |
| Вместимость | `sign/schedule/limits` - места по группам занятий; в самих занятиях есть `limit` и `available` |
| `intersection` | Занятие пересекается с учебным расписанием; запись не блокирует, только предупреждение |
| Проекты | Запись в проект конфликтует с обычными секциями |
| Ответ записи | POST `lessons` возвращает id занятий, на которые студент записан |

Коды ошибок записи (HTTP 2xx с ненулевым `error_code`):

| Код | Значение |
|---|---|
| 137 | `cannot enroll student: ...` - дневной лимит, недельный лимит или пересечение записей |
| 130 | Студент не записан (при отписке) |
| 135 | Выбор секций ещё не открыт |
| 114 | Нет доступа |

После записи или отписки первый повторный GET иногда возвращает состояние до изменения; ITMO.Widgets
перечитывает данные через секунду (статус: проекты).

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `sport`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.sport.getSportSchedule` | `GET /api/sport/sign/schedule` |
| `itmo.sport.getSportFilters` | `GET /api/sport/sign/schedule/filters` |
| `itmo.sport.getSportSignLimits` | `GET /api/sport/sign/schedule/limits` |
| `itmo.sport.getChosenSportSections` | `GET /api/sport/sign/chosen` |
| `itmo.sport.signInLessons` | `POST /api/sport/sign/schedule/lessons` |
| `itmo.sport.signInLesson` | тот же `POST` с одним занятием; ответ разобран в исход: записан, мест нет, отказ, повторить позже |
| `itmo.sport.signOutLessons` | `DELETE /api/sport/sign/schedule/lessons` |
| `itmo.sport.getPersonalSportCalendar` | `GET /api/sport/personal/calendar` |
| `itmo.sport.getSportScore` | `GET /api/sport/personal/score` |
| `itmo.sport.getSportAttempts` | `GET /api/sport/personal/have_attempts` |
| `itmo.sport.getSportSignAttempts` | `GET /api/sport/personal/sign_attempts` |
| `itmo.sport.getSportDebt` | `GET /api/sport/personal/debt` |
| `itmo.sport.getSportHealthLevel` | `GET /api/sport/personal/health_level` |
| `itmo.sport.getSportExternat` | `GET /api/sport/personal/externat` |
| `itmo.sport.getSportSelections` | `GET /api/sport/personal/selections` |
| `itmo.sport.getSportProjects` | `GET /api/sport/projects/list` |
| `itmo.sport.getSportSemesters` | `GET /api/sport/semesters/list` |
| `itmo.sport.getCurrentSportSemester` | `GET /api/sport/semesters/current` |
| `itmo.sport.getSportTimeSlots` | `GET /api/sport/time_slots` |
| `itmo.sport.getSportTypes` | `GET /api/sport/sport_types` |

Семестровые группы, соревнования, инструктаж и подача на экстернат в библиотеке не реализованы.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_sport_status` | `semesters/current`, `personal/score`, `sign/chosen`, `personal/calendar` (14 дней), `have_attempts`, `debt`, `health_level` |
| `itmo_get_sport_points_history` | `personal/score` |
| `itmo_get_sport_schedule` | `sign/schedule` |
| `itmo_get_sport_filters` | `sign/schedule/filters`, `semesters/list` |
| `itmo_get_sport_competitions` | `competitions/list` |
| `itmo_sport_signup_preview` | Проверяет занятие и готовит POST `lessons` (уровень 1) или POST `lesson_groups/{id}` |
| `itmo_sport_cancel_preview` | Готовит DELETE `lessons` или DELETE `lesson_groups/{id}` |
| `itmo_sport_competition_preview` | `competitions/list`, `competitions/list/limits`, POST/DELETE `sign/competitions/{id}` |

Изменения выполняются только после `itmo_confirm_action`. Перед записью инструмент отказывает, если занятие
уже началось, студент уже записан, `can_sign_in` ложно, нет свободных мест или попыток (для `type_id` 5
берётся `debt.free_attempts`). Открытые занятия с анкетой не поддерживаются: инструмент отправляет на сайт.
Проекты, инструктаж и экстернат не реализованы.

## Риски и открытые вопросы

- Запись в семестровую группу, соревнования и проекты живьём не проверялись: тело и коды ошибок восстановлены
  по бандлу сайта.
- Подписание инструктажа - юридически значимое подтверждение; автоматика не должна его выполнять.
- Не ясно, чем `/sport_types/available` отличается от `/sport_types` и `sign_attempts` от `have_attempts`.
- Серверные правила лимитов (неделя календарная или скользящая, часовой пояс) не подтверждены.
- Мобильные ручки `.../mobile` могут отдавать другую форму ответа; не вызывались.
