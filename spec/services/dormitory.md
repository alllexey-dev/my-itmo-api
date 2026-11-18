# Общежитие

## Назначение

Очередь на общежитие, назначенное общежитие, запись на дату заселения, медицинские документы для заселения,
договоры найма с балансом и графиком платежей, онлайн-оплата, заявка на смену категории (другое общежитие).
Раздел сайта my.itmo.ru "Сервисы -> Общежития" и экран общежития Android-приложения. Доступно студентам,
подавшим заявку на общежитие или уже проживающим.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/dormitory/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0 | `https://updaters.api.itmo.su/dormitory/v1/...` | Приложение отправляет `Authorization: Bearer` токена клиента `is-app`; `status/short` вживую принял и токен my.itmo.ru |

Пути после префикса совпадают. См. [../hosts.md](../hosts.md), [../auth.md](../auth.md). Ответы обёрнуты в
`{error_code, error_message, result}` ([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/dormitory/status/full` | Статус: этап, место в очереди, тип, назначенное общежитие с инструкцией по заселению | проверено |
| GET | `/api/dormitory/payments/contracts/periods` | Учебные годы, за которые есть договоры: `{year, dateFrom, dateTo}` | проверено |
| GET | `/api/dormitory/payments/contracts?from&to` | Договоры за период: баланс и график платежей | проверено |
| GET | `/api/dormitory/apartments` | Общежития для выбора категории: `[{id, name, address}]` | проверено |
| GET | `/api/dormitory/documents` | Медицинские документы для заселения: `[{documentType, documentTypeName, status, status_name, requestId, templateId}]` | проверено |
| GET | `/api/dormitory/settlement/slots` | Слоты заселения `[{timeSlotId, timeSlot}]` | проверено |
| POST | `/api/dormitory/settlement/register` | Записаться на дату заселения: `{timeSlotId}`; статус 2 -> 3 | сайт |
| DELETE | `/api/dormitory/settlement/remove` | Снять запись на дату, без тела; статус 3 -> 2 | сайт |
| DELETE | `/api/dormitory/settlement/cancel` | Отказаться от места и выйти из очереди, без тела | сайт |
| POST | `/api/dormitory/settlement/change_category` | Сменить категорию; без тела, вызывается после заявки-шаблона 5587 | сайт |
| POST | `/api/dormitory/payments/pay` | Платёжная сессия: `{contractId, sum, successUrl, failureUrl}`; `result` - URL оплаты | сайт |

Приложение: те же ручки на `https://updaters.api.itmo.su/dormitory/v1/...` (статус: приложение) и дополнительно
GET `status/short` - краткий статус: `statusId`, `queuePlace`, `dormType`, `assignedDormitory` (`name`, `address`).
Он вызван вживую с токеном my.itmo.ru и описан как `getDormitoryStatusShort` в
[../openapi/itmo-services.yaml](../openapi/itmo-services.yaml) (статус: проверено).

## Модели и правила

| Тема | Правило |
|---|---|
| `statusId` | 1 - в очереди; 2 - место предоставлено, нужно выбрать дату заселения в течение 24 или 48 часов; 3 - дата заселения выбрана; 4 - заселён |
| Статус | `queuePlace` (может быть `null`), `dormType` (строка-код типа), `assignedDormitory {buildingId, name, address, settlementProcedureText}`; `settlementProcedureText` - HTML; весь `result` может быть `null` |
| Договор | `contractId` (строка), `number`, `name`, `dateStart`, `dateEnd`, `active`, `balance` (рубли, отрицательный - долг), `payments[] {sum, paid, payUntil, period}` |
| Периоды | `from`, `to` для `contracts` берутся из `periods` (`dateFrom`, `dateTo`) |
| Документы | `status_name` вида "Отсутствует" или "Подтверждено"; `templateId` указывает на шаблон заявки, через которую документ загружается ([requests.md](requests.md)) |
| Смена категории | Сайт сначала шлёт `POST /api/requests/send` с `request_id` 5587: поле `13094` (`dictionary`, id выбранных общежитий через запятую) и необязательное `13095` (`text`, комментарий); при наличии `reqId` - `POST settlement/change_category` |
| Оплата | Как у оплаты обучения ([finances.md](finances.md)): сайт делает redirect на URL из `result` |

Полные схемы: `DormitoryStatus`, `DormitoryContract` в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml).

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `dormitory`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/). Краткий статус на бэкенде приложения: `getDormitoryStatusShort` в
[../openapi/itmo-services.yaml](../openapi/itmo-services.yaml).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.dormitory.getDormitoryStatus` | `GET /api/dormitory/status/full` |
| `itmo.dormitory.getDormitoryContractPeriods` | `GET /api/dormitory/payments/contracts/periods` |
| `itmo.dormitory.getDormitoryContracts` | `GET /api/dormitory/payments/contracts` |
| `itmo.dormitory.getDormitoryDocuments` | `GET /api/dormitory/documents` |

Только чтение: записи на заселение, смены категории и оплаты в библиотеке нет.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_dormitory` | `status/full`, `payments/contracts/periods`, `payments/contracts` за последний период |

Запись на заселение, отмена, смена категории и оплата в itmo-mcp не реализуются: решение владельца проекта.
`cancel`, `change_category` и `payments/pay` не автоматизировать ни при каких условиях.

## Риски и открытые вопросы

- `settlement/cancel` - потеря места в очереди, по-видимому без возврата. `payments/pay` - деньги.
- `register` и `remove` восстановлены по коду сайта; ограничения (сколько раз можно перенести дату, что будет
  после истечения 24/48 часов на этапе 2) не известны.
- Чем определяется 24 или 48 часов на этапе 2, не выяснено.
- Значения `dormType` и полный список `status` документов не задокументированы.
- Договоры содержат номера и суммы; в фикстуры и логи не сохранять.
