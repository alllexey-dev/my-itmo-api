# Changelog

## 2.3.1 - 2026-10-23

- Fix `ItmoIdSessions.end` refusing base64url session ids: ITMO.ID's `sid`s are 24
  base64url characters and may contain `_`, which threw `IllegalArgumentException`
  before any request, so such a session was never ended.

## 2.3.0 - 2026-10-22

Additive: ITMO.ID sessions, as id.itmo.ru `/activity` shows and ends them.

- Add `ItmoIdClient.logout(refreshToken)`: the OIDC logout
  (`POST <issuer>/protocol/openid-connect/logout`, observed 204). It ends exactly
  the session of that refresh token; other sessions live. OAuth errors are
  `Auth`, server and network failures stay `Http`/`Network`.
  `ItmoIdConfiguration.logoutEndpoint` is its address.
- Add `TokenSet.sessionId`: the `sid` claim of the ID token (null without one),
  the id `ItmoIdSessions` uses. The signature is not checked.
- Add `ItmoIdSessions(engine, clock)` over the id.itmo.ru front's public client
  `profile`: `profileToken(cookieHeader)` gets its access token silently from a
  live SSO cookie (PKCE, no redirects followed, code checked against state and
  redirect; null for a dead cookie), `list(bearer)` reads
  `GET /api/profile/sessions` into `ItmoIdSession` (`sid`, `started`,
  `lastAccess`, `expires`, `os`, `browser`, `ipAddress`, `mobile`), and
  `end(bearer, sid)` sends `DELETE /api/account/sessions/{sid}` (204 and 404
  are success). A MyITMO bearer gets 401 there, which is `Auth`.
- Add `ItmoSessionLabel`: `browserUserAgent(platform)` (desktop Chrome 155 for
  Linux, macOS or Windows) and `botUserAgent(client, name, version, platform)`
  (`ITMO Widgets <client> <name> bot/<version> (<os>)`, which ITMO.ID shows as
  is), since the sign-in User-Agent fixes a session's label for its whole life.

## 2.2.0 - 2026-10-17

- Add `ScheduleApi.getTeacherSchedule(isu, dateStart, dateEnd)`:
  `GET /api/schedule/teacher/{isu}`, the lessons a teacher gives in the same
  `Schedule` day model as the personal schedule. An ISU that teaches nothing
  in the range answers with days without lessons; lessons at the same time for
  several groups come as separate lessons.
- Document the observed `work_type_id` 9 (graded credit) and 131 (meeting, a
  room reservation) and `flow_type_id` 9 (online lessons in teacher schedules,
  meaning unknown).

## 2.1.1 - 2026-10-17

- Add `normalizedSportLessonEnd` and `SportLesson.normalizedDateEnd` /
  `ChosenSportLesson.normalizedDateEnd`: My ITMO sends some lessons with a
  `date_end` on the wrong date (observed: a weekly series ending two days
  before it starts). An end not after the start or on another Moscow date is
  rebuilt on the start's Moscow date from the time slot end (else the raw time
  of day) and must be 15 minutes to 6 hours after the start, otherwise null.
  `dateEnd` keeps the raw value.

## 2.1.0 - 2026-10-12

Additive: existing types and the advanced setup are unchanged.

- Add `MyItmo`, a quick-start facade over `MyItmoClient` that owns its engine:
  `MyItmo.login(username, password)` (the 1.x ITMO.ID form flow with PKCE; the
  form action must be on the issuer host, credentials are never stored),
  `MyItmo.fromRefreshToken(token)` and `MyItmo.fromStorage(storage)`, every
  MyITMO area as a property, `close()`/`use`.
- `MyItmo.bars` gets and renews its BARS session from the ITMO.ID cookies of
  `MyItmo.login`, kept in that object only.
- Add `InMemoryTokenStorage` (the `MyItmo` default) and, on the JVM,
  `FileTokenStorage` (atomic, owner-only file).
- Add `Blocking.call` on the JVM to call suspend functions from Java;
  `MyItmo` factories are `@JvmStatic` with Java overloads.
- Publish ktor-client-core, kotlinx-datetime and kotlinx-serialization-json as
  `api` dependencies: their types are in the public API (engines, `LocalDate`,
  `JsonElement`), so consumers no longer add them by hand.
- README: a three-step quick start for Kotlin/JVM, Java and Android, sections on
  token storage, errors, Moscow time, BARS, advanced setup and a 1.x mapping;
  the Java and Android samples are compiled by the tests too.
- Add `SportApi.signInLesson(lessonId)` returning `SportSignResult`: `Booked`,
  `Full` (the legacy "no free places" refusal with its context), `Rejected`
  (another MyITMO refusal on HTTP 2xx, such as code 137) or `RetryLater` (no
  decision: network, non-2xx status, session or access codes, missing result).
  A default method over `signInLessons`, so existing implementations compile.

### Also since 2.0.0 (2.0.1 was never released)

