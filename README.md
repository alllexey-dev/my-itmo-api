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

## Требования

- Java 8 или новее.
- Maven или другая система сборки с поддержкой Maven Central.

Основные зависимости библиотеки: Retrofit, OkHttp, Gson и Lombok.

## Подключение

Добавьте зависимость в `pom.xml`:

```xml
<dependency>
    <groupId>dev.alllexey</groupId>
    <artifactId>my-itmo-api</artifactId>
    <version>1.7.0</version>
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
