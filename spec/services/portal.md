# Служебные ручки портала

## Назначение

Настройки интерфейса my.itmo.ru: какие разделы и сервисы доступны пользователю и как расположены виджеты на
главной странице. Полезно, чтобы узнать, какие сервисы включены для конкретного студента.

## Хосты и авторизация

`https://my.itmo.ru/api/system/...`, `Authorization: Bearer` токена my.itmo.ru, см. [../auth.md](../auth.md).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/system/v1/menu/items` | Главное меню: `to` (маршрут), `title`, `name`, `icon`, `description` | проверено |
| GET | `/api/system/v1/menu/services` | Меню сервисов с цветом и иконкой | проверено |
| GET | `/api/system/dashboard` | Раскладка виджетов главной страницы: `i`, `x`, `y`, `w`, `h`, `widget`, `static` | проверено |

## Модели и правила

- Набор пунктов меню зависит от роли и прав пользователя: у студента без доступа к разделу пункт не приходит.
- Раскладка дашборда - формат библиотеки сетки виджетов (`min_w`, `max_h`, `is_draggable` и т.п.), большинство полей `null`.

## Спецификация

`getMenuItems`, `getMenuServices`, `getDashboard` в [../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml).

## Реализации

**my-itmo-api** (Kotlin Multiplatform, [библиотека этого репозитория](../../README.md)):

| Метод | Ручка |
|---|---|
| `itmo.system.getMenuItems` | `GET /api/system/v1/menu/items` |
| `itmo.system.getServices` | `GET /api/system/v1/menu/services` |
| `itmo.system.getDashboard` | `GET /api/system/dashboard` |

В itmo-mcp сервиса нет.

## Риски и открытые вопросы

- Полный список возможных пунктов меню и прав, от которых они зависят, не известен.