- Add `LICENSE` (MIT, as declared in the POMs) and a contributor section in the
  README; `scripts/verify.sh` runs without build-slot tooling outside this
  machine. Drop internal planning IDs from KDoc and comments.
- Restructure the README after the 1.x one: features, requirements, setup,
  authentication, API usage, BARS and notes.
- Build each MyITMO request path once in the transport instead of overwriting
  it per call; request URLs are unchanged.
- Drop the test-only comparison with the 1.8.2 artifact and the generated
  member map in `docs/migration.md`; `FixtureDecodeTest` decodes every endpoint
  fixture into its 2.x model. Merge the MockEngine test helpers.
- Add `CONTRIBUTING.md` and a single fixture README; shorten `AGENTS.md`.
- Behaviour change: HTTP 403 from MyITMO or BARS is now `MyItmoException.Http(403)`,
  not `Auth`; only 401 means the session has ended (as in 1.x BARS). Consumers
  that signed the user out on 403 now keep the session.
- A `TokenManager` whose refresh is rejected re-reads storage and uses a
  snapshot another manager on the same storage rotated meanwhile, instead of
  failing with `Auth`. A shared `TokenRefreshGuard` is still required.
- Add `core.MOSCOW` (Europe/Moscow) and document that instants are shown in it
  and that HH:mm schedule strings are already Moscow time.

## 2.0.0 - 2026-10-10

- Add the separate Kotlin Multiplatform client in `kmp/` for JVM/Android and
  iOS, with suspend ITMO.ID, MyITMO and BARS APIs under `dev.alllexey.itmoapi`.
- Inject engines, time and consumer-owned atomic token/session storage. Add
  typed failures, coalesced refresh and isolated BARS cookie replay without
  password login, shared-client SSO or `obtainCodeFromSession`.
- Cover ported models/endpoints with synthetic tests comparing them with
  1.8.2, including model/member completeness and documentation checks.
- Document 2.x first in the Russian README, with compiled/executed examples,
  and provide an English migration guide with the full generated member map.
- Keep 1.x compatible in the root as `dev.alllexey:my-itmo-api`.

## 1.8.3 - unreleased

- Reject unsuccessful HTTP responses, OAuth errors and incomplete/malformed token
  payloads before updating `Storage`, for both code exchange and refresh. Refresh
  failures remain `TokenRefreshException`; transport failures retain their cause.
- Redact `TokenResponse`, `RuntimeStorage` and `RuntimeBarsStorage` string
  representations. Invalid API error JSON becomes `ApiException` without echoing
  the response body.
- Keep Java 8 compatibility with `maven.compiler.release=8`, make Lombok a
  build-only dependency and align delombok with 1.18.38. Add the Maven 3.9.11
  script-only wrapper; sign only under the owner-run `release` profile on JDK 17.
- Remove the unused shade/release plugins and the assembly plugin. The previously
  published Maven Central `jar-with-dependencies` classifier is no longer built
  or published; main, sources and Javadoc JARs remain.

## 1.8.2 - 2026-10-03

### 2026-09-30

- `BarsAuthHelper.requestCodeWithCookies(state, cookieHeader)` requests a BARS
  authorization code with ITMO.ID session cookies held by the caller (for
  example a WebView cookie store): one request, no redirects followed, the body
  is not read and the client's cookie jar is untouched. `BarsSessionCode`
  tells a code (`CODE`) from a required login (`LOGIN_REQUIRED`), a foreign or
  unusable redirect (`REJECTED`) and a server failure (`HTTP_ERROR`), and
  returns the response's `Set-Cookie` values. `obtainCodeFromSession` is
  unchanged. Released in 1.8.2.

### 2026-09-28

- Documented observed `getPersonality` response shapes and nullable fields for
  student, staff and service profiles, including numeric ISU, boolean exchange
  status, null photos and empty collections. Unknown fields remain untyped.
- Synthetic Gson and MockWebServer tests cover those shapes, the ISU path and
  Russian `Accept-Language`. The observed missing-person HTTP 400 with numeric
  `error_code=100` and explicit `result=null` remains in Retrofit's error body;
  endpoint-specific interpretation belongs to the consumer. No public API,
  model type or version change; the artifact stays 1.8.1.

## 1.8.1
- Every MyITMO request carries `Accept-Language` from
  `MyItmoConfiguration.getAcceptLanguage()` (default `ru`), so people search
  returns Cyrillic names instead of transliterations. An explicit header on a
  request wins.

## 1.8.0
- `api.bars`: BARS client with ITMO.ID login, SSO session login, external OIDC
  code exchange, silent renewal through `BarsCodeSupplier`, period selection,
  discipline and flow catalogs, student journal with marks and approvals.
- Javadoc links to Lombok getters fixed.

## 1.7.x
- Sport venue IDs documented as raw values; `building_id` nullable.
- Sport flow limits request; attendance nullability.

## 1.6.0
- Recordbook, control-event tree, sport semesters and scores as used by the
  ITMO.Widgets recordbook.
