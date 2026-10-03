# MyItmoApi agent guide

The ecosystem-wide rules are in the Android repository's `AGENTS.md`
(`/Users/alllexey/proj/ITMO.Widgets/AGENTS.md`). This file adds what is
specific to this library.

## Responsibilities

The only client for official MyITMO (`api.myitmo`) and BARS (`api.bars`)
endpoints used by ITMO.Widgets. When the university exposes a useful endpoint,
it is added here, never as a second Retrofit interface in the app or Backend.

## Hard rules

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
mvn -q test
```
