# Сервисы только мобильного приложения

## Назначение

Сервисы, которые есть в Android-приложении my.itmo 4.13.0 (`ru.ifmo.itmostudents`), но не найдены в
веб-клиенте my.itmo.ru. Источник - статический разбор приложения: адреса по умолчанию и найденные HTTP-вызовы.
Ручки со статусом **приложение** не вызывались. Ручки чтения со статусом **проверено** позже вызваны вживую с
токеном my.itmo.ru и описаны в [../openapi/itmo-services.yaml](../openapi/itmo-services.yaml).

Метод `?` означает, что HTTP-вызов не прослежен (есть только URL по умолчанию); это не GET.

## Хосты и авторизация

Приложение отправляет на эти бэкенды `Authorization: Bearer` токена ITMO.ID клиента `is-app`
(scope `openid email profile offline_access`). Ручки со статусом "проверено" вживую приняли и токен my.itmo.ru
(клиент `student-personal-cabinet`), с которым работают my-itmo-api и itmo-mcp: отдельный вход клиентом `is-app`
для них не нужен. Для остальных ручек это не проверялось, и права двух токенов могут отличаться. URL по умолчанию
могут переопределяться удалённой конфигурацией (см. раздел "Удалённая конфигурация"). Карта хостов:
[../hosts.md](../hosts.md); вход: [../auth.md](../auth.md).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

### Достижения (`https://api.itmo.su/achievements/api/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/achievements/my` | Достижения пользователя и прогресс | проверено |
| GET | `/achievements/status` | Новые (непросмотренные) достижения | проверено |
| PATCH | `/achievements/status/{id}` | Отметить просмотренным; массовый вызов шлёт тело-массив id, а в URL первый id | приложение |

Поля модели: `achievement_id`, `name`, `description`, `description_short`, `icon_link`, `level`, `type_id`,
`current_progress`, `needed_progress`, `last_progress`, `started_at`, `completed_at`.

### Клубы (`https://clubs.itmo.su/api/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/clubs/types` | Типы студенческих клубов | проверено |
| GET | `/clubs/types/{typeID}` | Клубы одного типа | проверено |
| GET | `/clubs/{index}` | Карточка клуба, встречи | проверено |
| GET | `/clubs/my_clubs` | Клубы пользователя | проверено |

Поля: `name`, `name_en`, `short_description`, `long_description`, `club_type`, `vk`, `instagram`, `is_member`,
`is_owner`, `meetings` (`dt_start`, `place`, `address`, `signed`). Управление клубом открывается в WebView.
Старые ручки клубов и событий - в разделе "Legacy".

### Еда, столовые (`https://havchik.itmo.su/api/v2`)

Проверено 2026-10-05: домен не резолвится в DNS, сервис, по-видимому, выключен; адреса остались в приложении.

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| ? | `/canteen/list` | Список столовых | приложение |
| ? | `/canteen/menu/{restaurantId}` | Меню столовой | приложение |
| ? | `/canteen/order/{restaurantId}` | Новый заказ в столовой | приложение |
| ? | `/canteen/order?curr` | Заказы пользователя (`curr` - текущие) | приложение |
| ? | `/canteen/order/{orderId}` | Закрыть заказ | приложение |
| ? | `/canteen/rate` | Оценка заказа | приложение |

Найдены только URL по умолчанию; экранов и вызовов в 4.13.0 нет. Возможно, функция выключена.

### Самокаты ITMOLNIA (`https://kicksharing.itmo.su/api/v2`)

Проверено 2026-10-05: домен не резолвится в DNS, сервис, по-видимому, выключен; адреса остались в приложении.

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| ? | `/promo` | Получить промокод | приложение |
| ? | `/promo/{promocode}` | Сведения о промокоде | приложение |
| ? | `/routes` | Маршруты | приложение |
| ? | `/trips` | История поездок | приложение |

Как и у еды, есть только URL по умолчанию (ключи `itmolnia_*`).

### Загруженность помещений (`https://api.cctv.la.itmo.su/isapp`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/buildings/` | Корпуса: `id`, `humanreadable_name`. Без слэша на конце отвечает 301 | проверено |
| GET | `/rooms?building_id` | Общие места корпуса (`result.places`): `name`, `location_name`, `occupancy_rate` (проценты), `image_url` | проверено |

Данные, судя по хосту, считаются по камерам. Кандидат для read-only инструмента "где свободно посидеть".

