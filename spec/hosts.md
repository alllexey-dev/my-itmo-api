# Карта хостов

Какие хосты участвуют в студенческих сервисах ИТМО, кто к ним ходит и как они связаны с прокси `my.itmo.ru/api/*`.
Статус строки относится к самому хосту: вызывался ли он при исследовании (проверено) или известен только из клиента.

## Основные хосты

| Хост | Что обслуживает | Кто использует | Связь с `my.itmo.ru/api` | Статус |
|---|---|---|---|---|
| `my.itmo.ru` | Веб-портал (Nuxt 2) и прокси `/api/*` к бэкендам | Сайт, приложение (бронирование), my-itmo-api, itmo-mcp | Сам прокси | проверено |
| `dev.my.itmo.su` | Тестовое окружение портала, клиент `student-personal-cabinet-dev` | itmo-fastpick (DEV) | Тот же набор `/api/*` | проекты |
| `id.itmo.ru` | ITMO.ID, Keycloak realm `itmo` | Все | Токены для прокси, см. [auth.md](auth.md) | проверено |
| `bars.itmo.ru` | БАРС: SPA и REST `/backend/rest/*` | Сайт БАРС, my-itmo-api, itmo-mcp, ITMO.Widgets | Не связан, своя сессия | проверено |
| `isu.ifmo.ru` | ИСУ, Oracle APEX (`/pls/apex/f?p=...`), фото `/userpics/` | itmo-widgets-backend; ссылки с сайта и из приложения | Не связан, см. [services/isu.md](services/isu.md) | проекты |
| `qr.itmo.su` | Цифровой пропуск `GET /v1/user/pass` (`SimpleResponse`) | Приложение, my-itmo-api | Не через прокси; токен my.itmo.ru подходит | проекты |
| `photo.itmo.su` | Фотографии профилей `/avatar/<uuid>/...` | Ссылки в ответах `personalities` | Только как значение поля; сам хост не запрашивался | проверено (ссылки в ответах) |

## Бэкенды за прокси my.itmo.ru

Сайт вызывает только `https://my.itmo.ru/api/<раздел>/...`. Куда раздел уходит дальше, видно из конфигурации
модулей `@frontend/<module>` в бандле (поле `apiHost`) и из приложения, которое ходит в те же бэкенды напрямую.
Само правило проксирования на стороне сервера не видно; соответствие ниже - вывод по совпадению путей.

