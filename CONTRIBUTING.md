# Contributing

## Layout

| Line | Where | Artifact | Status |
|---|---|---|---|
| 1.x | repository root (`src/`, `pom.xml`) | `dev.alllexey:my-itmo-api` | Java 8, Retrofit/OkHttp/Gson; compatible fixes only |
| 2.x | `kmp/` | `dev.alllexey:my-itmo-api-kmp` | Kotlin Multiplatform (JVM/Android, iOS), Ktor; all new work |

2.x packages live under `dev.alllexey.itmoapi`: `itmoid` (ITMO.ID OAuth/PKCE and
tokens), `myitmo.<area>` (My ITMO endpoints by area), `bars` (BARS), `core`
(transport, envelopes, errors). Test fixtures are in `kmp/fixtures/`.

## Running tests

JDK 17 for both lines.

```bash
./gradlew -p kmp jvmTest                 # 2.x on JVM; includes README samples
./gradlew -p kmp iosSimulatorArm64Test   # 2.x on the iOS simulator, needs Xcode
./mvnw -B verify -Dgpg.skip=true         # 1.x
```

`scripts/verify.sh [all|maven|kmp|kmp-jvm|kmp-ios]` runs the same commands, as CI
does. Tests use injected Ktor `MockEngine`s or local servers, never the real
ITMO.ID, My ITMO or BARS.

## Adding an endpoint (2.x)

1. Model: a `@Serializable` class in `kmp/src/commonMain/.../myitmo/<area>/`
   (or `bars/model/`) with English KDoc on the class and every property:
   - the observed wire shape and what the field means;
   - units (seconds, milliseconds, rubles, points) and time zone;
   - observed enum-like values;
   - nullability: a property is nullable only if null or absence was actually
     observed; otherwise give it a non-null default (empty list, `""`, `0`).
   A field seen on the wire whose type is unknown stays a commented
   declaration, never `Any?` or `JsonElement`. Do not infer a type from a
   single sample.
2. Endpoint: a method on the area interface with KDoc (HTTP method and path,
   purpose) and `@Throws(MyItmoException::class, CancellationException::class)`;
   the implementation calls `transport.execute(serializer, method, "api/...")`.
3. Fixture: a synthetic `kmp/fixtures/<area>/<case>.json`, see
   `kmp/fixtures/README.md`, and its entry in `FixtureDecodeTest`.
4. Test: a common test using `mockExchange` that checks the method, path, query,
   body and the decoded model, plus the empty and error cases you have.
5. A line in `CHANGELOG.md`.

1.x models follow the same documentation rules with Javadoc; use wrapper types
where null was observed and keep Java 8 compatibility.

## Releases

Releases are made only by the owner, by pushing a tag on a `master` commit:

- `1.x.y` tag: `release.yml` publishes the root Maven project to Maven Central;
  the tag must equal the `pom.xml` version.
- `2.x.y` tag: `release-kmp.yml` publishes `kmp/` to Maven Central; the tag must
  equal `VERSION_NAME` in `kmp/gradle.properties`.

Both workflows then create a draft GitHub release. Pull requests never push
tags or run publishing tasks.
