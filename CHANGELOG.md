# Changelog

## 2.0.1 - development

- Move `kmp/` to `2.0.1-SNAPSHOT` after the 2.0.0 release; the README points
  at `dev.alllexey:my-itmo-api-kmp:2.0.0` on Maven Central.

## 2.0.0 - 2026-10-10

- Add the separate Kotlin Multiplatform client in `kmp/` for JVM/Android and
  iOS, with suspend ITMO.ID, MyITMO and BARS APIs under `dev.alllexey.itmoapi`.
- Inject engines, time and consumer-owned atomic token/session storage. Add
  typed failures, coalesced refresh and isolated BARS cookie replay without
  password login, shared-client SSO or `obtainCodeFromSession`.
- Cover ported models/endpoints with synthetic parity tests against 1.8.2,
  including model/member completeness and documentation checks.
- Document 2.x first in the Russian README, with compiled/executed examples,
  and provide an English migration guide with the full generated member map.
- Keep 1.x compatible in the root as `dev.alllexey:my-itmo-api`.

## 1.8.3 — unreleased

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

## 1.8.2 — 2026-10-03

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
