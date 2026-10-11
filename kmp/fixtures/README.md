# Test fixtures

Every file here is synthetic and hand-made. None is a captured university
response or account export: names, ISU numbers, identifiers, tokens and codes
are made-up test values. Never add real data, even redacted.

Layout: `<area>/<case>.json` (and `errors/*.html` for non-JSON bodies). The
`generateFixtures` Gradle task embeds the whole tree into common test sources,
so the same fixtures run on JVM and iOS.

## Adding a fixture

1. Write the smallest JSON that shows the observed wire shape: the envelope,
   the fields you rely on, observed nulls and absences, enum-like values and
   units exactly as the server sends them. Do not invent fields you have not
   seen.
2. Save it as `kmp/fixtures/<area>/<case>.json`; use the endpoint area name and
   a case name such as `empty`, `error` or `absence`.
3. Load it in a test with `fixture("<area>/<case>.json")`, usually through
   `mockExchange` in `testing/MockClient.kt`.
4. Add the path and its model serializer to `FixtureDecodeTest`; it fails when
   an endpoint fixture has no model. Fixtures under `core/`, `errors/` and
   `itmoid/` are exercised by their own codec, transport and ITMO.ID tests.
