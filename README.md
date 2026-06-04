# NACHA ACH File Reader — Pentaho PDI Step Plugin

A custom Pentaho Data Integration (PDI) step plugin that reads and parses NACHA ACH (Automated Clearing House) files. For each input row carrying a file path, the step reads the NACHA file and outputs one row per ACH transaction, enriched with file and batch header context.

---

## What it does

For every input row (each supplying a file path), the step:

1. Opens the NACHA file at the path held in the configured field.
2. Parses all 94-character fixed-width records.
3. Emits output rows according to the selected **Output Mode**.
4. Passes all original input fields through unchanged.

---

## NACHA File Format

NACHA files are fixed-width flat files where every line is exactly **94 characters**. Record types are identified by the first character:

| Code | Record Type    | Description |
|------|----------------|-------------|
| `1`  | File Header    | One per file — destination/origin bank info |
| `5`  | Batch Header   | One per batch — company name, SEC code, effective date |
| `6`  | Entry Detail   | One per transaction — account, amount, individual name |
| `7`  | Addenda        | Optional — additional payment info for the preceding entry |
| `8`  | Batch Control  | One per batch — totals and hash |
| `9`  | File Control   | One per file — totals across all batches |

---

## Output Modes

### Entry Details (default)

One output row per **Entry Detail (type 6)** record, enriched with context from the surrounding file header and batch header. Multiple addenda records are concatenated into a single field using `|` as separator.

**Output fields (28 NACHA fields appended after all input fields):**

| Field | Source |
|---|---|
| `nacha_record_type` | Always `"6"` |
| `nacha_file_immediate_destination` | File Header |
| `nacha_file_immediate_origin` | File Header |
| `nacha_file_creation_date` | File Header (YYMMDD) |
| `nacha_file_creation_time` | File Header (HHMM) |
| `nacha_file_id_modifier` | File Header |
| `nacha_file_destination_name` | File Header |
| `nacha_file_origin_name` | File Header |
| `nacha_batch_service_class_code` | Batch Header |
| `nacha_batch_company_name` | Batch Header |
| `nacha_batch_company_discretionary_data` | Batch Header |
| `nacha_batch_company_identification` | Batch Header |
| `nacha_batch_sec_code` | Batch Header (PPD, CCD, WEB, …) |
| `nacha_batch_company_entry_description` | Batch Header |
| `nacha_batch_effective_entry_date` | Batch Header (YYMMDD) |
| `nacha_batch_odfi_identification` | Batch Header |
| `nacha_batch_number` | Batch Header |
| `nacha_entry_transaction_code` | Entry Detail |
| `nacha_entry_receiving_dfi_routing` | Entry Detail |
| `nacha_entry_check_digit` | Entry Detail |
| `nacha_entry_dfi_account_number` | Entry Detail |
| `nacha_entry_amount` | Entry Detail (10 digits, implied 2 decimal places) |
| `nacha_entry_individual_id_number` | Entry Detail |
| `nacha_entry_individual_name` | Entry Detail |
| `nacha_entry_discretionary_data` | Entry Detail |
| `nacha_entry_addenda_indicator` | Entry Detail (`0` or `1`) |
| `nacha_entry_trace_number` | Entry Detail |
| `nacha_addenda_concatenated` | Addenda records joined by `\|` |

### All Records

One output row per **NACHA line** (all record types). Fields not applicable to a given record type are output as empty string.

Adds **41 NACHA fields** covering all record types including batch control (`nacha_batch_entry_hash`, `nacha_batch_total_debit`, …), addenda (`nacha_addenda_type_code`, `nacha_addenda_payment_info`, …), and file control (`nacha_file_batch_count`, `nacha_file_total_debit`, …).

---

## Usage in a Transformation

```
┌─────────────────────┐      ┌──────────────────────────┐      ┌──────────────────┐
│  Get File Names     │─────▶│  NACHA ACH File Reader   │─────▶│  Table Output    │
│  (or Table Input)   │      │                          │      │  (or any step)   │
│                     │      │  File Path Field: path   │      │                  │
│  outputs: path      │      │  Output Mode: Entry      │      └──────────────────┘
└─────────────────────┘      │  Details                 │
                             └──────────────────────────┘
```

- **Get File Names** scans a directory and outputs a `path` field for each `.ach` file found.
- **NACHA ACH File Reader** reads each file and fans out to one row per ACH transaction.
- One input row (one file path) → many output rows (one per transaction).

The file path field supports PDI variables: e.g. `${ACH_INPUT_DIR}/batch.ach`.

---

## Dialog

The step dialog has a single **Settings** tab:

