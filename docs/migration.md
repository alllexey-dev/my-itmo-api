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
| MyITMO area models | `myitmo.<area>`; complete model/property mappings below |
| BARS models | `bars.model`; complete mappings below |

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
| `Auth(status)` | Missing/expired local session, recognized OAuth rejection, or unauthenticated request (401/403 without a more specific API error). Consumer decides when to show login. |
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

Token expiry uses `kotlin.time.Instant`, not epoch milliseconds; token lifetimes
from ITMO.ID are seconds converted with the injected `kotlin.time.Clock`.
Offset date-times become `kotlin.time.Instant` through `core.WireInstantSerializer`,
which normalizes serialization to UTC. Calendar dates use
`kotlinx.datetime.LocalDate`; timetable HH:mm fields remain strings. BARS
created/updated epoch values remain `Long` milliseconds. Do not blanket-convert
every date or timestamp to Instant.
Nullable properties represent observed null/absence; otherwise defaults are
non-null (for example empty collections, strings and zero numbers). Unknown
observed fields remain commented declarations, not invented public properties.
Check the map and KDoc instead of relying on Java wrapper nullability.

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

## Verification and complete generated member map

README Kotlin snippets are copied from and exercised by `ReadmeSamplesTest`
with MockEngine, injected time and synthetic issuer.invalid/callback.invalid
addresses. Run `scripts/verify.sh kmp-jvm` to compile/execute them and the
merged ML-09b completeness suite. No real university services are involved.
The suite checks 84 legacy model source files against pinned Central 1.8.2,
including inherited/nested fields, serialized names, unknown-type observations
and per-model KDoc counts. The full output below is copied verbatim from this
checkout's `kmp/build/parity/member-map.md`; build output is not committed.
It is a model/member map, not a claim that deprecated endpoints were ported.

<!-- Generated member map begin -->
# 1.x to 2.x member map

Generated from the checked-out source snapshot.

## src/main/java/api/bars/model/Approval.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/Approval.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| studentId | student_id | studentId |
| studentLogin | student_login | studentLogin |
| checkpointPlanId | checkpoint_plan_id | checkpointPlanId |
| attempt | attempt | attempt |
| marksSum | marks_sum | marksSum |
| markString | mark_string | markString |
| active | is_active | active |
| invalid | is_invalid | invalid |
| absent | is_absent | absent |
| recalculated | was_recalculated | recalculated |
| course | course | course |
| createdAt | created_at | createdAt |
| updatedAt | updated_at | updatedAt |

## src/main/java/api/bars/model/Checkpoint.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/Checkpoint.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| gid | gid | gid |
| name | name | name |
| type | type | type |
| typeId | type_id | typeId |
| week | week | week |
| group | group | group |
| key | key | key |
| minGrade | min_grade | minGrade |
| maxGrade | max_grade | maxGrade |
| subCheckpoints | sub_checkpoints | subCheckpoints |
| parentCheckpointId | parent_checkpoint_id | parentCheckpointId |

## src/main/java/api/bars/model/CheckpointPlan.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/CheckpointPlan.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| gid | gid | gid |
| year | year | year |
| terms | terms | terms |
| discipline | discipline | discipline |
| regularCheckpoints | regular_checkpoints | regularCheckpoints |
| finalCheckpoint | final_checkpoint | finalCheckpoint |
| pointDistribution | point_distribution | pointDistribution |
| additionalPoints | additional_points | additionalPoints |
| courseProject | has_course_project | courseProject |

## src/main/java/api/bars/model/Discipline.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/Discipline.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| terms | terms | terms |
| checkpointPlanIds | checkpoint_plan_ids | checkpointPlanIds |

## src/main/java/api/bars/model/GroupOrFlow.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/GroupOrFlow.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| type | type | type |
| name | name | name |
| identifier | identifier | identifier |
| checkpointPlanIds | checkpoint_plan_ids | checkpointPlanIds |

## src/main/java/api/bars/model/JournalHeaders.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/JournalHeaders.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| plan | plan | plan |
| type | type | type |
| identifier | identifier | identifier |
| name | name | name |