| Раздел на портале | Бэкенд (`apiHost` из бандла) | Подтверждение приложением | Статус |
|---|---|---|---|
| `/api/schedule/` | `https://api.schedule.itmo.su/api/v3/` | 4 шаблона, например `/api/v3/schedule/personal` = `/api/schedule/schedule/personal` | приложение |
| `/api/record_book/` | `https://recordbook.itmo.su/api/record_book` | 4 шаблона, пути совпадают | приложение |
| `/api/sport/` | `https://api.itmo.su/sport/students/api/v2/` | 16 шаблонов | приложение |
| `/api/personalities/` | `https://api.itmo.su/person/v1/` | 2 шаблона | приложение |
| `/api/requests/` | `https://applications.itmo.pro/api/v1/requests/` | 11 шаблонов | приложение |
| `/api/requests/v2/` | `https://api.itmo.su/requests/api/v1/` (ключ `requestsV2`) | нет | сайт |
| `/api/queues/` | `https://queue.itmo.pro/api/v1/queue` | 4 шаблона | приложение |
| `/api/dormitory/` | `https://updaters.api.itmo.su/dormitory/v1/` | 12 шаблонов | приложение |
| `/api/booking/` | `https://updaters.api.itmo.su/booking/api/v2/` | Приложение ходит в `my.itmo.ru/api/booking/` | сайт |
| `/api/finances/` | `https://api.itmo.su/finance/v1/`; `edupayments` - `https://updaters.api.itmo.su/edupayments/api/v1` | - | приложение |
| `/api/sign/` | `https://api.itmo.su/signature/api/v1/` | 7 шаблонов на `api.itmo.su/signature/...`, без пути-кандидата | сайт |
| `/api/eduPlan/` | `https://api.itmo.su/api/v1/public/study_plans/`, админка `https://api.itmo.su/study_plan_admin/api/` | нет | сайт |
| `/api/election/` | `http://choice-new.itmo-api:8080/choice/api/v3/` (внутреннее имя кластера) | нет | сайт |
| `/api/facultative/` | `https://faculties.choice.itmo.su/apiv2/` | нет | сайт |
| `/api/intro/` | `https://intro.choice.itmo.su/apiv2/` | нет | сайт |
| `/api/practices/` | `https://updaters.api.itmo.su/practice/v1/` | нет | сайт |
| `/api/services/adviser/` | `https://api.itmo.su/adviser/api/v2/` | нет | сайт |
| `/api/services/checklist/` | `https://checklist.itmo.su/api/v1` | Приложение: `checklist.itmo.su/api/v1/bypass` | сайт |
| `/api/agreements/` | `https://api.itmo.su/agreements/api/v1/` | нет | сайт |
| `/api/dms/` | `https://api.itmo.su/dms/api/v1/` | нет | сайт |
| `/api/constructor/`, `/api/constructor-ep/` | `https://api.itmo.su/constructor/api/v1/`, `https://api.itmo.su/constructor-ep/api/v1/` | нет | сайт |
| `/api/gia/`, `/api/gia-students/` | `https://api.itmo.su/gia/api/v1/`, `https://api.itmo.su/gia-students/api/v1/` | нет | сайт |
| `/api/individual-plan/` | `https://api.itmo.su/eeup/api/v1/` (и constructor) | нет | сайт |
| `/api/assistant/` | `https://julia.api.ai.itmo.pro` (FastAPI) | нет | сайт |
| `/api/navigator/` | Yandex Cloud API Gateway (`*.apigw.yandexcloud.net`) | нет | сайт |
| `/api/eduPlanNew/`, `/api/system/` | В конфигурации модулей не найдено | нет | сайт |

Пути на самом портале, которые вошли в [openapi/my-itmo.yaml](openapi/my-itmo.yaml), проверены на живом
аккаунте; статус в таблице описывает только соответствие бэкенду.

## Хосты мобильного приложения

Значения по умолчанию из сборки 4.13.0. Хосты со статусом "приложение" при исследовании не вызывались.

У бэкендов приложения со статусом "проверено" вживую вызывались только ручки чтения, описанные в
[openapi/itmo-services.yaml](openapi/itmo-services.yaml): они названы в скобках. Вызовы шли с токеном my.itmo.ru
(клиент `student-personal-cabinet`), и бэкенды его приняли: отдельный вход клиентом приложения `is-app` для этих
ручек не нужен. Остальные шаблоны тех же хостов известны только из приложения. Строки `my.itmo.ru` и `id.itmo.ru`
проверены сами по себе, см. таблицы выше и [auth.md](auth.md).

