# Соглашения

## Назначение

Соглашения и согласия, которые студент должен принять на my.itmo.ru (например, согласие на обработку данных):
список, версии и статус подписания. Чтение безопасно; подписание (`POST .../sign`, `sign-batch`) - юридически
значимое действие: в этот документ не входит и автоматизировать его нельзя.

## Хосты и авторизация

`https://my.itmo.ru/api/agreements/...`, `Authorization: Bearer` токена my.itmo.ru, см. [../auth.md](../auth.md).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/agreements/agreements` | Список соглашений с версиями и статусом | проверено |
| GET | `/api/agreements/agreements/{id}/` | Одно соглашение | проверено |

## Модели и правила

- Конверт `ResultResponse`, см. [../conventions.md](../conventions.md).
- Соглашение: `{id, title, code, is_bilateral, versions[]}`. Версия: `{id, version_number, date, status_id,
  status_name, date_signed, date_revoked, task_id, signature_id, sign_type}`.
- По `status_name` и `date_signed` видно, подписана ли актуальная версия.

## Спецификация

[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml), примеры - в [../openapi/examples/](../openapi/examples/).

## Реализации

В my-itmo-api и в itmo-mcp сервиса нет.

## Риски и открытые вопросы

- Подпись (`sign`, `sign-batch` с `Idempotency-Key`) не разбиралась: электронная подпись не автоматизируется.
