<h1 align="center">Java MyITMO API</h1>

<p align="center"><strong>Неофициальная Java-библиотека для работы с <a href="https://my.itmo.ru">MyITMO</a></strong></p>

## Возможности

- Аутентификация через ITMO ID по логину и паролю или refresh token.
- Автоматическое обновление access token.
- Получение личного учебного расписания и временных слотов.
- Получение зачётки и дерева контрольных мероприятий дисциплины.
- Получение структуры учебного плана.
- Просмотр и поиск персоналий.
- Работа со спортом:
  - расписание и фильтры;
  - личный календарь и выбранные секции;
  - запись на занятия и отмена записи;
  - баллы, попытки, задолженность и медицинская группа;
  - отборы, нормативы и специальные проекты.
- Получение раскладки главного экрана, меню и каталога сервисов MyITMO.
- Получение списка пользовательских заявок.
- Получение суммарных выплат по категориям.
- Просмотр и изменение выбора дисциплин и потоков.
- Получение QR-пропуска в корпуса в HEX-формате.
- БАРС: вход через ITMO.ID, выбор учебного периода, каталоги дисциплин и потоков,
  собственный журнал с баллами, планом контрольных точек и подтверждёнными оценками.

## Требования

- Java 8 или новее.
- Maven или другая система сборки с поддержкой Maven Central.

Основные зависимости библиотеки: Retrofit, OkHttp и Gson. Lombok нужен только
для сборки исходников библиотеки и не требуется при её использовании.

Для разработки нужен JDK 17; `scripts/verify.sh maven` запускает Maven wrapper
3.9.11, тесты и сборку JAR, исходников и Javadoc без подписи. Байткод библиотеки
остаётся совместимым с Java 8. Подпись включается только профилем `release`;
публикация выполняется владельцем по отдельному запросу.

Wrapper по умолчанию скачивает Maven в `target/maven-user-home`, а проверка
хранит зависимости в `target/verify-maven-repository`: общая папка `~/.m2`
при работе над библиотекой не изменяется.

## Подключение

Добавьте зависимость в `pom.xml`:

```xml
<dependency>
    <groupId>dev.alllexey</groupId>
    <artifactId>my-itmo-api</artifactId>
    <version>1.8.2</version>
</dependency>
```

## Аутентификация

### Логин и пароль

```java
MyItmo myItmo = new MyItmo();
myItmo.auth("my_cool_id", "my_strong_password");
```

Логин и пароль не сохраняются и используются только во время входа.

### Refresh token

```java
MyItmo myItmo = new MyItmo();
myItmo.getStorage().setRefreshToken("long_refresh_token");
myItmo.getStorage().setRefreshExpiresAt(Long.MAX_VALUE);
myItmo.forceRefreshTokens();
```

Токены по умолчанию хранятся только в памяти. Для постоянного хранения передайте собственную реализацию `Storage`:

```java
MyItmo myItmo = new MyItmo();
myItmo.setStorage(customStorage);
```

Не записывайте access token, refresh token, логин и пароль в логи или сообщения об ошибках.

Ошибочный HTTP-ответ, OAuth-ошибка или неполный ответ ITMO.ID не заменяют токены
в `Storage`. `forceRefreshTokens()` сообщает о сбое через `TokenRefreshException`,
сохраняя причину сетевой ошибки. Строковое представление токенов и хранилищ
MyITMO/БАРС не раскрывает секреты.

## Использование API

Методы доступны через `MyItmo#getApi()` и возвращают Retrofit `Call`.

### Учебное расписание

```java
MyItmo myItmo = new MyItmo();
myItmo.setStorage(storageWithTokens);

LocalDate today = LocalDate.now();
DataResponse<List<Schedule>> response = myItmo.getApi()
        .getPersonalSchedule(today, today.plusDays(1))
        .execute()
        .body();

List<Schedule> schedules = response == null ? null : response.getData();
```

### Зачётка

```java
ResultResponse<List<Specialization>> response = myItmo.getApi()
        .getSpecializations()
        .execute()
        .body();
```

Большинство методов использует `ResultResponse<T>`, где `errorCode == 0` означает успешный ответ. Старые сервисы расписания используют `DataResponse<T>` с аналогичным значением `code == 0`.

Полный перечень методов и параметров находится в [`MyItmoApi.java`](src/main/java/api/myitmo/MyItmoApi.java).

## БАРС

Клиент `api.bars.Bars` работает с `https://bars.itmo.ru` — отдельным сервисом с
собственной сессией. Токен MyITMO для него не подходит.

