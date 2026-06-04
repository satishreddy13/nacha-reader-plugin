package com.example.pdi.plugin.nachareader.nacha;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the structural integrity of a NACHA ACH file.
 *
 * <p>Checks performed (always):
 * <ul>
 *   <li>Every non-blank line is exactly 94 characters</li>
 *   <li>Record type discriminator is one of 1, 5, 6, 7, 8, 9</li>
 *   <li>File starts with a File Header (type 1) and ends with a File Control (type 9)</li>
 *   <li>Every batch opened with a Batch Header (type 5) is closed with a Batch Control (type 8)</li>
 *   <li>Entry Detail (type 6) and Addenda (type 7) records only appear inside a batch</li>
 *   <li>Batch numbers match between type-5 and type-8 records</li>
 * </ul>
 *
 * <p>Optional checks (driven by {@link NachaValidationConfig}):
 * <ul>
 *   <li><b>Count checks</b>: entry/addenda count, entry hash in batch and file control</li>
 *   <li><b>Amount checks</b>: debit/credit totals in batch and file control</li>
 *   <li><b>Header checks</b>: immediate destination, origin, company name, SEC code</li>
 * </ul>
 *
 * <p>Returns an empty list for a valid file; one {@link NachaValidationError} per finding otherwise.
 */
public class NachaValidator {

    /**
     * Validates {@code filePath} using the supplied config.
     * @return empty list if valid; one entry per error if not
     */
    public List<NachaValidationError> validate(Path filePath, NachaValidationConfig config)
            throws IOException {

        List<NachaValidationError> errors = new ArrayList<>();

        // ---- parser state ----
        boolean inBatch           = false;
        boolean fileHeaderSeen    = false;
        boolean fileControlSeen   = false;
        boolean lastWasEntry      = false;   // for addenda orphan check
        int     batchOpenLine     = 0;
        String  batchHeaderNumber = "";

        // ---- batch-level accumulators ----
        int  batchEntryCount   = 0;
        int  batchAddendaCount = 0;
        long batchDebitTotal   = 0L;
        long batchCreditTotal  = 0L;
        long batchEntryHash    = 0L;

        // ---- file-level accumulators ----
        int  totalBatchCount        = 0;
        int  totalEntryAddendaCount = 0;
        long totalDebitTotal        = 0L;
        long totalCreditTotal       = 0L;
        long totalEntryHash         = 0L;

        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.US_ASCII)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                if (fileControlSeen) continue;  // skip padding records after File Control

                // ---- line-length check ----
                if (line.length() != NachaRecord.LINE_LENGTH) {
                    errors.add(new NachaValidationError(lineNumber,
                        NachaValidationError.LINE_LENGTH,
                        "Expected 94 characters but found " + line.length(),
                        line));
                    continue;  // cannot safely parse further fields on this line
                }

                RecordType rt = RecordType.fromChar(line.charAt(0));