| Хост | Что обслуживает | Шаблонов | Пересечение с порталом | Статус |
|---|---|---|---|---|
| `api.itmo.su` | Шлюз микросервисов: `person`, `achievements` и сервисы сотрудников | 30 | `person` | проверено (`achievements`) |
| `api.schedule.itmo.su` | Расписание `/api/v3/` | 4 | `/api/schedule/` | проверено (`/api/v3/teacher/{isu}`) |
| `recordbook.itmo.su` | Зачётка `/api/record_book/` | 4 | `/api/record_book/` | проверено (`history`) |
| `applications.itmo.pro` | Электронные заявки `/api/v1/requests/` | 11 | `/api/requests/` | приложение |
| `queue.itmo.pro` | Электронные очереди `/api/v1/queue` | 4 | `/api/queues/` | приложение |
| `updaters.api.itmo.su` | Общежитие `/dormitory/v1/` | 12 | `/api/dormitory/` | проверено (`status/short`) |
| `scholarship.itmo.pro` | Стипендия `/api/v1/` | 3 | нет | проверено (`/form`) |
| `clubs.itmo.su` | Студенческие клубы `/api/v1/clubs/` | 4 | нет | проверено (все четыре GET) |
| `havchik.itmo.su` | Столовые, меню, заказы `/api/v2/canteen/` (домен не резолвится) | 6 | нет | приложение |
| `help.itmo.su` | Взаимопомощь: задачи и ответы `/api/v1/` | 9 | нет | проверено (`category`, `tasks/my`, `tasks/created`) |
| `rate.itmo.su` | Оценка занятий и событий `/api/v2/` | 6 | нет | проверено (чтение: корпуса, помещения, события, теги, свои оценки) |
| `quality.itmo.su` | Отправка оценки `/api/v2/rating` | 1 | нет | приложение |
| `kicksharing.itmo.su` | Самокаты: промокоды, маршруты, поездки `/api/v2/` (домен не резолвится) | 4 | нет | приложение |
| `api.cctv.la.itmo.su` | Загруженность помещений `/isapp/` | 2 | нет | проверено (обе ручки) |
| `actual.itmo.su` | Лента "Актуальное" `/v1/misc/relevant` | 1 | нет | проверено |
| `checklist.itmo.su` | Обходной лист `/api/v1/bypass` | 1 | `/api/services/checklist/` (по конфигу сайта) | проверено (`bypass`, ответ пустой) |
| `qr.itmo.su` | Пропуск `/v1/user/pass` | 1 | нет | проекты |
| `my.itmo.ru` | Бронирование напрямую через прокси портала | 7 | Сам прокси | проверено |
| `id.itmo.ru` | `token`, `userinfo` | 2 | - | проверено |
| `schedule.itmo.su` | Старый `userinfo` `/api/v1/userinfo` | 1 | нет | приложение |
| `api.itmostudents.ru` | Старый виджет расписания `/schedule/personal/` | 1 | нет | приложение |
| `itmostudents.ru` | Старые API: ФИО, публичные списки групп и преподавателей, матпомощь, уведомления, плашки | 10 | нет | проверено (матпомощь: основания и свои заявления; плашка) |
| `legacy.itmostudents.ru` | Старые события, партнёры, клубы | 6 | нет | приложение |
| `endpoints.itmostudents.ru` | Удалённая конфигурация URL (`?key=`) | 1 | нет | приложение |

Ещё в сборке есть `itmo-students.firebaseio.com` (Firebase) и ссылки на страницы (`student.itmo.ru`,
`helpdesk.itmo.ru`, `itmo.ru` и другие), которые API не являются.

## Вспомогательные хосты сайта

| Хост | Назначение | Статус |
|---|---|---|
| `flagsmith.itmo.pro` | Feature flags сайта (`FLAGSMITH_API_URL`); флаг `my-itmo.web.ai-assistant.workspace-id` даёт `workspace_id` ассистента | сайт |
| `widget.itmo.su` | `API_SERVER_URL` в окружении Nuxt (`https://widget.itmo.su/api/`); для чего используется, не выяснено | сайт |
| `mbtiles.itmo.su` | Тайлы карт навигатора | сайт |
| `aparts.itmo.ru`, `dms.itmo.ru`, `edu.itmo.ru` | Ссылки на внешние страницы | сайт |

## Открытые вопросы

- Правило проксирования `my.itmo.ru/api/<раздел>` -> бэкенд выведено по конфигу и совпадению путей; само правило
  (Nuxt server middleware или ingress) не видно.
- Принимают ли токен my.itmo.ru ручки записи и остальные шаблоны хостов приложения, не проверялось: вживую
  вызывалось только чтение из [openapi/itmo-services.yaml](openapi/itmo-services.yaml).
- Зачем сайту `widget.itmo.su` и куда идут `/api/eduPlanNew/` и `/api/system/`.