### Помощь и задачи (`https://help.itmo.su/api/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/category` | Категории просьб `[{id, name}]` | проверено |
| GET | `/tasks` | Текущие задачи | приложение |
| POST | `/tasks` | Создать просьбу о помощи (`name`, `description`, `category`, `owner_id`) | приложение |
| GET | `/tasks/{type}` | Ответы по задаче | приложение |
| GET | `/tasks/my` | Задачи, где пользователь исполнитель | проверено |
| GET | `/tasks/created` | Задачи, созданные пользователем | проверено |
| PUT | `/tasks/close/{taskId}` | Закрыть задачу | приложение |
| POST | `/answer` | Откликнуться на задачу | приложение |
| PUT | `/answer/best/{answerId}` | Выбрать лучший ответ | приложение |
| POST | `/rate/add` | Оценить исполнителя | приложение |

Ответы содержат ФИО и контакты других студентов (`fio`, `contact`); профиль подтягивается из legacy
`itmostudents.ru/api/future/fio/{isu}`.

### Оценка занятий и событий (`https://rate.itmo.su/api/v2`, `https://quality.itmo.su/api/v2`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `rate.itmo.su /buildings` | Корпуса: `id`, `name`, `bld_type` | проверено |
| GET | `rate.itmo.su /rooms/{bldId}` | Помещения корпуса: `room_number`, `floor`, `room_type` | проверено |
| GET | `rate.itmo.su /events` | События университета для оценки: `event_id`, `name`, `date_start`, `date_end` | проверено |
| GET | `rate.itmo.su /tags/{type}` | Теги для отзыва по типу оценки | проверено |
| GET | `rate.itmo.su /rating/lessons?teacher&discipline&date&type_lesson&group` | Оценка конкретного занятия, если уже поставлена | приложение |
| GET | `rate.itmo.su /rating` | Оценки пользователя | проверено |
| POST | `rate.itmo.su /rating` | Отправить оценку занятия, события или инфраструктуры (`mark`, `emoji`, `comment`, `tags`, `photos`) | приложение |
| ? | `quality.itmo.su /rating` | Старый адрес отправки оценки (`send_rate`) | приложение |

### Актуальное (`https://actual.itmo.su/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/misc/relevant` | Лента "Актуальное": карточки, категории, страницы (`name`, `short_description`, `long_description`, `date_start`, `date_end`, `type_actual`, `pages`) | проверено |

Плашка-предупреждение над лентой берётся из legacy `itmostudents.ru/api/v1/user/plashka`.

### Обходной лист (`https://checklist.itmo.su/api/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/bypass` | Обходной лист (вероятно, при выпуске или отчислении): подразделения, решения, проблемы, контактные лица | проверено |

Поля: `departments`, `decision`, `decision_id`, `problem`, `comment`, `actions` (`action_id`, `url`),
`contact_person_name`.

### QR-пропуск (`https://qr.itmo.su/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/user/pass` | Данные QR-пропуска в корпуса: `qr_hex`, сообщение, контакты поддержки. Реализован в my-itmo-api (`itmo.qr.getQrCode`), токен my.itmo.ru подходит | проекты |

QR-код - средство физического доступа: не логировать, не сохранять и не отдавать автоматическим агентам.

### Стипендия (`https://scholarship.itmo.pro/api/v1`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/account` | Счёт для стипендии | приложение |
| GET | `/income?from&to` | Начисления за период | приложение |
| GET | `/form` | Бюджет или контракт (используется экраном матпомощи): `result.study_form`; `getStudyForm` в `itmo-services.yaml` | проверено |

На сайте те же данные доступны через проверенные `/api/finances/scholarship/*`
([../openapi/my-itmo.yaml](../openapi/my-itmo.yaml), [finances.md](finances.md)), поэтому черновик не строится.

### Legacy: itmostudents.ru, api.itmostudents.ru, schedule.itmo.su

