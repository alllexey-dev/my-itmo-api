# Migrating from MyItmoApi 1.x to 2.x

2.x is a separate Kotlin Multiplatform artifact, not a binary-compatible update
of the Java artifact. `dev.alllexey:my-itmo-api-kmp:2.0.0` is on Maven Central;
the source in `kmp/` is at `2.0.1-SNAPSHOT`. 1.x stays in the root as `dev.alllexey:my-itmo-api`.
Targets are JVM (JVM 11 bytecode, also Android minSdk 26), iosArm64 and
iosSimulatorArm64. Source builds use JDK 17 and Kotlin language/API 2.2.

## Packages and entry points

| 1.x entry point | 2.x entry point under `dev.alllexey.itmoapi` |
|---|---|
| `api.myitmo.MyItmo` | `myitmo.MyItmoClient` |
| `api.myitmo.utils.MyItmoConfiguration` | `myitmo.MyItmoConfiguration`, `itmoid.ItmoIdConfiguration` |
| `MyItmo.getApi()` / `api.myitmo.MyItmoApi` | Separate area interfaces accessed through client properties below |
| `api.myitmo.storage.Storage` | `itmoid.TokenStorage`, `itmoid.TokenSet` |
| `api.myitmo.storage.RuntimeStorage` | Consumer-owned `TokenStorage`; no built-in persistent or runtime token storage |
| `forceRefreshTokens()` | `client.tokens.forceRefresh()` on `itmoid.TokenManager` |
| Authorization-code exchange | `client.identity.exchange(code, verifier)`, then `client.tokens.replaceTokens(snapshot)` |
| BARS client / `api.bars.BarsApi` | `bars.BarsClient` exposes suspend endpoint methods directly |
| BARS configuration/storage/code supplier | `bars.BarsConfiguration`, `BarsStorage`, `RuntimeBarsStorage`, `BarsCodeSupplier` |
| `api.bars.utils.BarsAuthHelper` | `bars.auth.BarsLogin` |
| `api.bars.utils.BarsSessionCode` | `bars.auth.BarsSessionCode`, including nested `Outcome` |
| `api.myitmo.model` response envelopes | `core.ResultResponse`, `DataResponse`, `SimpleResponse`, `CountWrapper`, `IdValuePair` |
| MyITMO area models | `myitmo.<area>`; same model names (see KDoc) |
| BARS models | `bars.model`; same model names (see KDoc) |

Do not retain mutable `setStorage`/`getStorage` setup or Retrofit/OkHttp
configuration plumbing: client configuration, storage, engine and clock are
constructor arguments in 2.x. `MyItmoConfiguration.DEFAULT` and `.DEV` select
observed official environments. Custom `baseUrl`, `itmoId`, `acceptLanguage`
(default `ru`) and `clockSkew` (30 seconds) are immutable. `ItmoIdConfiguration`
accepts issuer, public client ID and an exact HTTPS redirect URI.

## MyITMO endpoint areas

Method names generally remain unchanged, but calls move to these properties.
Every listed interface is public under `dev.alllexey.itmoapi.myitmo.<area>`.

| Client property / interface | Methods |
|---|---|
| `schedule` / `ScheduleApi` | `getPersonalSchedule`, `getTimeSlots` |
| `recordBook` / `RecordBookApi` | `getSpecializations`, `getRecordBook`, `getControlEntries` |
| `personalities` / `PersonalitiesApi` | `getPersonality`, `searchPersonalities` |
| `studyplan` / `StudyPlanApi` | `getStudyPlanPrograms`, `getStudyPlan` |
| `qr` / `QrApi` | `getQrCode` |
| `sport` / `SportApi` | `getSportTimeSlots`, `getSportFilters`, `getSportSchedule`, `getSportScore`, `getSportAttempts`, `getSportSemesters`, `getCurrentSportSemester`, `getSportSignLimits`, `getChosenSportSections`, `signInLessons`, `signOutLessons` |
| `sport` / inherited `SportRemainingApi` | `getSportTypes`, `getSportSignAttempts`, `getPersonalSportCalendar`, `getSportDebt`, `getSportExternat`, `getSportHealthLevel`, `getSportSelections`, `getSportProjects` |
| `election` / `ElectionApi` | `getElectionAvailability`, `getAvailableDisciplines`, `validateSelectedDisciplines`, `getFlowLimits`, `getOrderedFlowChains`, `getChosenFlows`, `changeSelectedFlows`, `clearAllSelectedFlows`, `changeSelectedDisciplines` |
| `finances` / `FinancesApi` | `getScholarshipTotals` |
| `requests` / `RequestsApi` | `getMyRequests` |
| `system` / `SystemApi` | `getDashboard`, `getMenuItems`, `getServices` |