```java
Bars bars = new Bars();
bars.auth("my_cool_id", "my_strong_password");          // форма ITMO.ID для клиента bars

// или без пароля по SSO, если MyItmo уже вошёл через тот же OkHttpClient
MyItmo myItmo = new MyItmo();
myItmo.auth("my_cool_id", "my_strong_password");
Bars sso = new Bars(BarsConfiguration.DEFAULT, myItmo.getOkHttpClient());
sso.authWithSession();

// или из внешнего OIDC-входа (например, WebView): URL для входа и код из callback
String state = BarsAuthHelper.newState();
String loginUrl = bars.getAuthHelper().getLoginUrl(state);
String code = bars.getAuthHelper().extractCode(callbackUrl, state);
bars.login(code);
```

Сессия — заголовок `Bearer ...`, который сервер возвращает при входе. Он живёт около
30 минут, refresh token не выдаётся. Для тихого продления задайте `BarsCodeSupplier`:
при HTTP 401 клиент один раз запросит новый код и повторит запрос.

```java
bars.setCodeSupplier(state -> bars.getAuthHelper().obtainCodeFromSession(state));
bars.setStorage(customBarsStorage);                       // по умолчанию сессия только в памяти
```

Если сессия ITMO.ID живёт в чужом хранилище cookie (например, в WebView), код можно
получить без браузера: `requestCodeWithCookies` делает один запрос авторизации с
переданным заголовком `Cookie`, не выполняет переходы, не читает тело и не трогает
cookie jar клиента. Итог отличает «нужен вход» от сбоя сервера.

```java
String state = BarsAuthHelper.newState();
String loginUrl = bars.getAuthHelper().getLoginUrl(state);
String cookieHeader = browserCookies.get(loginUrl);       // "NAME=value; ..."
BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies(state, cookieHeader);
browserCookies.store(loginUrl, result.getSetCookies());  // ITMO.ID может обновить cookie сессии
switch (result.getOutcome()) {
    case CODE:           bars.login(result.getCode()); break; // обменять сразу, код одноразовый
    case LOGIN_REQUIRED: break;                           // сессии ITMO.ID нет, нужен интерактивный вход
    case REJECTED:                                        // callback с чужим state или переход не туда
    case HTTP_ERROR:     break;                           // сбой; повторить позже, getHttpCode()
}
```

`browserCookies` здесь — хранилище cookie вызывающего; `toString()` результата не
содержит кода и cookie.

Каталоги и журналы читаются в контексте периода, сохранённого на сервере; эта
настройка общая с веб-версией БАРС.

```java
List<StudentJournal> journals = bars.withPeriod("2025/2026", Term.SPRING, () -> {
    List<Discipline> disciplines = bars.execute(bars.getApi().getDisciplines(true));
    List<GroupOrFlow> flows = bars.execute(bars.getApi().getGroupsAndFlows(null));
    List<StudentJournal> result = new ArrayList<>();
    for (Discipline discipline : disciplines) {
        for (long plan : discipline.getCheckpointPlanIds()) {
            GroupOrFlow flow = flows.stream().filter(f -> f.getCheckpointPlanIds().contains(plan)).findFirst().orElse(null);
            if (flow != null) result.add(bars.execute(bars.getApi().getStudentJournal(plan, flow.getType(), flow.getIdentifier())));
        }
    }
    return result;
});
```

`StudentMarks#getTotal()` у пустого журнала равен `0.0` — проверяйте `hasAnyMark()`.
Оценка подтверждения приходит словами (`Удвл., E`); `Approval#getGradeCode()`
переводит её в формат MyITMO (`3/E`). Идентификаторы БАРС не совпадают с
`discipline_id` и `est_id` MyITMO.

## QR-пропуск

Полученный HEX можно преобразовать в QR-код, например с помощью [io.nayuki/qrcodegen](https://central.sonatype.com/artifact/io.nayuki/qrcodegen):

```java
String qrHex = "12345ABC";
QrSegment segment = QrSegment.makeBytes(qrHex.getBytes(StandardCharsets.ISO_8859_1));
QrCode qr = QrCode.encodeSegments(
        Collections.singletonList(segment),
        QrCode.Ecc.LOW,
        1,
        1,
        -1,
        false
);
```

## Особенности

MyITMO не предоставляет публичную документацию для всех используемых сервисов. Модели основаны на наблюдаемых ответах API, поэтому сервер может добавлять новые поля и справочные значения. Неизвестные, но подтверждённо присутствующие поля отмечены в моделях закомментированными объявлениями до уточнения их типов.