## src/main/java/api/bars/model/Mark.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/Mark.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| checkpointId | checkpoint_id | checkpointId |
| checkpointPlanId | checkpoint_plan_id | checkpointPlanId |
| mark | mark | mark |
| type | type | type |
| absent | is_absent | absent |
| notBiggerThanMax | is_not_bigger_than_max | notBiggerThanMax |
| createdAt | created_at | createdAt |
| updatedAt | updated_at | updatedAt |
| createdByName | created_by_name | createdByName |
| updatedByName | updated_by_name | updatedByName |

## src/main/java/api/bars/model/PlanDiscipline.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/PlanDiscipline.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| courseProject | course_project | courseProject |

## src/main/java/api/bars/model/Setting.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/Setting.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| value | value | value |

## src/main/java/api/bars/model/StudentAccessibility.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/StudentAccessibility.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| canEditCurrentMarks | can_edit_current_marks | canEditCurrentMarks |
| canEditAdditionalMarks | can_edit_additional_marks | canEditAdditionalMarks |
| canEditCourseMarks | can_edit_course_marks | canEditCourseMarks |
| canEditFinalMarks | can_edit_final_marks | canEditFinalMarks |
| canApproveMarks | can_approve_marks | canApproveMarks |
| canApproveRetryMarks | can_approve_retry_marks | canApproveRetryMarks |
| hasUnfilledKeyCheckpoints | has_unfilled_key_checkpoints | hasUnfilledKeyCheckpoints |
| courseProjectThemeNotApproved | course_project_theme_not_approved | courseProjectThemeNotApproved |

## src/main/java/api/bars/model/StudentJournal.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/StudentJournal.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| students | students | students |
| headers | headers | headers |

## src/main/java/api/bars/model/StudentMarks.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/StudentMarks.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| regular | regular | regular |
| finalMark | final | finalMark |
| additional | additional | additional |
| regularSum | regularSum | regularSum |
| total | total | total |
| activeApprovals | active_approvals | activeApprovals |

## src/main/java/api/bars/model/StudentRecord.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/StudentRecord.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| studentId | student_id | studentId |
| studentLogin | student_login | studentLogin |
| studentName | student_name | studentName |
| marks | marks | marks |
| accessibility | accessibility | accessibility |
| wantsToIncreaseMarks | wants_to_increase_marks | wantsToIncreaseMarks |

## src/main/java/api/bars/model/Term.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/Term.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| wireValue | wireValue | wireValue |

## src/main/java/api/bars/model/User.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/User.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| login | login | login |
| firstName | first_name | firstName |
| middleName | middle_name | middleName |
| lastName | last_name | lastName |
| userRoles | user_roles | userRoles |
| selectedRole | selected_role | selectedRole |
| selectedYear | selected_year | selectedYear |
| selectedTerm | selected_term | selectedTerm |
| personalConfig | personal_config | personalConfig |
| canChangeUser | can_change_user | canChangeUser |
| restrictedToHaveReadOnlyAccess | restricted_to_have_read_only_access | restrictedToHaveReadOnlyAccess |

## src/main/java/api/bars/model/UserRole.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/bars/model/UserRole.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| locked | locked | locked |
| selected | selected | selected |
| allowsWhiteList | allows_white_list | allowsWhiteList |
| allowsMultiple | allows_multiple | allowsMultiple |

## src/main/java/api/myitmo/model/CountWrapper.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/core/Envelopes.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| count | count | count |
| data | data | data |

## src/main/java/api/myitmo/model/DataResponse.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/core/Envelopes.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| code | code | code |
| data | data | data |
| message | message | message |

## src/main/java/api/myitmo/model/IdValuePair.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/core/Envelopes.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| value | value | value |

## src/main/java/api/myitmo/model/ResultResponse.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/core/Envelopes.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| errorCode | error_code | errorCode |
| errorMessage | error_message | errorMessage |
| result | result | result |

## src/main/java/api/myitmo/model/SimpleResponse.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/core/Envelopes.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| response | response | response |

## src/main/java/api/myitmo/model/election/AvailableDiscipline.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/AvailableDiscipline.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| dcId | dcId | dcId |
| discId | discId | discId |
| langId | langId | langId |
| depName | depName | depName |
| discName | discName | discName |
| langCode | langCode | langCode |
| required | required | required |
| semesters | semesters | semesters |
| description | description | description |
| depNameShort | depNameShort | depNameShort |
| notCompatibleWith | notCompatibleWith | notCompatibleWith |

