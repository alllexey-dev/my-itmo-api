# Study-plan synthetic fixtures

Every fixture here is synthetic, not a captured university response or account
export. Names and identifiers are test-only values.

- `programs.json`: active and inactive programs, with and without specialization.
- `study-plan.json`: all nine study-plan models; nested blocks, modules and
  disciplines; semester-keyed workloads, academic credits, five observed work
  kinds, and null assessment hours.
- `absence.json`: a plan and discipline with absent or null optional details.
- `empty-programs.json`: no available programs.

KM-10b1 consumers may vendor these fixtures after delivery. Record the source
MyItmoApi commit SHA, original `kmp/fixtures/studyplan/<case>.json` path and
synthetic nature in the consumer's provenance README. Refresh the SHA whenever
updating a vendored fixture.