`SportApi` inherits `SportRemainingApi`: these eight methods are available on
`client.sport`, not on a second client. Repeated sport filter query values keep
their order and duplicates; null/empty optional lists omit the filter.
The deprecated `getSelectedFlowChains` endpoint and recordbook `Flow`,
`FlowChain`, `FlowChainsWrapper` are not ported. This does not remove election
operations or `ElectionFlow`/`ElectionFlowChain`.

## Suspend, envelopes and errors

Replace `Call.execute()`/callbacks with suspend calls in a caller-owned coroutine.
There is no blocking facade. Public suspend operations declare
`@Throws(MyItmoException::class, CancellationException::class)` for interop;
cancellation remains cancellation and is never converted into authentication.
MyITMO methods return typed envelopes, not bare payloads. Use `core.requireResult`
to validate their error code and required payload. BARS methods return decoded
models/lists directly. `changeSelectedFlows` and `changeSelectedDisciplines`
return `ResultResponse<JsonElement?>`, not the legacy `ChangeResult` type;
`clearAllSelectedFlows` also retains the untyped result as `JsonElement?`.

| `core.MyItmoException` subtype | Meaning and consumer responsibility |
|---|---|
| `Network(cause)` | Engine I/O failure; preserve session and retry according to consumer policy. Cause is retained for classification, not logging. |
| `Http(status)` | Unsuccessful HTTP status without a recognized API error envelope. Refresh 5xx is always this subtype, never `Auth`, even with an OAuth error body. |
| `Api(status, errorCode, message)` | Recognized MyITMO error, including error envelopes with HTTP 200/400. Raw `serverMessage` is untrusted and only for explicit domain classification. |
| `Auth(status)` | Missing/expired local session, recognized OAuth rejection, or unauthenticated request (401 without a more specific API error; MyITMO retries once after a refresh). Consumer decides when to show login. 403 is `Http(403)`, as 1.x BARS treated only 401 as unauthorized. |
| `Decode` | Invalid or incomplete successful payload; preserve storage and report/retry separately from ended authentication. |

Do not clear stored credentials on `Network`, `Http` or `Decode`. ITMO.ID
`exchange`/`refresh` are stateless; `TokenManager` is the sole session writer.
All manager failures preserve the last complete snapshot, including `Auth`.
Redacted exception diagnostics do not make raw causes, server messages,
callback URLs, codes, verifier/state, tokens or cookie headers safe to log.

## Session storage, time and models

`TokenStorage.read()` returns one atomic `TokenSet?`; `write()` atomically
replaces all five fields. `null` removes the session. Supply secure persistence
in the consumer; the README's `MemoryTokens` is a single-owner sample only.
Route login/logout through `TokenManager.replaceTokens`, not parallel storage
writes. `validAccessToken()` refreshes near expiry; `forceRefresh()` handles
explicit refresh, including the consumer's missing-QR-pass retry.
`isRefreshTokenExpired()` is a local expiry check, not a network probe.
Concurrent successful refreshes coalesce within one manager. Clients/processes
sharing storage must share a `TokenRefreshGuard` whose action runs once under
an exclusive lock and releases it even on cancellation. Do not run a legacy
refresher against that storage simultaneously.
Without a shared guard, a manager whose refresh is rejected re-reads storage
and uses the snapshot another manager stored meanwhile, if its refresh token
differs; this is a fallback, not a replacement for the guard.