## src/main/java/api/myitmo/model/election/AvailableDisciplineSemester.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/AvailableDisciplineSemester.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| semester | semester | semester |
| statusId | statusId | statusId |
| groupFlow | groupFlow | groupFlow |
| statusName | statusName | statusName |

## src/main/java/api/myitmo/model/election/ChangeResult.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/ChangeResult.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| status | status | status |
| name | name | name |
| flows | flows | flows |

## src/main/java/api/myitmo/model/election/DisciplineSelectionValidation.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/DisciplineSelectionValidation.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| disciplines | disciplines | disciplines |
| validVariantSelection | validVariantSelection | validVariantSelection |
| validRequiredSelection | validRequiredSelection | validRequiredSelection |
| needSelectRequired | needSelectRequired | needSelectRequired |
| needSelectVariants | needSelectVariants | needSelectVariants |
| needSelectVariantsMax | needSelectVariantsMax | needSelectVariantsMax |
| scheduleAvailable | scheduleAvailable | scheduleAvailable |

## src/main/java/api/myitmo/model/election/ElectionAvailability.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/ElectionAvailability.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| status | status | status |
| semesterStart | semesterStart | semesterStart |
| semesterEnd | semesterEnd | semesterEnd |
| dateStart | dateStart | dateStart |
| dateEnd | dateEnd | dateEnd |
| timeStart | timeStart | timeStart |
| timeEnd | timeEnd | timeEnd |
| studyYear | studyYear | studyYear |
| semesterId | semesterId | semesterId |
| semester | semester | semester |

## src/main/java/api/myitmo/model/election/ElectionFlow.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/ElectionFlow.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| limitMax | limitMax | limitMax |
| teachers | teachers | teachers |
| variants | variants | variants |
| workType | workType | workType |
| available | available | available |
| selections | selections | selections |

## src/main/java/api/myitmo/model/election/ElectionFlowChain.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/ElectionFlowChain.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| groupFlow | groupFlow | groupFlow |
| disciplineId | disciplineId | disciplineId |
| disciplineName | disciplineName | disciplineName |
| flows | flows | flows |

## src/main/java/api/myitmo/model/election/FlowLimit.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/election/FlowLimit.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| limitMax | limitMax | limitMax |
| occupied | occupied | occupied |
| free | free | free |

## src/main/java/api/myitmo/model/finance/ScholarshipTotal.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/finances/ScholarshipTotal.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| categoryId | category_id | categoryId |
| categoryName | category_name | categoryName |
| sum | sum | sum |

## src/main/java/api/myitmo/model/other/QrData.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/qr/QrData.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| qrHex | qr_hex | qrHex |

## src/main/java/api/myitmo/model/other/TokenResponse.java

Renamed: src/commonMain/kotlin/dev/alllexey/itmoapi/itmoid/TokenSet.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| accessToken | access_token | accessToken |
| expiresIn | expires_in | accessExpiresAt (seconds converted to Instant using injected Clock) |
| refreshToken | refresh_token | refreshToken |
| refreshExpiresIn | refresh_expires_in | refreshExpiresAt (seconds converted to Instant using injected Clock) |
| idToken | id_token | idToken |
| sessionState | session_state | Not retained: ML-04a five-field Storage contract; ItmoIdClient.TokenWire documents ignored session_state |

## src/main/java/api/myitmo/model/personality/Contact.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/personalities/Contact.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| contact | contact | contact |
| contactAlias | contact_alias | contactAlias |

## src/main/java/api/myitmo/model/personality/Education.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/personalities/Education.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| course | course | course |
| facultyName | faculty_name | facultyName |
| group | group | group |

## src/main/java/api/myitmo/model/personality/Personality.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/personalities/Personality.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| isu | isu | isu |
| fio | fio | fio |
| gender | gender | gender |
| photoUrl | photo | photoUrl |
| contacts | contacts | contacts |
| rooms | rooms | rooms |
| positions | positions | positions |
| education | education | education |
| exchangeTraining | exchange_training | exchangeTraining |
Observed unknown-type declarations (not public members): powers, levels, activities. Retained as comments, not invented wire types.

## src/main/java/api/myitmo/model/personality/PersonalityMin.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/personalities/PersonalityMin.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| fio | fio | fio |
| gender | gender | gender |
| phone | phone | phone |
| email | email | email |
| work | work | work |
| photoUrl | photo | photoUrl |
Observed unknown-type declarations (not public members): education. Retained as comments, not invented wire types.

