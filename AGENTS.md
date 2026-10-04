# MyItmoApi agent guide

The ecosystem-wide rules are in the Android repository's `AGENTS.md`
(`/Users/alllexey/proj/ITMO.Widgets/AGENTS.md`). This file adds what is
specific to this library.

## Responsibilities

The only client for official MyITMO (`api.myitmo`) and BARS (`api.bars`)
endpoints used by ITMO.Widgets. When the university exposes a useful endpoint,
it is added here, never as a second Retrofit interface in the app or Backend.

1.x remains the Java client in the root (`api.myitmo`, `api.bars`,
`dev.alllexey:my-itmo-api`), with source- and binary-compatible fixes only.
2.x is the Kotlin Multiplatform client for ITMO.ID, MyITMO and BARS beside it
in `kmp/`: `dev.alllexey:my-itmo-api-kmp`, root project `my-itmo-api-kmp`,
package `dev.alllexey.itmoapi`, development version `2.0.0-SNAPSHOT`.
Password login, `obtainCodeFromSession` and shared-client SSO are not ported.

## Hard rules

### 1.x

- Every model and endpoint documents the endpoint purpose, the observed response
  shape, field semantics and units, observed enum-like values, and whether null
  or absence was actually seen. Never invent a type from one sample.
- A field known to exist but of unknown type stays as a commented declaration
  with a precise TODO, never `Object`.
- Fields are nullable only when the wire field is genuinely optional. Wrapper
  types instead of primitives where null has been observed.
- Java 8 source compatibility; Retrofit, OkHttp, Gson and Lombok only.
- Tokens, logins and passwords never appear in logs, exception messages, tests
  or documentation. The local `.refresh-token` file in the Android repository is
  for read-only exploration and is never copied or printed.
- Maven Central publication happens only on explicit request.

### 2.x

- Kotlin 2.4.20, Gradle 9.7.0, Ktor 3.6.0, kotlinx.serialization 1.11.0,
  kotlinx-datetime 0.8.0, coroutines 1.11.0 and JDK 17. Targets: JVM
  (Java 11 bytecode), `iosArm64`, `iosSimulatorArm64`. Per SP-09, set both
  language/API versions to Kotlin 2.2 and `coreLibrariesVersion = "2.2.0"`
  while the oldest JVM consumer uses Kotlin 2.2; opt into experimental time
  and UUID APIs at module scope when needed.
- `jvmMain` also serves Android (minSdk 26): no `java.net.http` or JDK 12+
  APIs; use only APIs available on Android. No blocking facade.
- Every model and endpoint has English KDoc of observed wire shapes, purpose,
  semantics, units, enum-like values and nulls. Nullable means observed null
  or absence; otherwise use non-null defaults. Unknown field types remain
  commented declarations, never `Any?`; `JsonElement` is reserved for the
  untyped `ResultResponse<?>` payload.
- Take time only from an injected `kotlin.time.Clock`. Public suspend
  operations carry `@Throws(MyItmoException::class, CancellationException::class)`.
  Exceptions and diagnostics are typed and English; consumers own localized
  UI, `DemoMode` and custom-services opt-in, with no global token/client state
  in the library.
- Preserve ADR 0012 on both networking engines: replay caller-owned ITMO.ID
  cookies without shared cookie storage, redirects or caching; return every
  `Set-Cookie` header. Codes, cookies and tokens never reach logs, exceptions,
  `toString()`, fixtures or docs. A server/network failure never clears a
  stored session; an ended session is a separate outcome.
- Production dependencies allowed: Ktor client core, content-negotiation,
  serialization-kotlinx-json, okhttp and darwin engines; kotlinx-serialization,
  kotlinx-datetime and kotlinx-coroutines. Tests may use kotlin-test,
  ktor-client-mock and coroutines-test; MockWebServer and Maven Central
  `dev.alllexey:my-itmo-api:1.8.2` are `jvmTest` dependencies only.
- All fixtures are synthetic, under `kmp/fixtures/<area>/<case>.json`.
  Tests use injected engines, never real ITMO.ID, MyITMO or BARS endpoints.
  Never open `.refresh-token`, `.env*`, `local.properties`, `*.jks` or `*.har`.
- One writer per area: port cards touch only
  `kmp/src/*/kotlin/dev/alllexey/itmoapi/<area-path>/**`, their fixtures and
  their `kmp/src/jvmTest/**/parity/<Area>ParityTest.kt`. Core, client shells,
  stubs and fixture wiring belong to their designated lane cards.
  ML-01a and ML-01b run alone in `kmp/`. Port cards never edit Gradle files:
  hand missing dependencies to the next authorized build-file card.
- Never push tags or run `mvn install`/`deploy`, `publishToMavenLocal` or any
  Central publication task. Do not write to `~/.m2`. Releases and publication
  always need the owner's word.

## v2.3 lanes

For an agent executing a v2.3 lane card, the lane rules of `ITMO.Widgets/AGENTS.md` § v2.3 lanes
apply here too; until that section exists in the app repository, this block also overrides its
Git hygiene lines 115-116 and Definition of done item 10 for lane actions in this repository.

- Lanes push only `v2.3/<lane-id>/<card-id>-<slug>` and open PRs into `v2.3/next`.
- No agent pushes a tag: any pushed tag publishes to Maven Central through `release.yml`.
  Releases (1.8.x, 2.0.0) happen only on the owner's word.
- 2.x lives in `kmp/` (artifact `my-itmo-api-kmp`, package `dev.alllexey.itmoapi`) beside 1.x;
  no `mvn install`/`deploy`, no `publishToMavenLocal`.
- Everything else in the § Forbidden and § owner-word lists of the app repository applies.

## Build

```bash
scripts/verify.sh          # Maven and KMP, once kmp/ exists
scripts/verify.sh maven    # 1.x verification, without signing
scripts/verify.sh kmp      # JVM tests, iOS klibs, simulator tests with Xcode
scripts/verify.sh kmp-jvm  # JVM tests only
scripts/verify.sh kmp-ios  # Simulator tests only; exit 2 without Xcode
```

The script uses JDK 17, the shared build slots and a Maven repository under
`target/verify-maven-repository`, never writes to `~/.m2`, and prints a final
`VERIFY M <mode> PASS|FAIL <secs>s <sha7>[+dirty]` summary. Exit 2 means a
requested toolchain or module is unavailable; other failures exit 1.

## Release lines

- 1.x is the root Maven project (`my-itmo-api`): compatibility fixes use
  `1.8.x`, with `1.*` tags. Development is `1.8.3-SNAPSHOT`.
- 2.x lives beside it in `kmp/` (`my-itmo-api-kmp`), with `2.*` tags and its
  separate release workflow once the KMP publishing card lands.
- Both lines release only from `master`. The 1.x workflow checks a stable
  semver tag, equality with the POM version and ancestry from `master` before
  deployment, then creates a draft GitHub release.
- Tags, Maven Central publication and pressing «Publish» belong to the owner.
  Never push a tag from a lane: older commits still carry the unrestricted
  release workflow.
