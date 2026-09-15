# MyItmoApi agent guide

The ecosystem-wide rules are in the Android repository's `AGENTS.md`
(`/Users/alllexey/proj/ITMO.Widgets.copy/AGENTS.md`). This file adds what is
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

## Build

```bash
mvn -q test
```
