# NACHA ACH File Reader — Pentaho PDI Step Plugin

A custom Pentaho Data Integration (PDI) step plugin that reads and parses NACHA ACH (Automated Clearing House) files. For each input row carrying a file path, the step reads the NACHA file and outputs rows according to the selected mode: one row per ACH transaction, one row per NACHA line, or a structural validation result.

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

### Validate

Validates the structural integrity of the NACHA file without parsing individual transactions. Emits **one `PASS` row** if the file is valid, or **one `FAIL` row per error** if not.

**Output fields (5 fields appended after all input fields):**

| Field | PASS value | FAIL value |
|---|---|---|
| `nacha_validation_status` | `PASS` | `FAIL` |
| `nacha_validation_line_number` | `""` | 1-based line number of the offending record |
| `nacha_validation_error_code` | `""` | Error code (see table below) |
| `nacha_validation_message` | `""` | Human-readable description |
| `nacha_validation_raw_line` | `""` | The raw 94-character line that caused the error |

**Checks always performed:**

| Error code | Description |
|---|---|
| `LINE_LENGTH` | A non-blank line is not exactly 94 characters |
| `INVALID_RECORD_TYPE` | First character is not one of `1 5 6 7 8 9` |
| `MISSING_FILE_HEADER` | No File Header (type 1) found |
| `DUPLICATE_FILE_HEADER` | More than one File Header record |
| `MISSING_FILE_CONTROL` | No File Control (type 9) found |
| `UNCLOSED_BATCH` | A Batch Header was opened but not closed with a Batch Control |
| `BATCH_CONTROL_NO_BATCH` | A Batch Control appears without a preceding Batch Header |
| `ORPHAN_ENTRY_DETAIL` | An Entry Detail (type 6) appears outside a batch |
| `ORPHAN_ADDENDA` | An Addenda (type 7) appears outside a batch or not after an Entry Detail |
| `BATCH_NUMBER_MISMATCH` | The batch number in Batch Header and Batch Control do not match |

**Optional checks (configurable in the Validation tab):**

| Error code | Enabled by | Description |
|---|---|---|
| `ENTRY_COUNT_MISMATCH` | Count checks | Entry/addenda count in Batch Control doesn't match actual count |
| `ENTRY_HASH_MISMATCH` | Count checks | Entry hash in Batch Control doesn't match sum of routing numbers |
| `FILE_BATCH_COUNT_MISMATCH` | Count checks | Batch count in File Control doesn't match actual batch count |
| `FILE_ENTRY_COUNT_MISMATCH` | Count checks | Entry/addenda count in File Control doesn't match actual count |
| `FILE_HASH_MISMATCH` | Count checks | Entry hash in File Control doesn't match sum of routing numbers |
| `DEBIT_AMOUNT_MISMATCH` | Amount checks | Debit total in Batch Control doesn't match sum of debit entries |
| `CREDIT_AMOUNT_MISMATCH` | Amount checks | Credit total in Batch Control doesn't match sum of credit entries |
| `FILE_DEBIT_MISMATCH` | Amount checks | Debit total in File Control doesn't match sum across batches |
| `FILE_CREDIT_MISMATCH` | Amount checks | Credit total in File Control doesn't match sum across batches |
| `HEADER_DESTINATION_MISMATCH` | Expected Immediate Destination | File Header destination doesn't match configured value |
| `HEADER_ORIGIN_MISMATCH` | Expected Immediate Origin | File Header origin doesn't match configured value |
| `HEADER_COMPANY_MISMATCH` | Expected Company Name | Batch Header company name doesn't match configured value |
| `HEADER_SEC_CODE_MISMATCH` | Expected SEC Code | Batch Header SEC code doesn't match configured value |

---

## Usage in a Transformation

**Transaction processing (Entry Details):**
```
┌─────────────────────┐      ┌──────────────────────────┐      ┌──────────────────┐
│  Get File Names     │─────▶│  NACHA ACH File Reader   │─────▶│  Table Output    │
│  (or Table Input)   │      │                          │      │  (or any step)   │
│                     │      │  File Path Field: path   │      │                  │
│  outputs: path      │      │  Output Mode: Entry      │      └──────────────────┘
└─────────────────────┘      │  Details                 │
                             └──────────────────────────┘
```

**File validation (Validate):**
```
┌─────────────────────┐      ┌──────────────────────────┐      ┌──────────────────┐
│  Get File Names     │─────▶│  NACHA ACH File Reader   │─────▶│  Filter Rows     │
│  (or Table Input)   │      │                          │      │  status = FAIL   │
│                     │      │  File Path Field: path   │      │                  │
│  outputs: path      │      │  Output Mode: Validate   │      └──────────────────┘
└─────────────────────┘      └──────────────────────────┘
```

- **Get File Names** scans a directory and outputs a `path` field for each `.ach` file found.
- **NACHA ACH File Reader** reads each file and fans out to one row per ACH transaction.
- One input row (one file path) → many output rows (one per transaction).

The file path field supports PDI variables: e.g. `${ACH_INPUT_DIR}/batch.ach`.

---

## Dialog

### Settings tab

- **File Path Field** — select (or type) the upstream field that holds the NACHA file path.
- **Output Mode** — radio button:
  - `Entry Details` — one row per type-6 record (recommended for transaction processing)
  - `All Records` — one row per NACHA line (useful for auditing or raw inspection)
  - `Validate` — structural validation; emits PASS or one FAIL row per error

### Validation tab *(active when Output Mode is Validate)*

**Expected Header Values** — leave any field blank to skip that check:

| Field | Checked against |
|---|---|
| Immediate Destination | File Header positions 4–13 |
| Immediate Origin | File Header positions 14–23 |
| Company Name | Batch Header positions 5–20 |
| SEC Code | Batch Header positions 51–53 |

**Integrity Checks:**

- **Check entry/addenda counts and entry hash** — validates the entry count and routing-number hash in each Batch Control and the File Control (enabled by default)
- **Check debit/credit amount totals** — validates the debit and credit totals in each Batch Control and the File Control (enabled by default)

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
| `NachaReaderStepMetaTest` | 13 | Defaults, XML round-trip (filePathField + outputMode + validationConfig), clone independence, getFields field counts (5 VALIDATE / 28 ENTRY_DETAILS / 41 ALL_RECORDS), check() errors and OK |
| `NachaValidatorTest` | 26 | Valid file PASS, minimal file, mixed debit/credit totals, line-length error, missing/duplicate file header, missing file control, orphan entry/addenda, unclosed batch, batch number mismatch, entry count and hash mismatch, debit/credit amount mismatch, file-level count and amount mismatches, count/amount checks disabled, header config match and mismatch (destination, origin, company, SEC), isCredit/isDebit classification |
| **Total** | **63** | |

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
    │   │   │   ├── NachaParser.java          ← parseEntryDetails() / parseAllRecords()
    │   │   │   ├── NachaValidationConfig.java ← config POJO (header values, check flags)
    │   │   │   ├── NachaValidationError.java  ← error POJO with 23 error code constants
    │   │   │   └── NachaValidator.java        ← structural validation engine
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
        │   │   ├── NachaParserEdgeCaseTest.java
        │   │   └── NachaValidatorTest.java
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
