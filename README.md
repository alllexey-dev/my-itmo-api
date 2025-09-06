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
        <version>1.1.2</version>
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
myItmo.

setStorage(customStorageImpl);
```

Время жизни refreshToken - 30 дней, accessToken - 30мин; если он устареет - токены обновятся.

### API

Методы API доступны через **MyItmo#api()** <br>
Например, получение расписания на сегодня и завтра:

```java
MyItmo myItmo = new MyItmo();
myItmo.

setStorage(storageWithTokens); // или получите токены любым способом выше

LocalDate now = LocalDate.now();
MyItmoResponse<List<Schedule>> r = myItmo.api().getPersonalSchedule(now, now.plusDays(1)).execute().body();
List<Schedule> schedules = r.getData();
```