                switch (rt) {

                    // ----------------------------------------------------------------
                    case FILE_HEADER: {
                        if (fileHeaderSeen) {
                            errors.add(err(lineNumber, NachaValidationError.DUPLICATE_FILE_HEADER,
                                "Duplicate File Header record", line));
                        }
                        fileHeaderSeen = true;

                        FileHeaderRecord fh = new FileHeaderRecord(line);

                        if (!config.getExpectedImmediateDestination().isEmpty()
                                && !config.getExpectedImmediateDestination()
                                          .equals(fh.immediateDestination)) {
                            errors.add(err(lineNumber, NachaValidationError.HEADER_DESTINATION_MISMATCH,
                                "Immediate destination: expected '"
                                    + config.getExpectedImmediateDestination()
                                    + "' but found '" + fh.immediateDestination + "'", line));
                        }
                        if (!config.getExpectedImmediateOrigin().isEmpty()
                                && !config.getExpectedImmediateOrigin()
                                          .equals(fh.immediateOrigin)) {
                            errors.add(err(lineNumber, NachaValidationError.HEADER_ORIGIN_MISMATCH,
                                "Immediate origin: expected '"
                                    + config.getExpectedImmediateOrigin()
                                    + "' but found '" + fh.immediateOrigin + "'", line));
                        }
                        break;
                    }

                    // ----------------------------------------------------------------
                    case BATCH_HEADER: {
                        if (!fileHeaderSeen) {
                            errors.add(err(lineNumber, NachaValidationError.MISSING_FILE_HEADER,
                                "Batch Header found before File Header", line));
                        }
                        if (inBatch) {
                            errors.add(err(batchOpenLine, NachaValidationError.UNCLOSED_BATCH,
                                "Batch opened at line " + batchOpenLine
                                    + " was not closed before next Batch Header at line "
                                    + lineNumber, ""));
                        }
                        BatchHeaderRecord bh = new BatchHeaderRecord(line);

                        if (!config.getExpectedCompanyName().isEmpty()
                                && !config.getExpectedCompanyName().equals(bh.companyName)) {
                            errors.add(err(lineNumber, NachaValidationError.HEADER_COMPANY_MISMATCH,
                                "Company name: expected '"
                                    + config.getExpectedCompanyName()
                                    + "' but found '" + bh.companyName + "'", line));
                        }
                        if (!config.getExpectedSecCode().isEmpty()
                                && !config.getExpectedSecCode()
                                          .equals(bh.standardEntryClassCode)) {
                            errors.add(err(lineNumber, NachaValidationError.HEADER_SEC_CODE_MISMATCH,
                                "SEC code: expected '"
                                    + config.getExpectedSecCode()
                                    + "' but found '" + bh.standardEntryClassCode + "'", line));
                        }

                        // reset batch accumulators
                        inBatch           = true;
                        batchOpenLine     = lineNumber;
                        batchHeaderNumber = bh.batchNumber;
                        batchEntryCount   = 0;
                        batchAddendaCount = 0;
                        batchDebitTotal   = 0L;
                        batchCreditTotal  = 0L;
                        batchEntryHash    = 0L;
                        lastWasEntry      = false;
                        break;
                    }

                    // ----------------------------------------------------------------
                    case ENTRY_DETAIL: {
                        if (!inBatch) {
                            errors.add(err(lineNumber, NachaValidationError.ORPHAN_ENTRY_DETAIL,
                                "Entry Detail found outside a batch", line));
                        } else {
                            EntryDetailRecord ed = new EntryDetailRecord(line);
                            batchEntryCount++;
                            lastWasEntry = true;

                            if (config.isCheckAmounts()) {
                                long amount = parseLong(ed.amount, 0L);
                                if (isCredit(ed.transactionCode)) batchCreditTotal += amount;
                                else if (isDebit(ed.transactionCode)) batchDebitTotal += amount;
                            }
                            if (config.isCheckCounts()) {
                                batchEntryHash += parseLong(ed.receivingDFIRoutingNumber, 0L);
                            }
                        }
                        break;
                    }

                    // ----------------------------------------------------------------
                    case ADDENDA: {
                        if (!inBatch || !lastWasEntry) {
                            errors.add(err(lineNumber, NachaValidationError.ORPHAN_ADDENDA,
                                "Addenda record found outside a batch or not following an Entry Detail",
                                line));
                        } else {
                            batchAddendaCount++;
                            // lastWasEntry stays true — multiple addenda on one entry are valid
                        }
                        break;
                    }

                    // ----------------------------------------------------------------
                    case BATCH_CONTROL: {
                        if (!inBatch) {
                            errors.add(err(lineNumber, NachaValidationError.BATCH_CONTROL_NO_BATCH,
                                "Batch Control found without a preceding Batch Header", line));
                            break;
                        }
                        BatchControlRecord bc = new BatchControlRecord(line);

                        // batch number match
                        if (!batchHeaderNumber.equals(bc.batchNumber)) {
                            errors.add(err(lineNumber, NachaValidationError.BATCH_NUMBER_MISMATCH,
                                "Batch number mismatch: header had '" + batchHeaderNumber
                                    + "' but control has '" + bc.batchNumber + "'", line));
                        }

                        int expectedCount = batchEntryCount + batchAddendaCount;
                        if (config.isCheckCounts()) {
                            int actualCount = parseInt(bc.entryAddendaCount, -1);
                            if (actualCount != expectedCount) {
                                errors.add(err(lineNumber, NachaValidationError.ENTRY_COUNT_MISMATCH,
                                    "Entry/addenda count: expected " + expectedCount
                                        + " but batch control says " + actualCount, line));
                            }
                            long expectedHash = batchEntryHash % 10_000_000_000L;
                            long actualHash   = parseLong(bc.entryHash, -1L);
                            if (expectedHash != actualHash) {
                                errors.add(err(lineNumber, NachaValidationError.ENTRY_HASH_MISMATCH,
                                    "Entry hash: expected " + expectedHash
                                        + " but batch control says " + actualHash, line));
                            }
                        }

                        if (config.isCheckAmounts()) {
                            long actualDebit  = parseLong(bc.totalDebitAmount,  -1L);
                            long actualCredit = parseLong(bc.totalCreditAmount, -1L);
                            if (actualDebit != batchDebitTotal) {
                                errors.add(err(lineNumber, NachaValidationError.DEBIT_AMOUNT_MISMATCH,
                                    "Batch debit total: expected " + batchDebitTotal
                                        + " but batch control says " + actualDebit, line));
                            }
                            if (actualCredit != batchCreditTotal) {
                                errors.add(err(lineNumber, NachaValidationError.CREDIT_AMOUNT_MISMATCH,
                                    "Batch credit total: expected " + batchCreditTotal
                                        + " but batch control says " + actualCredit, line));
                            }
                        }

                        // roll up to file-level accumulators
                        totalBatchCount++;
                        totalEntryAddendaCount += expectedCount;
                        totalDebitTotal        += batchDebitTotal;
                        totalCreditTotal       += batchCreditTotal;
                        totalEntryHash         += batchEntryHash;

                        inBatch      = false;
                        lastWasEntry = false;
                        break;
                    }

                    // ----------------------------------------------------------------
                    case FILE_CONTROL: {
                        if (inBatch) {
                            errors.add(err(batchOpenLine, NachaValidationError.UNCLOSED_BATCH,
                                "Batch opened at line " + batchOpenLine
                                    + " was not closed before File Control", ""));
                        }
                        FileControlRecord fc = new FileControlRecord(line);
                        fileControlSeen = true;

                        if (config.isCheckCounts()) {
                            int actualBatches = parseInt(fc.batchCount, -1);
                            if (actualBatches != totalBatchCount) {
                                errors.add(err(lineNumber, NachaValidationError.FILE_BATCH_COUNT_MISMATCH,
                                    "File batch count: expected " + totalBatchCount
                                        + " but file control says " + actualBatches, line));
                            }
                            int actualEntries = parseInt(fc.entryAddendaCount, -1);
                            if (actualEntries != totalEntryAddendaCount) {
                                errors.add(err(lineNumber, NachaValidationError.FILE_ENTRY_COUNT_MISMATCH,
                                    "File entry/addenda count: expected " + totalEntryAddendaCount
                                        + " but file control says " + actualEntries, line));
                            }
                            long expectedHash = totalEntryHash % 10_000_000_000L;
                            long actualHash   = parseLong(fc.entryHash, -1L);
                            if (expectedHash != actualHash) {
                                errors.add(err(lineNumber, NachaValidationError.FILE_HASH_MISMATCH,
                                    "File entry hash: expected " + expectedHash
                                        + " but file control says " + actualHash, line));
                            }
                        }

                        if (config.isCheckAmounts()) {
                            long actualDebit  = parseLong(fc.totalDebitAmount,  -1L);
                            long actualCredit = parseLong(fc.totalCreditAmount, -1L);
                            if (actualDebit != totalDebitTotal) {
                                errors.add(err(lineNumber, NachaValidationError.FILE_DEBIT_MISMATCH,
                                    "File debit total: expected " + totalDebitTotal
                                        + " but file control says " + actualDebit, line));
                            }
                            if (actualCredit != totalCreditTotal) {
                                errors.add(err(lineNumber, NachaValidationError.FILE_CREDIT_MISMATCH,
                                    "File credit total: expected " + totalCreditTotal
                                        + " but file control says " + actualCredit, line));
                            }
                        }

                        // Stop after the first real File Control — everything after is padding
                        break;
                    }

                    // ----------------------------------------------------------------
                    case UNKNOWN:
                        errors.add(err(lineNumber, NachaValidationError.INVALID_RECORD_TYPE,
                            "Unknown record type discriminator: '" + line.charAt(0) + "'", line));
                        break;
                }
            }
        }

        // ---- post-file checks ----
        if (!fileHeaderSeen) {
            errors.add(new NachaValidationError(0, NachaValidationError.MISSING_FILE_HEADER,
                "No File Header (type 1) found in file", ""));
        }
        if (inBatch) {
            errors.add(new NachaValidationError(batchOpenLine, NachaValidationError.UNCLOSED_BATCH,
                "Batch opened at line " + batchOpenLine + " was never closed", ""));
        }
        if (!fileControlSeen) {
            errors.add(new NachaValidationError(lineNumber, NachaValidationError.MISSING_FILE_CONTROL,
                "No File Control (type 9) found in file", ""));
        }

        return errors;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static NachaValidationError err(int line, String code, String msg, String raw) {
        return new NachaValidationError(line, code, msg, raw);
    }

    /**
     * Credit transaction codes: second digit is '2', '3', or '4'.
     * Covers: 22/23/24 (demand), 32/33/34 (savings), 42/43/44 (GL), 52/53/54 (loan).
     */
    static boolean isCredit(String txCode) {
        if (txCode == null || txCode.length() < 2) return false;
        char c = txCode.charAt(1);
        return c == '2' || c == '3' || c == '4';
    }

    /**
     * Debit transaction codes: second digit is '5', '7', '8', or '9'.
     * Covers: 27/28/29 (demand), 37/38/39 (savings), 47/48/49 (GL), 55 (loan).
     */
    static boolean isDebit(String txCode) {
        if (txCode == null || txCode.length() < 2) return false;
        char c = txCode.charAt(1);
        return c == '7' || c == '8' || c == '9' || c == '5';
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static long parseLong(String s, long fallback) {
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