- **File Path Field** — select (or type) the upstream field that holds the NACHA file path.
- **Output Mode** — radio button:
  - `Entry Details` — one row per type-6 record (recommended for transaction processing)
  - `All Records` — one row per NACHA line (useful for auditing or format validation)

---

## Testing

The project ships a JUnit 5 test suite that exercises the parser and step metadata without requiring a running PDI instance.

### Run all tests

```bash
mvn test
```

### Test classes and coverage

| Test class | Tests | What is covered |
|---|---|---|
| `NachaParserTest` | 15 | Full parse of the sample fixture: entry count, file/batch/entry field values, addenda concatenation, all-records mode, record type sequencing |
| `NachaParserEdgeCaseTest` | 9 | Empty file, blank-lines-only file, invalid line length → IOException, entry without file header, entry without batch header, multiple batches (independent context), multiple addenda (pipe-joined), field trimming, padding-nines ignored in entry-detail mode |
| `NachaReaderStepMetaTest` | 13 | Defaults, XML round-trip (filePathField + outputMode), clone (distinct/copies/independent), getFields field counts (28 ENTRY_DETAILS / 41 ALL_RECORDS), check() errors and OK |
| **Total** | **37** | |

The parser tests use `src/test/resources/sample-nacha.ach` — a minimal but complete ACH file:
1 file header · 1 batch header · 2 entry details (first with an addenda) · 1 batch control · 1 file control · 3 padding lines (10-line block).

---

## Building

### Prerequisites

- Java 11+
- Maven 3.6+
- PDI compile-only stubs installed locally (see below)

### 1. Install the PDI compile-only stubs

```bash
git clone https://github.com/satishreddy13/pdi-stubs.git
cd pdi-stubs
mvn install -DskipTests
```

### 2. Build the plugin

```bash
cd nacha-reader-plugin
mvn clean package -DskipTests
```

Produces:
```
target/
  nacha-reader-plugin-1.0.0.jar          ← compiled plugin
  nacha-reader-plugin-1.0.0-plugin.zip   ← deployable zip
```

---

## Deployment

Unzip the plugin into PDI's `plugins/` directory and restart Spoon:

```bash
unzip target/nacha-reader-plugin-1.0.0-plugin.zip \
  -d <PDI_HOME>/plugins/
```

The **"NACHA ACH File Reader"** step will appear in the **Input** category.

---

## Project Structure

```
nacha-reader-plugin/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/pdi/plugin/nachareader/
    │   │   ├── nacha/
    │   │   │   ├── RecordType.java           ← enum: 1/5/6/7/8/9
    │   │   │   ├── NachaRecord.java          ← abstract base (field extraction helper)
    │   │   │   ├── FileHeaderRecord.java     ← type 1
    │   │   │   ├── BatchHeaderRecord.java    ← type 5
    │   │   │   ├── EntryDetailRecord.java    ← type 6
    │   │   │   ├── AddendaRecord.java        ← type 7
    │   │   │   ├── BatchControlRecord.java   ← type 8
    │   │   │   ├── FileControlRecord.java    ← type 9
    │   │   │   └── NachaParser.java          ← parseEntryDetails() / parseAllRecords()
    │   │   ├── NachaReaderStep.java          ← row processing (reads file, emits rows)
    │   │   ├── NachaReaderStepMeta.java      ← @Step annotation, getFields, XML persistence
    │   │   ├── NachaReaderStepData.java      ← runtime data holder
    │   │   └── NachaReaderStepDialog.java    ← SWT dialog (field combo + mode radios)
    │   └── resources/
    │       ├── plugin.xml                    ← plugin registration descriptor
    │       └── assembly.xml                  ← Maven assembly (builds deployable zip)
    └── test/
        ├── java/com/example/pdi/plugin/nachareader/
        │   ├── nacha/
        │   │   ├── NachaParserTest.java
        │   │   └── NachaParserEdgeCaseTest.java
        │   └── NachaReaderStepMetaTest.java
        └── resources/
            └── sample-nacha.ach              ← minimal complete ACH test fixture
```

---

## Compatibility

| Component | Version |
|-----------|---------|
| Pentaho PDI | 11.0.0.0-237 (tested) |
| Java | 11+ (compiled at Java 11 bytecode) |
| OS | macOS (tested), Linux, Windows |

---

## Related

- [xml-generator-plugin](https://github.com/satishreddy13/xml-generator-plugin) — XML Generator PDI step plugin
- [csps-doc-id-generator](https://github.com/satishreddy13/csps-doc-id-generator) — CSPS DOC_ID Generator PDI step plugin
- [pdi-stubs](https://github.com/satishreddy13/pdi-stubs) — Compile-only PDI API stubs used by all plugins
