# Changelog

## Unreleased

### 2026-09-30

- `BarsAuthHelper.requestCodeWithCookies(state, cookieHeader)` requests a BARS
  authorization code with ITMO.ID session cookies held by the caller (for
  example a WebView cookie store): one request, no redirects followed, the body
  is not read and the client's cookie jar is untouched. `BarsSessionCode`
  tells a code (`CODE`) from a required login (`LOGIN_REQUIRED`), a foreign or
  unusable redirect (`REJECTED`) and a server failure (`HTTP_ERROR`), and
  returns the response's `Set-Cookie` values. `obtainCodeFromSession` is
  unchanged. Version 1.8.2-SNAPSHOT, installed to Maven Local only; not
  published.

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
