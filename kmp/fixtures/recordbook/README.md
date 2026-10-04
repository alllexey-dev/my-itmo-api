# Recordbook synthetic fixtures

Every fixture here is synthetic, not a captured university response or account
export. `record-book.json` retains the SP-02 seed unchanged.

- `specializations.json`: a program and its current and previous semesters.
- `record-book.json`: subjects with and without points, a final grade, a teacher,
  surrounding name whitespace and an offset date-time.
- `controls.json`: root and child controls, points ranges, a pending result and
  a teacher with absent name parts.
- `absence.json`: absence of an assessment result, date, teacher and LMS link.
  It does not invent an attendance or absence-grade wire value.
- `empty.json`: no available entries.

KM-10b1 consumers may vendor these fixtures after delivery. Record the source
MyItmoApi commit SHA, original `kmp/fixtures/recordbook/<case>.json` path and
synthetic nature in the consumer's provenance README. Refresh the SHA whenever
updating a vendored fixture.
