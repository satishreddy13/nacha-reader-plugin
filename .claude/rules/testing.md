---
paths:
  - "src/test/**"
---

# Testing

- Framework: JUnit 5 (`junit-jupiter:5.10.2`), run with `mvn test`
- Parser tests use `@TempDir` for files built in-test; fixture tests load `sample-nacha.ach` via classloader
- `NachaParserTest` — 15 tests against sample-nacha.ach (entry count, field values, addenda, all-records mode)
- `NachaParserEdgeCaseTest` — 9 tests (empty file, blank lines, invalid length, missing headers, multiple batches, multiple addenda, trimming, padding nines)
- `NachaReaderStepMetaTest` — 13 tests (defaults, XML round-trip, clone, getFields counts, check())
- When building temp ACH lines in tests: every line must be exactly 94 chars — use `pad(s, len)` helper or the parser will throw `IOException`
- XML round-trip test wraps `meta.getXML()` in `<step>...</step>` before DOM parsing
