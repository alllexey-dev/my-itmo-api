# Contributing

## Layout

| Path | What |
|---|---|
| `src/` | The library: Kotlin Multiplatform (JVM/Android, iOS), Ktor. Artifact `dev.alllexey:my-itmo-api` |
| `fixtures/` | Synthetic test fixtures, see `fixtures/README.md` |
| `docs/` | Library guides (Russian). Their code samples are compiled in `src/jvmTest` |
| `spec/` | Language-neutral description of the ITMO APIs: guides, service documents, OpenAPI specifications, examples |
| `scripts/verify.sh` | The checks CI runs |

Packages live under `dev.alllexey.itmoapi`: `itmoid` (ITMO.ID OAuth/PKCE and
tokens), `myitmo.<area>` (My ITMO endpoints by area), `bars` (BARS), `core`
(transport, envelopes, errors), `blocking` (JVM only: the blocking view for
Java).

The 1.x Java library was removed in 2.5.0; its last sources are in the `1.8.2`
tag.

## Running tests

JDK 17.

```bash
./gradlew jvmTest                 # JVM; includes the documentation samples
./gradlew iosSimulatorArm64Test   # iOS simulator, needs Xcode
```

`scripts/verify.sh [all|jvm|ios]` runs the same commands, as CI does. Tests use
injected Ktor `MockEngine`s or local servers, never the real ITMO.ID, My ITMO
or BARS.

Specifications and examples have their own checks:

```bash
cd spec/tools && npm ci && npm run lint && npm run examples
```

## Adding an endpoint

1. Model: a `@Serializable` class in `src/commonMain/.../myitmo/<area>/`
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
   purpose) and `@Throws(MyItmoException::class, CancellationException::class)`.
   It returns the payload, not the envelope: the implementation calls
   `transport.result(serializer, method, "api/...")` (`data` for the schedule
   envelope, `response` for external services). Use `resultOrNull` only where a
   successful null result was observed, and say in the KDoc what null means.
3. Blocking twin: the same method on the `blocking/Blocking<Area>` class;
   `BlockingParityTest` fails without it.
4. Fixture: a synthetic `fixtures/<area>/<case>.json`, see
   `fixtures/README.md`, and its entry in `FixtureDecodeTest`.
5. Test: a common test using `mockExchange` that checks the method, path, query,
   body and the decoded model, plus the empty and error cases you have.
6. A row in `docs/api.md` (`ReadmeSamplesTest` fails without it), the endpoint
   in `spec/` (the service document, and the specification once the answer has
   been checked), and a line in `CHANGELOG.md`.

## Documentation samples

Code blocks in `README.md` and `docs/*.md` that show library calls are copied
from compiled sources in `src/jvmTest` (`ReadmeSamplesTest.kt`, `readme/`),
between `// README <name> begin` and `// README <name> end`. Change the source
and the document together; the test compares them.

## Releases

Releases are made only by the owner, by pushing a `2.x.y` tag on a `master`
commit: `release.yml` publishes to Maven Central and creates a draft GitHub
release. The tag must equal `VERSION_NAME` in `gradle.properties`. Pull requests
never push tags or run publishing tasks.