## src/main/java/api/myitmo/model/personality/Position.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/personalities/Position.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| departmentName | department_name | departmentName |
| departmentLink | department_link | departmentLink |
| positionName | position_name | positionName |
Observed unknown-type declarations (not public members): vacation, startVacation, endVacation. Retained as comments, not invented wire types.

## src/main/java/api/myitmo/model/personality/Room.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/personalities/Room.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| roomNumber | room_number | roomNumber |
| bldName | bld_name | bldName |

## src/main/java/api/myitmo/model/recordbook/ControlEntry.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/recordbook/ControlEntry.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| controlName | control_name | controlName |
| parentId | parent_id | parentId |
| lowerValue | lower_value | lowerValue |
| maxValue | max_value | maxValue |
| minValue | min_value | minValue |
| required | required | required |
| rate | rate | rate |
| date | date | date |
| teacher | teacher | teacher |

## src/main/java/api/myitmo/model/recordbook/Flow.java

Not ported: deprecated Flow API excluded by the L20 exit policy.

## src/main/java/api/myitmo/model/recordbook/FlowChain.java

Not ported: deprecated Flow API excluded by the L20 exit policy.

## src/main/java/api/myitmo/model/recordbook/FlowChainsWrapper.java

Not ported: deprecated Flow API excluded by the L20 exit policy.

## src/main/java/api/myitmo/model/recordbook/RecordBookEntry.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/recordbook/RecordBookEntry.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| name | name | name |
| disciplineId | discipline_id | disciplineId |
| estId | est_id | estId |
| currentScore | current_score | currentScore |
| rate | rate | rate |
| attempt | attempt | attempt |
| controlType | control_type | controlType |
| controlTypeId | control_type_id | controlTypeId |
| examDate | exam_date | examDate |
| haveTree | have_tree | haveTree |
| lmsLink | lms_link | lmsLink |
| teacher | teacher | teacher |

## src/main/java/api/myitmo/model/recordbook/RecordBookTeacher.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/recordbook/RecordBookTeacher.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| surname | surname | surname |
| name | name | name |
| patronymic | patronymic | patronymic |

## src/main/java/api/myitmo/model/recordbook/Semester.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/recordbook/Semester.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| studyYear | study_year | studyYear |
| semester | semester | semester |
| course | course | course |
| actual | actual | actual |

## src/main/java/api/myitmo/model/recordbook/Specialization.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/recordbook/Specialization.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| mainPlan | main_plan | mainPlan |
| specializationName | specialization_name | specializationName |
| semesters | semesters | semesters |

## src/main/java/api/myitmo/model/requests/RequestSummary.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/requests/RequestSummary.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| notice | notice | notice |
| status | status | status |
| statusName | status_name | statusName |
| createdAt | created_at | createdAt |
| updatedAt | updated_at | updatedAt |

## src/main/java/api/myitmo/model/schedule/ExtendedTimeSlot.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/schedule/ExtendedTimeSlot.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| order | order | order |
| id | id | id |
| timeStart | time_start | timeStart |
| timeEnd | time_end | timeEnd |
Inherited members: see the TimeSlot mapping; 2.x may flatten their declarations.

## src/main/java/api/myitmo/model/schedule/Lesson.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/schedule/Lesson.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| pairId | pair_id | pairId |
| subject | subject | subject |
| subjectId | subject_id | subjectId |
| note | note | note |
| type | type | type |
| timeStart | time_start | timeStart |
| timeEnd | time_end | timeEnd |
| teacherId | teacher_id | teacherId |
| teacherName | teacher_name | teacherName |
| room | room | room |
| building | building | building |
| format | format | format |
| workType | work_type | workType |
| workTypeId | work_type_id | workTypeId |
| group | group | group |
| flowTypeId | flow_type_id | flowTypeId |
| flowId | flow_id | flowId |
| zoomUrl | zoom_url | zoomUrl |
| zoomPassword | zoom_password | zoomPassword |
| zoomInfo | zoom_info | zoomInfo |
| bldId | bld_id | bldId |
| formatId | format_id | formatId |
| mainBldId | main_bld_id | mainBldId |

