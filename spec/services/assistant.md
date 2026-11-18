# ИИ-ассистент ИТМО

## Назначение

Чат-ассистент на сайте my.itmo.ru: отвечает на вопросы об университете по базе знаний (workspace), ведёт сессии
диалогов, показывает источники, принимает оценку ответов. Перед первым использованием пользователь даёт согласие.
Включается флагом в Flagsmith; в Android-приложении 4.13.0 ручек ассистента не найдено.

## Хосты и авторизация

| Что | Значение |
|---|---|
| API | `https://my.itmo.ru/api/assistant/...` |
| Авторизация | `Authorization: Bearer` токена my.itmo.ru (клиент `student-personal-cabinet`) (заголовки берутся из axios сайта и для `fetch`) |
| Флаги | Flagsmith на `flagsmith.itmo.pro`; `workspace_id` - строковый флаг `my-itmo.web.ai-assistant.workspace-id` |

См. [../hosts.md](../hosts.md), [../auth.md](../auth.md). Ответы не обёрнуты в `{error_code, result}`: тело -
сразу объект, ошибки в стиле FastAPI `{detail}` ([../conventions.md](../conventions.md)).

## Ручки

Что значат статусы и чем "проверено" в основных спецификациях сильнее, чем в исследовательских:
[легенда](../README.md#статусы).

Пометка "(GET снят)" означает, что ответ получен вживую при исследовании, но со схемой не сверялся.

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `/api/assistant/auth/users/me` | Пользователь ассистента: `id`, `email`, `name`, `avatar_url`, `google_id`, `role`, `consent_given_at`, `created_at` | проверено |
| PUT | `/api/assistant/auth/users/me/consent` | Дать согласие: `{accepted: true}` | сайт |
| GET | `/api/assistant/chat/sessions?workspace_id&skip&limit` | Список сессий постранично (GET снят без `workspace_id`: `{detail: "workspace_id is required"}`) | проверено |
| GET | `/api/assistant/chat/sessions/{id}` | История сессии: `{messages[]}` | проверено |
| DELETE | `/api/assistant/chat/sessions/{id}` | Удалить сессию | сайт |
| POST | `/api/assistant/chat` | Отправить сообщение, ответ потоком SSE | сайт |
| POST | `/api/assistant/chat/messages/{id}/feedback` | Оценка ответа: `{rating: "up" или "down", category?, comment?}` | сайт |

`POST /api/assistant/chat` вызывается через `fetch`, а не axios, поэтому отсутствует в каталоге
`../data/web-api-calls.tsv`.

## Модели и правила

### Отправка сообщения

- Заголовки `Accept: text/event-stream`, `Content-Type: application/json`.
- Тело `{workspace_id, content, session_id, enable_citations}`: `session_id` - `null` для новой сессии, сайт
  отправляет `enable_citations: false`.
- Без `workspace_id` ничего не работает; значение берётся из флага Flagsmith.

### События SSE

| `type` | Смысл |
|---|---|
| `session` | `content.session_id` новой сессии |
| `message` | Очередной фрагмент текста ответа |
| `message_id` | `content.id` ответа и `content.user_message_id` вопроса; нужны для `feedback` |
| `clarification` | Уточняющий вопрос ассистента, дописывается к ответу |
| `thought` | Шаг рассуждения (`title`, `content`); показывается при включённом флаге |
| `tool_call`, `tool_result` | Вызовы инструментов; показываются при включённом флаге |
| `sources` | Источники ответа |
| `token_usage` | Расход токенов |
| `session_updated` | Сессия изменилась (например, заголовок), сайт перечитывает список |
| `error` | Ошибка генерации, код в `content` |

### Флаги Flagsmith

`my-itmo.web.ai-assistant.enabled`, `.workspace-id`, `.feedback-tags`, `.quick-actions`, `.thoughts.enabled`,
`.thoughts.tools-enabled`, `.thoughts.animation`, `.thoughts.show-after-response`, `.token-usage.enabled`,
`.token-usage.format` (по умолчанию `compact`).

Окружение Flagsmith (`FLAGSMITH_API_URL=https://flagsmith.itmo.pro`, id окружения) зашито в бандл сайта.
Проверено 2026-10-05: окружение читается без авторизации (`GET https://flagsmith.itmo.pro/api/v1/flags/` с
заголовком `X-Environment-Key`), среди флагов есть и `workspace-id`. Значение `workspace_id` в этих заметках не
приводится. Через него `GET /api/assistant/chat/sessions` отдаёт реальный список сессий (проверено).

### Прочее

- `feedback.comment` сайт обрезает до 1000 символов.
- Сайт дополнительно кэширует историю сессий у себя в браузере.

## Спецификация

`getAssistantUser`, `getAssistantSessions`, `getAssistantSession` - в
[../openapi/my-itmo-extra.yaml](../openapi/my-itmo-extra.yaml) (тег `assistant`), примеры ответов - в
[../openapi/examples/](../openapi/examples/).
Отправка сообщений и поток SSE в спецификацию не входят.

## Реализации

В my-itmo-api и в itmo-mcp сервиса нет.

## Риски и открытые вопросы

- Ни одна ручка, кроме `users/me` и пустого `sessions`, не вызывалась; формат событий SSE (поле `type`, вложенность
  `content`) восстановлен по коду сайта.
- Как получить `workspace_id` без браузера (публичный ли ключ окружения Flagsmith, один ли workspace для всех),
  не проверено.
- Что происходит без согласия (`consent_given_at` пустой), не проверено; вероятно, чат отвечает ошибкой.
- `users/me` возвращает почту и имя: персональные данные.
- Лимиты запросов и стоимость генерации не известны; автоматическая нагрузка на чат нежелательна.
