# MyItmoApi agent guide

Read `CONTRIBUTING.md` first: layout, tests and how to add an endpoint.

## Build and test

```bash
./gradlew jvmTest                 # JDK 17
./gradlew iosSimulatorArm64Test   # iOS, needs Xcode
cd spec/tools && npm ci && npm run lint && npm run examples   # specifications
```

## Rules

- Never log, print or put in exceptions, `toString()`, tests, fixtures or docs:
  tokens, authorization codes, PKCE verifiers, cookies, `Authorization` headers,
  `MyItmoException.Network.cause` or `Api.serverMessage`.
- Test fixtures (`fixtures/`) are synthetic and hand-made (`fixtures/README.md`).
  Never add real responses there, even redacted. Tests never call real ITMO.ID,
  My ITMO or BARS.
- `spec/openapi/examples/` is the one exception: it holds anonymized live
  responses that back the specifications. Anonymize them before adding (no
  names, ISU numbers, emails, phones, tokens or identifiers of real people) and
  never use them as test fixtures.
- Never open `.refresh-token`, `.env*`, `local.properties`, `*.jks` or `*.har`.
- Publishing happens only by the owner pushing a tag. Never push tags, run
  release workflows, `publishToMavenLocal` or any Maven Central task.
