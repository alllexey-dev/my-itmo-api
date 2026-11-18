# Финансы: стипендия и оплата обучения

## Назначение

Раздел сайта my.itmo.ru "Финансы": начисления и выплаты стипендии и других выплат студенту, назначенные
стипендии, договоры и график платежей за платное обучение с онлайн-оплатой. В Android-приложении есть экран
стипендии. Зарплата сотрудников живёт в том же префиксе `/api/finances/salary/*`; это сервис сотрудников, здесь он не описывается.

## Хосты и авторизация

| Клиент | Базовый URL | Авторизация |
|---|---|---|
| Сайт my.itmo.ru | `https://my.itmo.ru/api/finances/...` | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) |
| Приложение my.itmo 4.13.0, стипендия | `https://scholarship.itmo.pro/api/v1/...` | Приложение отправляет `Authorization: Bearer` токена клиента `is-app`; `/form` вживую принял и токен my.itmo.ru |

См. [../hosts.md](../hosts.md), [../auth.md](../auth.md). Ответы обёрнуты в
`{error_code, error_message, result}` ([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

### Стипендия

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/finances/scholarship/total?dateFrom&dateTo` | Суммы начислений по категориям за период; сайт вызывает и без параметров (вероятно, за всё время) | проверено |
| GET | `/api/finances/scholarship/income` | Выплаты, по элементу на дату выплаты, с разбивкой | проверено |
| GET | `/api/finances/scholarship/account` | Назначенные стипендии: `sum`, `payment_items[] {date_start, date_end, ...}` | проверено |
| GET | `https://scholarship.itmo.pro/api/v1/income?to&from` | Выплаты за период в приложении | приложение |
| GET | `https://scholarship.itmo.pro/api/v1/account` | Назначенные стипендии в приложении | приложение |
| GET | `https://scholarship.itmo.pro/api/v1/form` | Форма обучения (бюджет или договор), ключ `budget_or_contract` | проверено |

### Оплата обучения

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/finances/edupayments/availability` | Показывать ли раздел оплаты: `result` - boolean | проверено |
| GET | `/api/finances/edupayments/contracts` | Договоры платного обучения; у бюджетника пусто | проверено |
| GET | `/api/finances/edupayments/payments` | График и история платежей; у бюджетника пусто | проверено |
| POST | `/api/finances/edupayments/payments/pay` | Создать платёжную сессию: `{contractId, sum, successUrl, failureUrl}`; `result` - URL оплаты, сайт делает redirect | сайт |

## Модели и правила

| Тема | Правило |
|---|---|
| Итоги | `ScholarshipTotal`: `category_id`, `category_name` (бывает с пробелами по краям), `sum` в рублях |
| Период итогов | `dateFrom`, `dateTo` в `YYYY-MM-DD`; сайт вызывает либо без параметров, либо за последние 6 месяцев |
| Выплата | `ScholarshipPayout`: `payment_date` (date-time), `payment.income[]` - что зачислено на счёт (`item_id`, `item_name`, `sum`, `account` - маскированный счёт, `comment`), `payment.income_details[]` - разбивка по видам начислений |
| Период выплат | Сайт получает всю историю `income` без параметров; фильтровать по дате на клиенте |
| Бюджетник | `edupayments/contracts` и `payments` возвращают пустой массив (схема допускает `null`); `availability` - `false` |
| Оплата | `payments/pay` создаёт реальную платёжную сессию банка; `sum` - число из поля ввода, `contractId` из договора |

Полные схемы: `ScholarshipTotal`, `ScholarshipPayout`, `ScholarshipPayoutItem` в
[../openapi/my-itmo.yaml](../openapi/my-itmo.yaml). Аналогичная оплата общежития описана в
[dormitory.md](dormitory.md).

## Спецификация

Основные ручки - в [../openapi/my-itmo.yaml](../openapi/my-itmo.yaml) (тег `finances`), дополнительные - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры ответов дополнительных ручек - в
[../openapi/examples/](../openapi/examples/). Форма обучения и матпомощь на бэкендах приложения - в
[../openapi/itmo-services.yaml](../openapi/itmo-services.yaml).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.finances.getScholarshipTotals` | `GET /api/finances/scholarship/total` |
| `itmo.finances.getTuitionAvailability` | `GET /api/finances/edupayments/availability` |
| `itmo.finances.getTuitionContracts` | `GET /api/finances/edupayments/contracts` |
| `itmo.finances.getTuitionPayments` | `GET /api/finances/edupayments/payments` |

Оплаты (`payments/pay`) в библиотеке нет.

**itmo-mcp** (TypeScript, [MCP-сервер](https://github.com/alllexey-dev/itmo-mcp)):

| Инструмент | Ручки |
|---|---|
| `itmo_get_scholarship` | `scholarship/total?dateFrom&dateTo` и `scholarship/income`; по умолчанию последние 365 дней, выплаты фильтруются по дате на клиенте |

`edupayments/contracts` и `payments` есть в спецификации и входят в живую проверку itmo-mcp (`verify:live`), но
инструментом не используются. Оплата не автоматизируется и не должна.

## Риски и открытые вопросы

- Форма элементов `edupayments/contracts` и `payments` не известна: у проверочного аккаунта (бюджет) они пустые,
  в спецификации это `object` без полей.
- Что возвращает `scholarship/total` без `dateFrom`/`dateTo`, не проверено.
- Форма `scholarship/account` восстановлена только по коду сайта (`sum`, `payment_items`); живой ответ пустой.
- `income` и `account` на `scholarship.itmo.pro` не вызывались (вживую снят только `/form`); их соответствие
  `/api/finances/scholarship/*` сайта предполагается, но не доказано (у приложения есть `from`/`to` у `income`, у
  сайта нет).
- `payments/pay` - деньги: никогда не вызывать автоматически; даже "проверочный" вызов создаёт платёжную сессию.
- Суммы и счета - чувствительные данные; в фикстуры и логи не сохранять.