## src/main/java/api/myitmo/model/schedule/Schedule.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/schedule/Schedule.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| dayNumber | day_number | dayNumber |
| weekNumber | week_number | weekNumber |
| date | date | date |
| note | note | note |
| lessons | lessons | lessons |
Observed unknown-type declarations (not public members): type, intersections. Retained as comments, not invented wire types.

## src/main/java/api/myitmo/model/sport/CanSignIn.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/CanSignIn.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| canSignIn | can_sign_in | canSignIn |
| unavailableReasons | unavailable_reasons | unavailableReasons |

## src/main/java/api/myitmo/model/sport/ChosenSportLesson.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/ChosenSportLesson.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| dateStart | date_start | dateStart |
| dateEnd | date_end | dateEnd |
| timeSlotId | time_slot_id | timeSlotId |
| timeStart | time_start | timeStart |
| timeEnd | time_end | timeEnd |
| roomId | room_id | roomId |
| roomName | room_name | roomName |
| teacherIsu | teacher_isu | teacherIsu |
| teacherFio | teacher_fio | teacherFio |
| typeId | type_id | typeId |
| linkUrl | link_url | linkUrl |
| comment | comment | comment |
| intersection | intersection | intersection |

## src/main/java/api/myitmo/model/sport/ChosenSportSection.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/ChosenSportSection.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| sectionName | section_name | sectionName |
| level | level | level |
| lessonGroups | lesson_groups | lessonGroups |

## src/main/java/api/myitmo/model/sport/ChosenSportWeekday.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/ChosenSportWeekday.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| weekday | weekday | weekday |
| id | id | id |
| dateStart | date_start | dateStart |
| dateEnd | date_end | dateEnd |
| timeSlotId | time_slot_id | timeSlotId |
| timeStart | time_start | timeStart |
| timeEnd | time_end | timeEnd |
| roomId | room_id | roomId |
| roomName | room_name | roomName |
| teacherIsu | teacher_isu | teacherIsu |
| teacherFio | teacher_fio | teacherFio |
| typeId | type_id | typeId |
| linkUrl | link_url | linkUrl |
| comment | comment | comment |
| intersection | intersection | intersection |
Inherited members: see the ChosenSportLesson mapping; 2.x may flatten their declarations.

## src/main/java/api/myitmo/model/sport/SportAttempts.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportAttempts.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| totalAttempts | total_attempts | totalAttempts |
| usedAttempts | used_attempts | usedAttempts |
| freeAttempts | free_attempts | freeAttempts |
| canSignIn | can_sign_in | canSignIn |

## src/main/java/api/myitmo/model/sport/SportAttendance.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportAttendance.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| type | type | type |
| name | name | name |
| evaluationId | evaluation_id | evaluationId |
| evaluationName | evaluation_name | evaluationName |
| sectionLevel | section_level | sectionLevel |
| score | score | score |
| date | date | date |
| isCompetition | is_competition | isCompetition |
| disciplineName | discipline_name | disciplineName |
| competitionName | competition_name | competitionName |
| place | place | place |

## src/main/java/api/myitmo/model/sport/SportDebt.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportDebt.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| havingDebt | is_having_debt | havingDebt |
| neededScore | needed_score | neededScore |
| freeAttempts | free_attempts | freeAttempts |

## src/main/java/api/myitmo/model/sport/SportExternat.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportExternat.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| signed | signed | signed |
| externatStatusId | externat_status_id | externatStatusId |
| declineReason | decline_reason | declineReason |

## src/main/java/api/myitmo/model/sport/SportFilters.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportFilters.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| buildingId | building_id | buildingId |
| sectionId | section_id | sectionId |
| sportTypeId | sport_type_id | sportTypeId |
| teacherIsu | teacher_isu | teacherIsu |

## src/main/java/api/myitmo/model/sport/SportHealthLevel.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportHealthLevel.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| isu | isu | isu |
| name | name | name |

## src/main/java/api/myitmo/model/sport/SportHealthLevelResponse.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportHealthLevelResponse.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| healthLevel | health_level | healthLevel |

