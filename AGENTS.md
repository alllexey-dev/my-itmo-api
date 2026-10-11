# MyItmoApi agent guide

Read `CONTRIBUTING.md` first: layout, tests and how to add an endpoint.

## Build and test

```bash
./gradlew -p kmp jvmTest                 # 2.x, JDK 17
./gradlew -p kmp iosSimulatorArm64Test   # 2.x on iOS, needs Xcode
./mvnw -B verify -Dgpg.skip=true         # 1.x, JDK 17
```

## Rules

- Never log, print or put in exceptions, `toString()`, tests, fixtures or docs:
  tokens, authorization codes, PKCE verifiers, cookies, `Authorization` headers,
  `MyItmoException.Network.cause` or `Api.serverMessage`.
- Fixtures are synthetic and hand-made (`kmp/fixtures/README.md`). Never add
  real responses, even redacted. Tests never call real ITMO.ID, MyITMO or BARS.
- Never open `.refresh-token`, `.env*`, `local.properties`, `*.jks` or `*.har`.
- 1.x (root) takes compatible fixes only; new work goes to 2.x in `kmp/`.
- Publishing happens only by the owner pushing a tag. Never push tags, run
  release workflows, `mvn deploy`/`install`, `publishToMavenLocal` or any
  Maven Central task.
