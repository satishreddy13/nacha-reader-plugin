---
paths:
  - "src/main/java/**/nacha/*.java"
---

# NACHA Domain Layer

- All record types extend `NachaRecord`; use `field(start, end)` for 1-based positional extraction (trims spaces automatically)
- `NachaParser` is stateless — no instance fields, thread-safe
- `parseEntryDetails()` flushes `currentEntry` at type-6 AND type-8 (batch control) — the last entry in a batch has no following type-6 to trigger the flush
- `ParsedEntry` and `RawRecord` are inner classes of `NachaParser`; all fields default to `""`
- NACHA amount field (positions 30–39 on type-6) is 10 digits with 2 implied decimal places — stored as String, not parsed to numeric
