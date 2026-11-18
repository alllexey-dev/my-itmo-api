# Авторизация: ITMO.ID, БАРС, ИСУ

ITMO.ID - Keycloak, realm `itmo`. Все студенческие сервисы получают доступ через него: my.itmo.ru и мобильное
приложение - по OIDC-токенам, БАРС - по своей сессии после обмена кода, ИСУ - по cookie `KEYCLOAK_IDENTITY`.

Документ написан для того, кто пишет свой клиент: сначала что и куда отправлять, затем пошаговые потоки с точными
запросами, в конце справочные таблицы. В примерах `<...>` - значения, которые подставляете вы; токенов, паролей и
cookie в документе нет.

Два названия, одинаковые во всех документах:

- **токен my.itmo.ru** - access token ITMO.ID публичного клиента `student-personal-cabinet`. Его принимают
  `my.itmo.ru/api/*`, `qr.itmo.su` и бэкенды из [openapi/itmo-services.yaml](openapi/itmo-services.yaml).
- **cookie `KEYCLOAK_IDENTITY`** - SSO-cookie ITMO.ID, которую Keycloak ставит при входе. С ней код для любого клиента
  получается без пароля; она нужна, чтобы продлевать сессию БАРС и входить в ИСУ.

## Что нужно какому сервису

