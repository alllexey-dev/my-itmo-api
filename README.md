<h1 align = "center">Java MyITMO API</h1>
<p align = "center">Интерфейс для сервиса MyITMO на Java</p>

## Зависимости

- `Retrofit`
- `OkHttp`
- `Gson`
- `Lombok`

## Использование

Добавьте в pom.xml:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.alllexey123</groupId>
        <artifactId>my-itmo-api</artifactId>
        <version>1.3.0</version>
    </dependency>
</dependencies>
```

### Аутентификация

* Логин через почту/ID и пароль
  ```java
  MyItmo myItmo = new MyItmo();
  myItmo.auth("my_cool_id", "my_strong_password");
  ```
* Логин через refresh_token (можно получить через F12 -> cookies в браузере)
  ```java
  MyItmo myItmo = new MyItmo();
  myItmo.refreshTokens("big_refresh_token");
  ```
* Своя реализация Storage (далее)

**Логины и пароли не сохраняются, и используются только один раз - при входе.**
Подробнее: [AuthHelper.java](/src/main/java/api/myitmo/utils/AuthHelper.java)

По умолчанию токены хранятся в памяти, рекомендуется создать свою реализацию Storage, чтобы хранить как-то иначе:

```java
MyItmo myItmo = new MyItmo();
myItmo.setStorage(customStorageImpl);
```

Время жизни refreshToken - 30 дней, accessToken - 30мин; если он устареет - токены обновятся.

### API

Методы API доступны через **MyItmo#api()** <br>
Например, получение расписания на сегодня и завтра:

```java
MyItmo myItmo = new MyItmo();
myItmo.setStorage(storageWithTokens); // или получите токены любым способом выше

LocalDate now = LocalDate.now();
MyItmoResponse<List<Schedule>> r = myItmo.api().getPersonalSchedule(now, now.plusDays(1)).execute().body();
List<Schedule> schedules = r.getData();
```

### QR

Генерировать QR-код 1-в-1 как приложение можно с помощью [io.nayuki/qrcodegen](https://central.sonatype.com/artifact/io.nayuki/qrcodegen) таким образом:

```java
String qrHex = "12345ABC";
QrSegment segment = QrSegment.makeBytes(qrHex.getBytes(StandardCharsets.ISO_8859_1));
QrCode qr = QrCode.encodeSegments(Collections.singletonList(segment), QrCode.Ecc.LOW, 1, 1, -1, false);
```