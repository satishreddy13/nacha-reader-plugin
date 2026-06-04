# NACHA ACH File Reader — PDI Step Plugin

## Architecture

Two layers — domain (pure Java, no PDI) and PDI integration:

```
nacha/                      ← pure Java, no PDI dependencies
  RecordType.java           ← enum: FILE_HEADER(1) BATCH_HEADER(5) ENTRY_DETAIL(6)
                                    ADDENDA(7) BATCH_CONTROL(8) FILE_CONTROL(9)
  NachaRecord.java          ← abstract base; field(start, end) extracts 1-based positions
  FileHeaderRecord.java     ← type 1
  BatchHeaderRecord.java    ← type 5
  EntryDetailRecord.java    ← type 6
  AddendaRecord.java        ← type 7
  BatchControlRecord.java   ← type 8
  FileControlRecord.java    ← type 9
  NachaParser.java          ← stateless; parseEntryDetails() / parseAllRecords()

NachaReaderStep.java        ← processRow: reads file path from input field, fans out rows
NachaReaderStepMeta.java    ← @Step annotation, getFields (28 or 41 fields), XML round-trip
NachaReaderStepData.java    ← outputRowMeta, filePathFieldIndex (resolved on first row)
NachaReaderStepDialog.java  ← SWT dialog: file-path field combo + output mode radios
```

## Key design decisions

- NACHA lines are exactly 94 chars; parser throws `IOException` on any other length
- Blank/empty lines are silently skipped (handles trailing newlines and padding)
- Nine-filled padding lines (`9999...`) parsed as FILE_CONTROL — ignored in ENTRY_DETAILS mode
- Addenda (type 7) are concatenated with `|` separator into `nacha_addenda_concatenated`
- File encoding is always `US_ASCII` — not configurable
- File path field supports PDI variable substitution via `environmentSubstitute()`

## Output modes

- **ENTRY_DETAILS** — one row per type-6 record enriched with file/batch header context (28 NACHA fields)
- **ALL_RECORDS** — one row per NACHA line; unpopulated fields are `""` (41 NACHA fields)

## Build commands

```bash
mvn test                          # run all 37 tests
mvn test -Dtest=NachaParserTest   # run parser tests only
mvn clean package -DskipTests     # build nacha-reader-plugin-1.0.0-plugin.zip
```

## Test fixture

`src/test/resources/sample-nacha.ach` — 10 lines (94 chars each):
- Line 1: File Header (type 1)
- Line 2: Batch Header (type 5)
- Line 3: Entry Detail with addenda indicator=1 (type 6) — JOHN SMITH, amount 0000150000
- Line 4: Addenda (type 7) — address info
- Line 5: Entry Detail, no addenda (type 6) — JANE DOE, amount 0000200000
- Line 6: Batch Control (type 8)
- Line 7: File Control (type 9)
- Lines 8–10: Padding nines

## PDI stubs

PDI JARs are not on Maven Central. Compile-only stubs are in the sibling `pdi-stubs` project, installed to `~/.m2` with `mvn install`. Scope is `provided` — available at compile and test time.

## Compact instructions

Preserve: code changes, test failures, field position corrections, NACHA spec details.
Summarize: architecture explanations, file listings, build output when all tests pass.