Token expiry uses `kotlin.time.Instant`, not epoch milliseconds; token lifetimes
from ITMO.ID are seconds converted with the injected `kotlin.time.Clock`.
Offset date-times become `kotlin.time.Instant` through `core.WireInstantSerializer`,
which normalizes serialization to UTC. Display instants in `core.MOSCOW`
(Europe/Moscow); timetable HH:mm strings are already Moscow time. Calendar dates use
`kotlinx.datetime.LocalDate`; timetable HH:mm fields remain strings. BARS
created/updated epoch values remain `Long` milliseconds. Do not blanket-convert
every date or timestamp to Instant.
Nullable properties represent observed null/absence; otherwise defaults are
non-null (for example empty collections, strings and zero numbers). Unknown
observed fields remain commented declarations, not invented public properties.
Check the KDoc instead of relying on Java wrapper nullability.

## Engines and BARS login

`core.defaultEngine()` is OkHttp on JVM/Android, Darwin on Apple. Engines are
injected and caller-owned; client/login `close()` releases initialized HTTP
clients but does not close the engine. Finish in-flight requests first, close
clients, then close the engine. A custom prebuilt engine must already disable
native cookies, caching and redirects; the client cannot sanitize it. The
default engines implement this policy; no shared cookie/cache plugin is used.

BARS has a separate complete `Authorization` header, not a MyITMO access token
and not a refresh token. `BarsStorage` stores that header; default
`RuntimeBarsStorage` is memory-only. `BarsClient.login(code)` validates and
stores it. `hasSession()` only checks presence; `logout()` clears it explicitly.
An optional suspend `BarsCodeSupplier` permits one renewal on 401 (or when
storage is empty); null means interactive authentication is needed. Transient
supplier/login failures preserve the session instead of silently logging out.

Use `BarsLogin.newState()`/`loginUrl(state)` and a browser, validate the exact
callback with `extractCode`, then call `BarsClient.login`. `isCallback` and
`isAllowedPage` expose strict HTTPS navigation checks; present `iss` must
match the configured issuer. BARS uses its observed public-client OIDC flow
without PKCE, unlike MyITMO's `Pkce` authorization-code exchange.
Password authentication, `authWithSession`, shared-client SSO and
`obtainCodeFromSession` are deliberately absent. Do not emulate them by sharing
a cookie-enabled engine. `BarsLogin.requestCodeWithCookies` replays only the
explicit caller Cookie header, makes one request without redirects/body reads,
and returns all `Set-Cookie` headers to caller-owned cookie storage:

- `BarsSessionCode.Outcome.CODE`: immediately exchange the single-use `code`.
- `LOGIN_REQUIRED`: absent/blank cookies (httpCode zero), a trusted issuer page,
  or a 2xx login page. Show interactive login.
- `REJECTED`: foreign redirect or callback/state/code/issuer validation failure.
- `HTTP_ERROR`: unsuccessful HTTP response or redirect without Location;
  preserve the session, inspect `httpCode`, and retry later.

Transport failures throw `Network`, not `LOGIN_REQUIRED`. All outcomes leave
storage decisions to the caller. Darwin normalizes Set-Cookie attribute
spelling/order while preserving semantics and Expires commas.
`BarsConfiguration` sets `restUrl`, `clientId`, `redirectUri` and issuer from
`ItmoIdConfiguration`. Period selection (`selectPeriod`/`withPeriod`) changes
the user's server settings, also used by the web UI. `withPeriod` serializes
period mutations through this client only; avoid nested period mutations.
BARS and MyITMO identifiers are not interchangeable.

## Verification

README Kotlin snippets are copied from and exercised by `ReadmeSamplesTest`
with MockEngine, injected time and synthetic issuer.invalid/callback.invalid
addresses. Run `./gradlew -p kmp jvmTest` to compile and execute them. No
real university services are involved.