| Сервис | Что отправлять | Как получить | Сколько живёт |
|---|---|---|---|
| `my.itmo.ru/api/*`, `qr.itmo.su`, бэкенды из [openapi/itmo-services.yaml](openapi/itmo-services.yaml) | `Authorization: Bearer <access_token>`, токен my.itmo.ru | [Вход в браузере](#поток-1-вход-в-браузере-authorization-code-и-pkce), [вход по паролю](#поток-2-вход-по-логину-и-паролю-без-браузера) или [тихий вход](#поток-3-тихий-вход-по-cookie-keycloak_identity), дальше [refresh](#обновление-токенов-refresh) | access 30 минут, refresh 30 дней |
| `bars.itmo.ru/backend/rest/*` | `Authorization: <значение заголовка authorization из /login>`; значение уже начинается с `Bearer ` | [Сессия БАРС](#сессия-барс): код клиента `bars`, затем `GET /login` | Около 30 минут, без refresh |
| `isu.ifmo.ru` | Cookie APEX-сессии | Цепочка редиректов с cookie `KEYCLOAK_IDENTITY`, см. [services/isu.md](services/isu.md) | Не измерялось |
| `id.itmo.ru/api/*` (сессии аккаунта) | `Authorization: Bearer <access_token>` клиента `profile` | [Сессии аккаунта](#сессии-аккаунта-на-iditmoru) | Как обычный access token |

Токен одного клиента к чужому сервису не подходит: токен my.itmo.ru не открывает БАРС и страницы id.itmo.ru, сессия
БАРС не открывает my.itmo.ru.

## Адреса и клиенты

Issuer: `https://id.itmo.ru/auth/realms/itmo`. Пути ниже даны относительно него.

| Метод | Путь | Назначение | Статус |
|---|---|---|---|
| GET | `/protocol/openid-connect/auth` | Authorization endpoint: форма входа или сразу редирект с `code` | проверено |
| POST | `/login-actions/authenticate?...` | Отправка формы логина; точный URL берётся из страницы входа | проверено |
| POST | `/protocol/openid-connect/token` | Обмен кода и refresh; им же пользуется password grant клиента `is-app` | проверено |
| POST | `/protocol/openid-connect/logout` | Завершение сессии по refresh token | проекты |
| GET | `/protocol/openid-connect/certs` | JWKS для проверки подписи токенов: ключ RS256 для подписи и ключ RSA-OAEP для шифрования | проверено |
| GET | `/protocol/openid-connect/userinfo` | Профиль по access token; принимает и токен my.itmo.ru | проверено |

| client_id | Кто использует | redirect_uri | PKCE | scope | Статус |
|---|---|---|---|---|---|
| `student-personal-cabinet` | my.itmo.ru, my-itmo-api, itmo-mcp, ITMO.Widgets | `https://my.itmo.ru/login/callback` | S256 | `openid profile` | проверено |
| `student-personal-cabinet-dev` | dev.my.itmo.su | `https://dev.my.itmo.su/login/callback` | не проверялось | - | проекты |
| `bars` | bars.itmo.ru, my-itmo-api, itmo-mcp | `https://bars.itmo.ru/rest/login` | нет | `openid` | проверено |
| `profile` | Страницы id.itmo.ru, my-itmo-api | `https://id.itmo.ru/login/callback` | S256 | `openid profile` | проверено |
| `voting-is` | voting.itmo.ru | `https://voting.itmo.ru/login/callback` | не описано | - | проверено, см. [services/voting.md](services/voting.md) |
| `is-app` | Мобильное приложение my.itmo | не используется (password grant) | - | `openid email profile offline_access` | приложение |

- `student-personal-cabinet` - публичный клиент: код обменивается без секрета. Код клиента `bars` обменивает бэкенд
  БАРС, поэтому, публичный ли он, неизвестно и для клиента неважно.
- `is-app` - клиент официального мобильного приложения my.itmo, он входит через password grant. Ни my-itmo-api, ни
  itmo-mcp им не пользуются, запросы с ним не отправлялись, поэтому как способ входа он здесь не описан.
- В окружении Nuxt my.itmo.ru есть `ALLOWED_AZP` со значениями `student-personal-cabinet` и `isu`: сайт принимает
  токены этих двух `azp`. Клиент `isu` в источниках больше не встречается (статус: сайт).

Свой `redirect_uri` зарегистрировать нельзя, поэтому сторонний клиент всегда использует официальный callback и
перехватывает редирект на него, а не открывает его.

## Поток 1: вход в браузере (authorization code и PKCE)

Пользователь вводит пароль только на странице ITMO.ID. Подходит мобильному или десктопному приложению с WebView или
любому клиенту, который может показать страницу и увидеть, куда она перешла.

1. Сгенерируйте PKCE и `state`:
   - `code_verifier` - 43-128 символов из `A-Z a-z 0-9 - . _ ~`; my-itmo-api берёт 32 случайных байта в base64url без
     дополнения (43 символа);
   - `code_challenge` = base64url без дополнения от SHA-256 от ASCII-байтов `code_verifier`;
   - `state` - случайная строка, например UUID. Храните `code_verifier` и `state` до шага 4.
2. Откройте страницу входа:

   ```text
   https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/auth
     ?response_type=code
     &client_id=student-personal-cabinet
     &redirect_uri=https%3A%2F%2Fmy.itmo.ru%2Flogin%2Fcallback
     &scope=openid%20profile
     &state=<state>
     &code_challenge_method=S256
     &code_challenge=<code_challenge>
   ```

3. После входа ITMO.ID переводит браузер на `https://my.itmo.ru/login/callback` с параметрами `code` и `state`
   (Keycloak добавляет и другие, например `iss`). Эту навигацию нужно перехватить, а не загружать: иначе страницу
   откроет сайт и обменяет код сам.
4. Проверьте callback, прежде чем брать `code`:
   - схема `https`, хост и путь в точности равны `redirect_uri` клиента (здесь `https://my.itmo.ru/login/callback`);
   - `state` равен сохранённому;
   - параметра `error` нет;
   - `iss`, если он есть, равен issuer (отсутствие `iss` допустимо).
5. Обменяйте код на токены, см. [Обмен кода](#обмен-кода-на-токены).

Cookie `KEYCLOAK_IDENTITY` в этом потоке остаётся у браузера или WebView. Если нужны БАРС, ИСУ или тихий вход,
возьмите её из cookie браузера для `id.itmo.ru` (путь `/auth/realms/itmo/`).

Статус: authorization endpoint и обмен кода с PKCE для `student-personal-cabinet` - проверено; перехват callback в
WebView - проекты (ITMO.Widgets, my-itmo-api).

## Поток 2: вход по логину и паролю без браузера

Тот же authorization code с PKCE, только форму Keycloak отправляет ваш клиент. Подходит скриптам и серверам. Именно
так входит my-itmo-api в `MyItmo.login`
([PasswordLogin.kt](../src/commonMain/kotlin/dev/alllexey/itmoapi/itmoid/PasswordLogin.kt)), и так же входила
библиотека 1.x. Ни один запрос не должен следовать редиректам автоматически: ответы нужно читать самому.

1. `GET` на URL авторизации из потока 1 (шаги 1-2).
2. Тема ITMO.ID рисует форму из JSON-конфига в странице: URL отправки - поле `loginAction`. Фрагмент страницы
   выглядит так (значения параметров условные):

   ```text
   "loginAction": "https://id.itmo.ru/auth/realms/itmo/login-actions/authenticate?session_code=<...>&execution=<...>&client_id=student-personal-cabinet&tab_id=<...>",
   ```

   Запасной вариант - атрибут `action` у `<form>`. В найденном значении замените `&amp;` на `&`. Убедитесь, что URL
   ведёт на хост issuer (`id.itmo.ru`): пароль нельзя отправлять никуда больше. Сохраните cookie ответа:
   `AUTH_SESSION_ID`, `KC_RESTART` и другие.
3. `POST <loginAction>` с cookie из шага 2 и телом `application/x-www-form-urlencoded`:

   ```text
   username=<логин>&password=<пароль>&rememberMe=on
   ```

4. Успех - `302` с заголовком `Location` на `redirect_uri` с `code` и тем же `state`, плюс `Set-Cookie:
   KEYCLOAK_IDENTITY=...`. Проверьте `Location` по правилам шага 4 потока 1. Любой другой ответ, обычно
   снова страница входа, значит одно из трёх: неверный логин или пароль, учётная запись заблокирована или ITMO.ID
   просит дополнительный шаг (например, второй фактор). Эти случаи по ответу не различаются.
5. Обменяйте код на токены, см. [Обмен кода](#обмен-кода-на-токены).

```bash
curl -s -c cookies.txt "<URL авторизации>" -o login.html
# loginAction из login.html, затем:
curl -s -b cookies.txt -c cookies.txt -o /dev/null -D headers.txt \
  --data-urlencode "username=<логин>" --data-urlencode "password=<пароль>" --data-urlencode "rememberMe=on" \
  "<loginAction>"
# code - в заголовке Location из headers.txt
```

Что сохранить из этого потока:

- токены из обмена кода;
- cookie `KEYCLOAK_IDENTITY`, если нужны БАРС, ИСУ или тихий вход: она открывает весь аккаунт, храните её как пароль.

Статус: проверено. Этими шагами владелец проекта создаёт служебные сессии: скрипт на Node.js, который выполняет
ровно их, успешно входил на живом аккаунте 2026-10-23, 2026-11-05 и 2026-11-13. Так же форму отправляет my-itmo-api
в `MyItmo.login`. В itmo-mcp та же отправка покрыта только тестами на моках.

Каждый вход по паролю создаёт отдельную сессию ITMO.ID. Строку на странице `https://id.itmo.ru/activity` ("ОС,
Browser: браузер") определяет `User-Agent` запроса входа, и позже она не меняется: refresh и тихие входы с другим
`User-Agent` на неё не влияют (статус: проекты, `ItmoSessionLabel` в my-itmo-api).

## Поток 3: тихий вход по cookie `KEYCLOAK_IDENTITY`

Если у вас есть живая cookie `KEYCLOAK_IDENTITY`, код для любого клиента (`student-personal-cabinet`, `bars`,
`profile`, `voting-is`) получается одним запросом, без пароля.

1. `GET` на URL авторизации нужного клиента с заголовком `Cookie: KEYCLOAK_IDENTITY=<значение>`, без следования
   редиректам.
2. Если SSO-сессия жива, Keycloak сразу отвечает `302` на `redirect_uri` с `code`. Если отдаёт страницу входа (`200`)
   или редирект на другую страницу ITMO.ID, cookie истекла или её одной недостаточно: нужен вход по паролю.
3. Keycloak при каждом таком входе выдаёт в `Set-Cookie` новый `KEYCLOAK_IDENTITY` (и `KEYCLOAK_SESSION`,
   `KEYCLOAK_REMEMBER_ME`, `AUTH_SESSION_ID`, `KC_AUTH_SESSION_HASH`, `KC_RESTART`, `KEYCLOAK_LOCALE`). Хранить нужно
   последний полученный: читайте `Set-Cookie` и у неуспешных ответов.
4. Проверьте `Location` по правилам шага 4 потока 1: хост и путь должны совпасть с `redirect_uri` того клиента, для
   которого запрошен код. Затем обменяйте код; код клиента `bars` не обменивают, а отдают БАРС, см.
   [Сессия БАРС](#сессия-барс).

```bash
curl -s -o /dev/null -D headers.txt -H "Cookie: KEYCLOAK_IDENTITY=<значение>" "<URL авторизации>"
```

Сетевой сбой или ответ 5xx не значит, что cookie умерла: не удаляйте её из-за временной ошибки.

Статус: проверено для `student-personal-cabinet` и `bars`.

## Обмен кода на токены

```bash
curl -s https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/token \
  -d grant_type=authorization_code \
  -d client_id=student-personal-cabinet \
  --data-urlencode "redirect_uri=https://my.itmo.ru/login/callback" \
  -d "code=<code>" \
  -d "code_verifier=<code_verifier>"
```

Ответ (значения токенов сокращены):

```json
{
  "access_token": "<JWT>",
  "expires_in": 1800,
  "refresh_token": "<JWT>",
  "refresh_expires_in": 2592000,
  "id_token": "<JWT>"
}
```

- Кроме этих полей приходят `scope`, `session_state`, `token_type` и `not-before-policy`; клиенту они не нужны.
- `expires_in` и `refresh_expires_in` - секунды от момента ответа: запомните абсолютное время истечения сразу.
- Ошибка OAuth - JSON со строковым полем `error`: код не принят.

Статус: проверено.

## Обновление токенов (refresh)

```bash
curl -s https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/token \
  -d grant_type=refresh_token \
  -d client_id=student-personal-cabinet \
  -d "refresh_token=<refresh_token>"
```

Ответ той же формы, что при обмене кода. Правила:

- **Refresh token ротируется при каждом обновлении.** Старый после успешного ответа больше не годится; новый нужно
  сохранить сразу и целиком вместе с access token. Если новый токен не сохранён, понадобится новый вход.
- **Одно обновление за раз.** Из двух параллельных refresh с одним токеном второй может получить отказ. Сериализуйте
  обновление внутри процесса (single-flight), а если хранилище делят несколько процессов - общей блокировкой.
  my-itmo-api при отказе перечитывает хранилище: если сосед уже сохранил другой refresh token, берётся его сессия.
- **Обновляйте заранее.** my-itmo-api и itmo-mcp считают access token негодным за 30 секунд до истечения.
- **401 от API - один повтор.** Получив `401` от my.itmo.ru, обновите токен принудительно и повторите запрос один
  раз; второй `401` значит, что нужен новый вход.
- **Ошибка OAuth и сбой - разные вещи.** Ответ с полем `error` значит, что refresh token отвергнут: сессия
  закончилась, нужен вход (или тихий вход по cookie `KEYCLOAK_IDENTITY`). Сетевая ошибка, 5xx и неразборчивый
  ответ - временные: сессию из хранилища не удаляйте.

Статус: проверено.

## Вызов API с токеном

```bash
curl -s "https://my.itmo.ru/api/schedule/schedule/personal?date_start=2026-10-05&date_end=2026-10-11" \
  -H "Authorization: Bearer <access_token>" \
  -H "Accept-Language: ru"
```

- `Accept-Language` (`ru` или `en`) переводит названия и сообщения; без `ru` раздел персоналий отдаёт имена
  транслитом.
- Отправляйте токен только на `my.itmo.ru`, `qr.itmo.su` и бэкенды из спецификаций, только по HTTPS и без следования
  редиректам на другие хосты.
- HTTP 200 ещё не успех: смотрите код в конверте ответа, см. [conventions.md](conventions.md).

## Выход

`POST /protocol/openid-connect/logout` с телом `application/x-www-form-urlencoded`:

```text
client_id=student-personal-cabinet&refresh_token=<refresh_token>
```

Завершает ровно ту сессию ITMO.ID, которой принадлежит refresh token: её refresh token и cookie
`KEYCLOAK_IDENTITY` перестают работать, остальные сессии аккаунта живут. Cookie для запроса не нужна. Успех - `204`;
ответ с полем `error` - отказ, например когда сессия уже завершена. Статус: проекты (`ItmoIdClient.logout` в
my-itmo-api).

## Сессии аккаунта на id.itmo.ru

Список сессий и завершение любой из них - то, что показывает страница `https://id.itmo.ru/activity`. Нужен токен
клиента `profile`; токен my.itmo.ru получает здесь `401`.

1. Получите токен клиента `profile` тихим входом (поток 3): `client_id=profile`,
   `redirect_uri=https://id.itmo.ru/login/callback`, PKCE S256, `scope=openid profile`; затем обычный обмен кода.
2. Вызовите API с `Authorization: Bearer <токен profile>` и `Accept: application/json`:

| Метод | Путь | Что делает | Статус |
|---|---|---|---|
| GET | `https://id.itmo.ru/api/profile/sessions` | Активные сессии аккаунта: `id`, `started`, `lastAccess`, `expires` (секунды epoch), `os`, `osVersion`, `browser`, `device`, `ipAddress`, `mobile`, `current`, `clients` | проверено |
| DELETE | `https://id.itmo.ru/api/account/sessions/{sid}` | Завершает сессию; `204` и `404` (уже нет) - успех. Без `Accept: application/json` отвечает `405` | проекты |

- `id` сессии равен claim `sid` в её токенах, так свою сессию можно отличить от остальных.
- `expires` равен `started` плюс 90 дней.
- `browser` приходит строкой `family/version` или объектом `{family, version}`; `sid` бывает UUID или строкой
  base64url.
- В ответе есть IP-адреса и идентификаторы сессий: не логируйте их.

Статус: тихий вход клиента `profile`, обмен кода и список сессий проверены 2026-11-18; завершение сессии - проекты
(`ItmoIdSessions` в my-itmo-api), вживую в рамках исследования не вызывалось.

## Сессия БАРС

1. Получите `code` для клиента `bars`: URL авторизации с `response_type=code`, `scope=openid`, `client_id=bars`,
   `redirect_uri=https://bars.itmo.ru/rest/login`, `state`; PKCE нет. Код приходит входом в браузере (поток 1),
   формой (поток 2) или тихим входом (поток 3). Callback проверяется так же, как в потоке 1, только хост и путь
   сравниваются с `https://bars.itmo.ru/rest/login`.
2. Обменяйте код на сессию. Код в ITMO.ID обменивает сам бэкенд БАРС:

   ```bash
   curl -s -o /dev/null -D headers.txt \
     "https://bars.itmo.ru/backend/rest/login?code=<code>&customRedirectUri=https%3A%2F%2Fbars.itmo.ru%2Frest%2Flogin"
   ```

   Это два разных адреса, и так задумано: `redirect_uri` клиента - `https://bars.itmo.ru/rest/login` (на него приходит
   код, и он же передаётся в `customRedirectUri`), а вызов API - `https://bars.itmo.ru/backend/rest/login`.
3. Сессия - значение заголовка ответа `authorization`. Оно уже содержит префикс: `Bearer <токен сессии>`.
   Отправляйте его обратно как `Authorization` целиком, ничего не добавляя:

   ```bash
   curl -s https://bars.itmo.ru/backend/rest/users/current_user/ -H "Authorization: <значение authorization>"
   ```

   Не допустите `Bearer Bearer ...`. В [openapi/bars.yaml](openapi/bars.yaml) сессия описана как схема `http`/`bearer`,
   поэтому сгенерированный клиент сам допишет `Bearer `: ему передавайте значение без этого префикса. Клиент,
   написанный вручную, отправляет значение заголовка как есть.

- Сессия живёт около 30 минут, refresh нет, из токена my.itmo.ru её не получить. Продлевать можно только новым кодом,
  то есть тихим входом по cookie `KEYCLOAK_IDENTITY`: так делают ITMO.Widgets, my-itmo-api и itmo-mcp.
- На `401` получите новый код, войдите заново и повторите запрос один раз.
- my-itmo-api сохраняет новое значение сессии, если ответ БАРС содержит заголовок `authorization` (статус: проекты).
- Данные БАРС зависят от периода в личных настройках пользователя, см. [services/bars.md](services/bars.md).

Статус: проверено. Контракт: [openapi/bars.yaml](openapi/bars.yaml).

## Сессия ИСУ

ИСУ (Oracle APEX) пускает по cookie `KEYCLOAK_IDENTITY` через цепочку редиректов на id.itmo.ru. Подробно:
[services/isu.md](services/isu.md).

## Сроки жизни

| Что | Срок | Источник | Статус |
|---|---|---|---|
| access token | 1800 с (`expires_in`) | ITMO.Widgets, itmo-mcp | проверено |
| id token | 1800 с | ITMO.Widgets | проекты |
| refresh token | 30 дней (`refresh_expires_in` 2592000), ротируется | ITMO.Widgets, itmo-mcp | проверено |
| Сессия ITMO.ID | 90 дней от входа по паролю | my-itmo-api | проекты |
| `KEYCLOAK_IDENTITY` | около 90 дней при `rememberMe=on`, переиздаётся при каждом SSO-входе; путь `/auth/realms/itmo/`, Secure, HttpOnly | ITMO.Widgets | проекты |
| `KEYCLOAK_SESSION` | около 90 дней | ITMO.Widgets | проекты |
| `KEYCLOAK_REMEMBER_ME` | 365 дней | ITMO.Widgets | проекты |
| `AUTH_SESSION_ID`, `KC_RESTART`, `KEYCLOAK_LOCALE` | сессионные | ITMO.Widgets | проекты |
| `KC_AUTH_SESSION_HASH` | 60 с | ITMO.Widgets | проекты |
| Сессия БАРС | около 30 минут, без refresh | ITMO.Widgets, my-itmo-api | проекты |

Срок `KEYCLOAK_IDENTITY` без галочки "запомнить меня" не измерялся.

## Содержимое токенов

- access token: JWT RS256. Claims: `allowed-origins`, `auth_time`, `azp`, `email`, `exp`, `iat`, `iss`, `isu`,
  `jti`, `preferred_username`, `scope`, `sid`, `sub`, `typ`.
- `isu` - номер ИСУ пользователя (целое). Он нужен как параметр многих ручек, например профиля
  (`/api/personalities/persons/{isu}`) и брони помещений: его удобно брать из этого claim.
- `sid` - идентификатор сессии ITMO.ID, тот же, что `id` в списке сессий.
- refresh token: JWT HS512 (подписан секретом realm, проверить нельзя и не нужно).
- id token: RS256, кроме профиля содержит `isu`, `groups`, `is_student`, `sid`.

### Проверка токена на своём сервере

Как в itmo-widgets-backend (`ItmoJwtVerifier.kt`): взять JWKS из `/protocol/openid-connect/certs` (кэш ключей на
дни), найти ключ по `kid` из заголовка JWT, проверить подпись RS256 и `iss` = issuer, допуск по времени 60 с.
`azp` и `aud` там не проверяются; если важно, от какого клиента токен, `azp` стоит проверять отдельно.

## Подводные камни

- Принимаются только зарегистрированные `redirect_uri`: свой callback не подставить, поэтому сторонние клиенты
  перехватывают редирект на официальный callback.
- Device Authorization Grant выключен для `student-personal-cabinet` и `student-personal-cabinet-dev`
  (itmo-fastpick).
- У my.itmo.ru нет CORS: preflight на защищённую ручку получает 405 без `Access-Control-Allow-Origin`. Браузерному
  приложению с другого домена нужен свой серверный relay.
- Токен my.itmo.ru не подходит для БАРС и наоборот.
- Форма из потока 2 в описанном виде не проходит дополнительные шаги входа (например, второй фактор): кода в
  ответе не будет. Такому аккаунту остаётся вход в браузере (поток 1).
- Refresh token нельзя использовать из двух мест сразу: каждое обновление делает предыдущий токен негодным.
- Секреты - это пароль, `code`, `code_verifier`, все три токена, cookie `KEYCLOAK_IDENTITY` и сессия БАРС. Не пишите
  их в логи, сообщения об ошибках и URL.

## Как это сделано в существующих клиентах

### my-itmo-api (Kotlin Multiplatform, этот репозиторий)

Код: [../src/commonMain/kotlin/dev/alllexey/itmoapi/itmoid/](../src/commonMain/kotlin/dev/alllexey/itmoapi/itmoid/)
и [../src/commonMain/kotlin/dev/alllexey/itmoapi/bars/](../src/commonMain/kotlin/dev/alllexey/itmoapi/bars/).

| Класс | Роль |
|---|---|
| `Pkce` | `code_verifier`, `code_challenge`, `state` |
| `ItmoIdClient` | URL авторизации, обмен кода, refresh, выход |
| `CallbackUrl` | Строгая проверка callback: хост, путь, `state`, `iss`, отсутствие `error` |
| `PasswordLogin` | Поток 2: форма входа, хранение cookie `KEYCLOAK_IDENTITY` и остальных cookie ITMO.ID |
| `TokenManager` | Хранение, обновление заранее, single-flight, повтор после `401` |
| `ItmoIdSessions` | Токен клиента `profile`, список и завершение сессий |
| `BarsLogin`, `BarsClient` | Код клиента `bars`, тихий вход по cookie `KEYCLOAK_IDENTITY`, сессия БАРС и её продление |

Запросы библиотеки отличаются от описанных выше тремя полями, унаследованными от версии 1.x; itmo-mcp обходится
без них:

- в URL авторизации добавлен `protocol=oauth2`, как в наблюдавшемся запросе сайта;
- при обмене кода отправляется ещё `response_type=code`;
- при refresh отправляется `scopes=openid profile` (именно `scopes`).

Хранилище токенов задаёт вызывающий код (`TokenStorage`): в памяти, в файле с правами только для владельца или своё
поверх Keystore и Keychain. Как пользоваться библиотекой: [../README.md](../README.md).

### itmo-mcp (TypeScript)

Код: [src/auth](https://github.com/alllexey-dev/itmo-mcp/tree/main/src/auth). Секреты читаются из окружения:
`ITMO_USERNAME`, `ITMO_PASSWORD`, `ITMO_REFRESH_TOKEN`, `ITMO_KEYCLOAK_IDENTITY`.

| Файл | Роль |
|---|---|
| `itmo-id.ts` | Клиенты `student-personal-cabinet` (PKCE S256, `openid profile`) и `bars` (без PKCE, `openid`); authorize, форма, обмен, refresh; чтение `isu` из access token |
| `sso.ts` | Получение `code` для любого клиента: сначала `KEYCLOAK_IDENTITY` |
| `token-manager.ts` | Access token для my.itmo.ru: кэш, refresh, затем SSO |
| `bars-session.ts` | Сессия БАРС через `sso.ts` и `/login` |
| `state-store.ts` | Файл `state.json` |

Порядок получения access token для my.itmo.ru:

1. Токен в памяти, если до истечения больше 30 с.
2. Refresh token: из памяти, из `state.json`, из `ITMO_REFRESH_TOKEN` (по очереди, отвергнутый пропускается).
3. Код через SSO: `KEYCLOAK_IDENTITY` из `state.json`, затем из `ITMO_KEYCLOAK_IDENTITY`, затем логин и пароль;
   код обменивается на токены.

Подробности:

- Получение токенов и сессии БАРС - single-flight: параллельные вызовы ждут один запрос.
- На 401 от my.itmo.ru токен обновляется принудительно и запрос повторяется один раз; на 401 от БАРС сессия
  забывается и создаётся заново.
- `state.json` лежит в `ITMO_MCP_STATE_DIR` (по умолчанию `~/.config/itmo-mcp`) и хранит `refreshToken` и
  `keycloakIdentity`. Запись атомарная (временный файл и rename), права 0600, каталог 0700. Каждый новый refresh
  token и каждый переизданный `KEYCLOAK_IDENTITY` сохраняются сразу, поэтому после перезапуска вход остаётся тихим.
- Ошибки входа не содержат секретов: `AuthError` и `CredentialRejectedError` (отвергнутый credential, пробуется
  следующий).

## Открытые вопросы

- Discovery-документ `/.well-known/openid-configuration` в источниках не использовался: список grant types и
  scopes по клиентам не сверялся.
- PKCE и scope клиента `student-personal-cabinet-dev` не проверялись.
- Какие права даёт токен `is-app` на прокси my.itmo.ru и наоборот (одинаковы ли `azp`, audience и роли) - неизвестно.
- Что за клиент `isu` из `ALLOWED_AZP` и где он используется.
- Срок `KEYCLOAK_IDENTITY` без `rememberMe=on`.
- Как ITMO.ID отвечает форме при включённом втором факторе, не снималось: известно только, что кода в ответе нет.