Проверено 2026-10-05: `itmostudents.ru` отвечает (материальная помощь, плашка, ФИО), `legacy.itmostudents.ru` и
`api.itmostudents.ru/schedule`, `schedule.itmo.su/api/v1` не отвечают или требуют авторизации. Материальная помощь
(`mathelp`) - через ITMO.ID, как и остальное; описана в [../openapi/itmo-services.yaml](../openapi/itmo-services.yaml).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `itmostudents.ru /api/future/fio/{isu}` | ФИО по ISU (профиль в "Помощи") | приложение |
| ? | `itmostudents.ru /api/v1/public/edu/schedule/groups` | Список групп для расписания | приложение |
| ? | `itmostudents.ru /api/v1/public/edu/schedule/teachers` | Список преподавателей | приложение |
| ? | `itmostudents.ru /api/v1/public/edu/teachers/{teacherID}` | Карточка преподавателя | приложение |
| GET | `itmostudents.ru /api/v1/public/is/mathelp/reasons` | Основания материальной помощи: `{id, name, documents, short_name, ...}` | проверено |
| GET | `itmostudents.ru /api/v1/public/is/mathelp/applicants/me` | Профиль и заявления на матпомощь: `is_member`, `applications[]` со статусами | проверено |
| POST | `itmostudents.ru /api/v1/public/is/mathelp/applications/create` | Подать заявление (multipart, `access_token` в query) | приложение |
| DELETE | `itmostudents.ru /api/v1/public/is/mathelp/applications/{id}` | Отозвать заявление (`access_token` в query) | приложение |
| ? | `itmostudents.ru /api/v1/public/notifications/register` | Старая регистрация push-токена | приложение |
| GET | `itmostudents.ru /api/v1/user/plashka?user_id` | Плашка-предупреждение `{title, text}` | проверено |
| ? | `legacy.itmostudents.ru /events/` | Старый список событий | проверено (не отвечает) |
| ? | `legacy.itmostudents.ru /events/{index}/?username` | Карточка события | приложение |
| ? | `legacy.itmostudents.ru /events/register/` | Регистрация на событие | приложение |
| ? | `legacy.itmostudents.ru /partners/`, `/partners/{index}/` | Партнёры и скидки | приложение |
| ? | `legacy.itmostudents.ru /v2/clubs/introduction/{userID}` | Данные "введения" (`intro_point`), смысл не выяснен | приложение |
| ? | `api.itmostudents.ru /schedule/personal/` | Старое личное расписание для виджета | проверено (не отвечает) |
| ? | `schedule.itmo.su /api/v1/userinfo` | Старый профиль пользователя | приложение |

Токен в query (`access_token`) - устаревшая схема; не повторять и не логировать такие URL.

### Удалённая конфигурация (`https://endpoints.itmostudents.ru?key=`)

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| ? | `/?key=` | Переопределение URL ручек по ключу (`Endpoints`, `EndpointReflector`, `setEndpoint`) | приложение |

Значения по умолчанию из этого документа может заменить сервер; реальные адреса в работающем приложении могут
отличаться.

## Модели и правила

- Все ответы и тела восстановлены по именам полей в AOT-коде, без типов и обязательности.
- Ключи приложения (`achievements_my`, `help_tasks` и т. п.) стабильны внутри сборки и используются как
  `operationId` в черновиках.
- Изменяющие вызовы: PATCH достижений (отметка просмотра), все POST и PUT в "Помощи", POST оценки, заказы еды,
  промокоды самокатов, регистрация устройств, заявления на матпомощь.

## Спецификация

Проверенные ручки клубов, загруженности, событий, "Актуального", достижений, помощи, обходного листа
и оценки описаны в [../openapi/itmo-services.yaml](../openapi/itmo-services.yaml), примеры
ответов - в [../openapi/examples/](../openapi/examples/).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.qr.getQrCode` | `GET https://qr.itmo.su/v1/user/pass` |

Остальных сервисов этого документа в библиотеке нет.

В itmo-mcp этих сервисов нет.

## Риски и открытые вопросы

- Запись (PATCH достижений, POST и PUT "Помощи", отправка оценок, заявления на матпомощь) вживую не вызывалась:
  принимают ли эти ручки токен my.itmo.ru, не известно.
- Хосты `*.dev.*` и `dev.*` в release-сборке: возможно, это тестовые стенды или реальный прод с таким именем.
- Еда и самокаты без прослеженных вызовов: функции могут быть выключены или удалены на сервере.
- Методы с `?` не установлены; в черновиках они помечены `x-method-unknown` и описаны как GET условно.
- QR-пропуск, заявления на матпомощь, "Помощь" и оценки преподавателей затрагивают доступ в здания, деньги,
  данные других людей и репутацию: автоматизировать только чтение и только с явного запроса.