## src/main/java/api/myitmo/model/sport/SportLesson.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportLesson.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| date | date | date |
| dateEnd | date_end | dateEnd |
| sectionId | section_id | sectionId |
| sectionName | section_name | sectionName |
| sectionLevel | section_level | sectionLevel |
| lessonGroupId | lesson_group_id | lessonGroupId |
| lessonLevel | lesson_level | lessonLevel |
| typeId | type_id | typeId |
| buildingId | building_id | buildingId |
| roomId | room_id | roomId |
| roomName | room_name | roomName |
| limit | limit | limit |
| available | available | available |
| comment | comment | comment |
| timeSlotId | time_slot_id | timeSlotId |
| timeSlotStart | time_slot_start | timeSlotStart |
| timeSlotEnd | time_slot_end | timeSlotEnd |
| intersection | intersection | intersection |
| canSignIn | can_sign_in | canSignIn |
| otherLessons | other_lessons | otherLessons |
| signed | signed | signed |
| teacherIsu | teacher_isu | teacherIsu |
| teacherFio | teacher_fio | teacherFio |
| OtherLesson.id | id | id |
| OtherLesson.weekday | weekday | weekday |
| OtherLesson.roomId | room_id | roomId |
| OtherLesson.roomName | room_name | roomName |
| OtherLesson.evaluationId | evaluation_id | evaluationId |
| OtherLesson.evaluationName | evaluation_name | evaluationName |
| OtherLesson.timeSlotId | time_slot_id | timeSlotId |
| OtherLesson.timeSlotStart | time_slot_start | timeSlotStart |
| OtherLesson.timeSlotEnd | time_slot_end | timeSlotEnd |
| OtherLesson.repeatable | repeatable | repeatable |
| OtherLesson.teacherIsu | teacher_isu | teacherIsu |
| OtherLesson.teacherFio | teacher_fio | teacherFio |
| OtherLesson.typeId | type_id | typeId |
| OtherLesson.comment | comment | comment |
| OtherLesson.intersection | intersection | intersection |

## src/main/java/api/myitmo/model/sport/SportLessonGroup.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportLessonGroup.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| level | level | level |
| levelName | level_name | levelName |
| hasFutureLessons | has_future_lessons | hasFutureLessons |
| lessons | lessons | lessons |
| weekdays | weekdays | weekdays |

## src/main/java/api/myitmo/model/sport/SportProject.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportProject.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| description | description | description |
| signed | signed | signed |
| limit | limit | limit |
| available | available | available |
| instructionLink | instruction_link | instructionLink |
| instructionDescription | instruction_description | instructionDescription |
| requisiteAvailable | requisite_available | requisiteAvailable |
| link | link | link |

## src/main/java/api/myitmo/model/sport/SportRequisite.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportRequisite.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| levelName | level_name | levelName |
| name | name | name |

## src/main/java/api/myitmo/model/sport/SportSchedule.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportSchedule.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| date | date | date |
| lessons | lessons | lessons |

## src/main/java/api/myitmo/model/sport/SportScore.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportScore.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| sum | sum | sum |
| attendances | attendances | attendances |
| Sum.attendances | attendances | attendances |
| Sum.other | other | other |

## src/main/java/api/myitmo/model/sport/SportSelection.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportSelection.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| requisites | requisites | requisites |

## src/main/java/api/myitmo/model/sport/SportSemester.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportSemester.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| studyYear | study_year | studyYear |
| semester | semester | semester |
| dateStart | date_start | dateStart |
| dateEnd | date_end | dateEnd |
| hardDateEnd | hard_date_end | hardDateEnd |
| current | current | current |
| choiceStart | choice_start | choiceStart |
| bachelorBound | bachelor_bound | bachelorBound |
| ppa1Start | ppa1_start | ppa1Start |
| ppa1End | ppa1_end | ppa1End |
| ppa2Start | ppa2_start | ppa2Start |
| ppa2End | ppa2_end | ppa2End |
| signDuration | sign_duration | signDuration |

## src/main/java/api/myitmo/model/sport/SportSemesterOption.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportSemesterOption.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| value | value | value |
| comment | comment | comment |

## src/main/java/api/myitmo/model/sport/SportSignLimit.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/SportSignLimit.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| limit | limit | limit |
| available | available | available |

## src/main/java/api/myitmo/model/sport/TimeSlot.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/sport/TimeSlot.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| timeStart | time_start | timeStart |
| timeEnd | time_end | timeEnd |

