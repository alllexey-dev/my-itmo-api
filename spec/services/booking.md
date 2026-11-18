# Бронирование помещений

## Назначение

Бронирование аудиторий, коворкингов, переговорных и залов университета. Доступно студентам и сотрудникам;
часть помещений после брони требует согласования. Раздел сайта my.itmo.ru "Бронирование" и экран
Android-приложения.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/booking/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://my.itmo.ru/api/booking/...` и `https://dev.my.itmo.su/api/booking/...` | `Authorization: Bearer` токена ITMO.ID клиента `is-app` |

Приложение ходит в тот же прокси сайта; две ручки (`users/status`, `users/event/mostPopular`) в release-сборке
по умолчанию направлены на `dev.my.itmo.su`. См. [../hosts.md](../hosts.md), [../auth.md](../auth.md).

Ответы обёрнуты в `{error_code, error_message, result}`; у бронирования `error_code` обычно `null` при успехе
(см. [../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/booking/dictionary/rooms/groups` | Группы помещений: `group_id`, `group_name`, `can_book_unconfirmed` | проверено |
| GET | `/api/booking/dictionary/rooms/categories?groupId` | Категории группы (обычно корпуса) с горизонтом брони `min_days`, `max_days` | проверено |
| GET | `/api/booking/rooms/roomsInCategory?categoryId` | Помещения категории: вместимость, этаж, адрес, оборудование, фото | проверено |
| GET | `/api/booking/rooms/roomBookings?categoryId&date&status=...` | Брони каждого помещения категории на дату; `status` повторяется | проверено |
| GET | `/api/booking/rooms/byName?search` | Поиск помещения по названию или номеру | проверено |
| GET | `/api/booking/users/status` | Телефон для предзаполнения формы: `{phone_number}` | проверено |
| GET | `/api/booking/bookings/my` | Брони, созданные студентом или где он соорганизатор (`dateStart` в приложении) | проверено |
| GET | `/api/booking/bookings/statuses` | Справочник статусов `[{status_id, status_name}]` | проверено |
| GET | `/api/booking/users/event/mostPopular?counts` | Частые названия мероприятий для подсказки в форме | проверено |
| POST | `/api/booking/bookings/` | Создать бронь: массив ровно из одного объекта | проверено |
| PATCH | `/api/booking/bookings/` | Изменить бронь владельца в статусе 5: то же тело плюс `booking_id`, `event_name` | сайт |
| DELETE | `/api/booking/bookings/{id}` | Отменить свою бронь до начала | проверено |

Приложение использует те же пути; `roomBookings` у него захардкожен со статусами `1,2,6,8`, список броней - с
`dateStart`. Отдельно в каталоге приложения: GET `https://dev.my.itmo.su/api/booking/users/status` и
GET `https://dev.my.itmo.su/api/booking/users/event/mostPopular?counts=2` (статус: приложение).

## Модели и правила

Порядок чтения: `groups` -> `categories?groupId` (горизонт `min_days..max_days` дней от сегодня) ->
`roomsInCategory?categoryId` (вместимость `min_cap..max_cap`, оборудование) -> `roomBookings?categoryId&date`
(занятость).

Статусы брони:

| id | Название | Занимает помещение | Можно отменить |
|---|---|---|---|
| 1 | Согласована | да | да |
| 2 | Отправлена | да | да |
| 3 | Отклонена | нет | нет |
| 4 | Проект | нет | да |
| 5 | На редактировании | да | да |
| 6 | На рассмотрении | да | да |
| 7 | Отменена | нет | нет |
| 8 | Выполнена | да | нет |

Сайт считает занятыми статусы 1, 2, 5, 6, 8; приложение - 1, 2, 6, 8. Новая бронь получает статус
"Отправлена", после DELETE - "Отменена" (проверено).

Тело POST (сайт, проверено):

| Поле | Значение |
|---|---|
| `name` | Название мероприятия |
| `additional_info` | Комментарий, может быть пустой строкой |
| `participants` | Число участников, в пределах вместимости помещения |
| `contact_phone` | Строго `+7 (XXX) XXX-XX-XX` |
| `event_id` | `null` |
| `co_bookers` | Сайт: массив ISU; приложение: объекты `{isu, fio}` |
| `start_datetime`, `end_datetime` | Московское время без зоны, `YYYY-MM-DD HH:mm` |
| `room_id` | Помещение |
| `equipment` | `[{equipment_id, equipment_name, count}]`, может быть пустым |
| `tech_support` | Сайт: `tech_support`; приложение: `need_tech_support` (всегда `false`) |

Правила клиента сайта: 08:00-23:00, не меньше 30 минут, не в прошлом, в горизонте категории, без пересечения
с занятыми статусами. Серверные проверки не изучены. Отмена: только владелец, статусы 1, 2, 4, 5, 6, до начала.

Персональные данные в ответах:

- `roomBookings` содержит `owner_fio` и `owner_isu` чужих броней.
- `bookings/my` содержит ФИО владельца, телефон, соорганизаторов и поля `password_for_room`,
  `password_for_virtual_room`, ссылки на виртуальную комнату.

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `booking`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/).

## Реализации

В my-itmo-api сервиса нет.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_booking_places` | `dictionary/rooms/groups`, `dictionary/rooms/categories` |
| `itmo_booking_search_rooms` | `rooms/byName` |
| `itmo_booking_availability` | `roomsInCategory`, `roomBookings` (статусы 1, 2, 5, 6, 8); чужие брони только как занятое время, имена и ISU отбрасываются |
| `itmo_get_room_bookings` | `bookings/my` |
| `itmo_booking_create_preview` | Проверяет время, горизонт, вместимость, пересечения, телефон (`users/status`), затем POST `bookings/` |
| `itmo_booking_cancel_preview` | `bookings/my`, проверки владельца, статуса и времени, затем DELETE `bookings/{id}` |

Изменения выполняются только после `itmo_confirm_action`. Соорганизаторы, оборудование, техподдержка и PATCH
не поддерживаются.

## Риски и открытые вопросы

- `roomsInCategory` иногда отвечает 500; чтения стоит повторять.
- Смысл `can_book_unconfirmed` у группы не подтверждён (вероятно, бронь без согласования).
- Расхождение тел сайта и приложения (`tech_support` и `need_tech_support`, формат `co_bookers`) не проверено
  на сервере: какой формат принимается, неизвестно.
- PATCH и правила статуса 5 восстановлены по коду сайта, живьём не вызывались.
- Серверные лимиты (число активных броней, длительность) не известны.
- Ручки приложения на `dev.my.itmo.su` могут вести на тестовый стенд; переносить их на production без
  проверки нельзя.
