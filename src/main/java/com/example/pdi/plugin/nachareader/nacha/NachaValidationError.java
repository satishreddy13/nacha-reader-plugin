package com.example.pdi.plugin.nachareader.nacha;

/**
 * A single structural validation finding.
 * {@code lineNumber} is 1-based; 0 means the error is at file level (not tied to a specific line).
 */
public class NachaValidationError {

    // ---- Error codes ----
    public static final String LINE_LENGTH              = "LINE_LENGTH";
    public static final String INVALID_RECORD_TYPE      = "INVALID_RECORD_TYPE";
    public static final String MISSING_FILE_HEADER      = "MISSING_FILE_HEADER";
    public static final String MISSING_FILE_CONTROL     = "MISSING_FILE_CONTROL";
    public static final String DUPLICATE_FILE_HEADER    = "DUPLICATE_FILE_HEADER";
    public static final String ORPHAN_ENTRY_DETAIL      = "ORPHAN_ENTRY_DETAIL";
    public static final String ORPHAN_ADDENDA           = "ORPHAN_ADDENDA";
    public static final String UNCLOSED_BATCH           = "UNCLOSED_BATCH";
    public static final String BATCH_CONTROL_NO_BATCH   = "BATCH_CONTROL_NO_BATCH";
    public static final String BATCH_NUMBER_MISMATCH    = "BATCH_NUMBER_MISMATCH";
    public static final String ENTRY_COUNT_MISMATCH     = "ENTRY_COUNT_MISMATCH";
    public static final String ENTRY_HASH_MISMATCH      = "ENTRY_HASH_MISMATCH";
    public static final String DEBIT_AMOUNT_MISMATCH    = "DEBIT_AMOUNT_MISMATCH";
    public static final String CREDIT_AMOUNT_MISMATCH   = "CREDIT_AMOUNT_MISMATCH";
    public static final String FILE_BATCH_COUNT_MISMATCH  = "FILE_BATCH_COUNT_MISMATCH";
    public static final String FILE_ENTRY_COUNT_MISMATCH  = "FILE_ENTRY_COUNT_MISMATCH";
    public static final String FILE_HASH_MISMATCH         = "FILE_HASH_MISMATCH";
    public static final String FILE_DEBIT_MISMATCH        = "FILE_DEBIT_MISMATCH";
    public static final String FILE_CREDIT_MISMATCH       = "FILE_CREDIT_MISMATCH";
    public static final String HEADER_DESTINATION_MISMATCH = "HEADER_DESTINATION_MISMATCH";
    public static final String HEADER_ORIGIN_MISMATCH     = "HEADER_ORIGIN_MISMATCH";
    public static final String HEADER_COMPANY_MISMATCH    = "HEADER_COMPANY_MISMATCH";
    public static final String HEADER_SEC_CODE_MISMATCH   = "HEADER_SEC_CODE_MISMATCH";

    // ---- Fields ----
    public final int    lineNumber; // 1-based; 0 = file-level
    public final String errorCode;
    public final String message;
    public final String rawLine;    // the offending line, or ""

    public NachaValidationError(int lineNumber, String errorCode, String message, String rawLine) {
        this.lineNumber = lineNumber;
        this.errorCode  = errorCode;
        this.message    = message;
        this.rawLine    = rawLine != null ? rawLine : "";
    }

    @Override
    public String toString() {
        return "[" + errorCode + "] line " + lineNumber + ": " + message;
    }
}