## src/main/java/api/myitmo/model/studyplan/StudyPlan.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlan.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| currentSemester | currentSemester | currentSemester |
| currentSemesterId | currentSemesterId | currentSemesterId |
| semestersCount | semestersCount | semestersCount |
| planInfo | planInfo | planInfo |
| semesters | semesters | semesters |
| structure | structure | structure |

## src/main/java/api/myitmo/model/studyplan/StudyPlanActivity.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanActivity.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| contentId | contentId | contentId |
| name | name | name |
| volume | volume | volume |
| workTypeId | workTypeId | workTypeId |

## src/main/java/api/myitmo/model/studyplan/StudyPlanContent.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanContent.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| moduleId | moduleId | moduleId |
| disciplineId | disciplineId | disciplineId |
| order | order | order |
| semester | semester | semester |
| creditPoints | creditPoints | creditPoints |
| activities | activities | activities |

## src/main/java/api/myitmo/model/studyplan/StudyPlanDepartment.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanDepartment.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| shortName | shortName | shortName |

## src/main/java/api/myitmo/model/studyplan/StudyPlanInfo.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanInfo.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| directionCode | directionCode | directionCode |
| directionName | directionName | directionName |
| levelQualification | levelQualification | levelQualification |
| planType | planType | planType |
| programName | programName | programName |
| startYear | startYear | startYear |

## src/main/java/api/myitmo/model/studyplan/StudyPlanNode.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanNode.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| id | id | id |
| name | name | name |
| type | type | type |
| moduleId | moduleId | moduleId |
| blockId | blockId | blockId |
| blockName | blockName | blockName |
| choiceParameterId | choiceParameterId | choiceParameterId |
| choiceParameterName | choiceParameterName | choiceParameterName |
| choiceAvailable | choiceAvailable | choiceAvailable |
| flowSelectable | flowSelectable | flowSelectable |
| replaceable | replaceable | replaceable |
| startSemesterSelectable | startSemesterSelectable | startSemesterSelectable |
| creditPoints | creditPoints | creditPoints |
| disciplineDuration | disciplineDuration | disciplineDuration |
| description | description | description |
| langCode | langCode | langCode |
| langName | langName | langName |
| rpdUrl | rpdUrl | rpdUrl |
| department | department | department |
| rules | rules | rules |
| children | children | children |
| contents | contents | contents |

## src/main/java/api/myitmo/model/studyplan/StudyPlanProgram.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanProgram.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| planId | planId | planId |
| specializationId | specializationId | specializationId |
| name | name | name |
| isActive | isActive | isActive |

## src/main/java/api/myitmo/model/studyplan/StudyPlanPrograms.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanPrograms.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| isu | isu | isu |
| programs | programs | programs |

## src/main/java/api/myitmo/model/studyplan/StudyPlanSemester.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/studyplan/StudyPlanSemester.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| semester | semester | semester |
| semesterId | semesterId | semesterId |
| semesterParity | semesterParity | semesterParity |
| studyYear | studyYear | studyYear |

## src/main/java/api/myitmo/model/system/DashboardItem.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/system/DashboardItem.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| i | i | i |
| x | x | x |
| y | y | y |
| w | w | w |
| h | h | h |
| widget | widget | widget |
| isStatic | static | isStatic |
| minWidth | min_w | minWidth |
| maxWidth | max_w | maxWidth |
| minHeight | min_h | minHeight |
| maxHeight | max_h | maxHeight |
| draggable | is_draggable | draggable |
| resizable | is_resizable | resizable |
| preserveAspectRatio | preserve_aspect_ratio | preserveAspectRatio |
| dragAllowFrom | drag_allow_from | dragAllowFrom |
| dragIgnoreFrom | drag_ignore_from | dragIgnoreFrom |
| resizeIgnoreFrom | resize_ignore_from | resizeIgnoreFrom |

## src/main/java/api/myitmo/model/system/MenuItem.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/system/MenuItem.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| title | title | title |
| description | description | description |
| to | to | to |
| icon | icon | icon |
| multicolorIcon | multicolor_icon | multicolorIcon |
| exact | exact | exact |
| color | color | color |
| children | children | children |

## src/main/java/api/myitmo/model/system/MenuResponse.java

Ported: src/commonMain/kotlin/dev/alllexey/itmoapi/myitmo/system/MenuResponse.kt

| Legacy member | Wire key | Modern member |
|---|---|---|
| menu | menu | menu |


<!-- Generated member map end -->
