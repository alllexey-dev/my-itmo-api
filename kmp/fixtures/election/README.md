# Election, finances, requests and system fixtures

All JSON in these four areas is synthetic. No university request, account export or credential is used.
The full wire trees are compared through the pinned Central `dev.alllexey:my-itmo-api:1.8.2` Retrofit/Gson converter.

- Election ports nine nondeprecated endpoints and eight models. `ElectionFlow`, `ElectionFlowChain` and
  `FlowLimit` belong to the current endpoints; the deprecated `selected_flow_chains` endpoint and its legacy
  `Flow*`/`FlowChainsWrapper` models are not ported.
- All three `order/*` POSTs expose `ResultResponse<JsonElement?>`, preserving unknown result shapes.
  `ChangeResult` documents the known legacy shape without constraining those responses.
- Finances ports one endpoint and `ScholarshipTotal`; optional date bounds are independently omitted.
  Category whitespace and sums in rubles are preserved.
- Requests ports one endpoint and `RequestSummary`; system ports three endpoints and three models.
- The six exact date spelling pairs in `ParityHarness` were coordinator-reviewed on 2026-10-04 under
  SP-02 date-by-instant / ADR 0025 Q5. Tests prove same instants and reject unreviewed spelling, values,
  paths, fixtures and extra keys. No new missing-field/default difference is accepted.
- Request tests compare each production operation to actual pinned legacy Retrofit request construction.
  Legacy `clearAllSelectedFlows` has a wildcard return type rejected by Retrofit before call creation;
  its test invokes pinned Retrofit 3.0.0 `RequestFactory` on that actual method's annotations instead.
  Gson's HTML escaping is compared as full JSON values; exact modern body bytes are tested separately.
  JSON content types are asserted concretely (legacy adds UTF-8 charset); queries are compared as full
  decoded multimaps, with encoded empty/absent query spelling treated equivalently.
- The common MockEngine seam is under the election test path and shared only by these four areas.
  The JVM request assertion is in `ElectionParityTest`; response comparisons still retain every wire key.

Consumer delivery still requires merge into `v2.3/next`, owner promotion to `master` and the app pin update.
A consumer copying a fixture must record its exact source library SHA and original fixture path.
