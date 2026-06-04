package com.example.pdi.plugin.nachareader.nacha;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("NachaValidator")
class NachaValidatorTest {

    @TempDir Path tempDir;

    private static Path SAMPLE;

    @BeforeAll
    static void locateSample() {
        URL url = NachaValidatorTest.class.getClassLoader().getResource("sample-nacha.ach");
        assertNotNull(url, "sample-nacha.ach must be on the test classpath");
        SAMPLE = Paths.get(url.getPath());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String pad(String s, int len) {
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
    }

    private static String fileHeader(String dest, String origin) {
        return "1" + "01" + " " + pad(dest, 9) + pad(origin, 10)
             + "260101" + "    " + "A" + "094" + "10" + "1"
             + pad("DEST BANK", 23) + pad("ORIGIN CO", 23) + pad("", 8);
    }
    private static String fileHeader() { return fileHeader("021000021", "0123456789"); }

    private static String batchHeader(String company, String sec, String batchNum) {
        return "5" + "220" + pad(company, 16) + pad("", 20) + pad("1111111111", 10)
             + sec + pad("PAYROLL", 10) + "260101" + "260101" + "   " + "1"
             + "02100002" + " " + pad(batchNum, 6);
    }
    private static String batchHeader() { return batchHeader("COMPANY", "PPD", "000001"); }

    private static String entryDetail(String routing, String amount, String txCode) {
        return "6" + txCode + routing + "0" + pad("123456789", 17)
             + pad(amount, 10) + pad("ID-001", 15) + pad("TEST PERSON", 22) + "  " + "0"
             + pad("021000020000001", 15);
    }
    private static String creditEntry()  { return entryDetail("10200345", "0000100000", "22"); }
    private static String debitEntry()   { return entryDetail("10200345", "0000050000", "27"); }

    private static String batchControl(String entryCount, String hash,
            String debit, String credit, String batchNum) {
        return "8" + "220" + pad(entryCount, 6) + pad(hash, 10)
             + pad(debit, 12) + pad(credit, 12)
             + pad("1111111111", 10) + pad("", 19) + pad("", 6)
             + "02100002" + " " + pad(batchNum, 6);
    }

    private static String fileControl(String batches, String blocks, String entryCount,
            String hash, String debit, String credit) {
        return "9" + pad(batches, 6) + pad(blocks, 6) + pad(entryCount, 8)
             + pad(hash, 10) + pad(debit, 12) + pad(credit, 12) + pad("", 39);
    }

    private static String padding() { return "9".repeat(94); }

    private Path write(String... lines) throws IOException {
        Path f = tempDir.resolve("test.ach");
        Files.write(f, List.of(lines), StandardCharsets.US_ASCII);
        return f;
    }

    private static NachaValidationConfig defaultConfig() {
        return new NachaValidationConfig();
    }

    private List<NachaValidationError> validate(Path f) throws IOException {
        return new NachaValidator().validate(f, defaultConfig());
    }

    private static void assertNoErrors(List<NachaValidationError> errors) {
        if (!errors.isEmpty()) fail("Expected no errors but got: " + errors);
    }

    private static void assertHasCode(List<NachaValidationError> errors, String code) {
        assertTrue(errors.stream().anyMatch(e -> code.equals(e.errorCode)),
            "Expected error code '" + code + "' but got: " + errors);
    }

    // -----------------------------------------------------------------------
    // PASS cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("valid sample file returns no errors (PASS)")
    void validFile_noErrors() throws Exception {
        assertNoErrors(validate(SAMPLE));
    }

    @Test
    @DisplayName("minimal valid file (1 batch, 1 credit entry, correct totals) passes")
    void minimalValidFile_passes() throws Exception {
        // routing 10200345, amount 100.00 → credit
        // hash = 10200345, credit total = 0000100000
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            batchControl("000001", "0010200345", "000000000000", "000000100000", "000001"),
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000000000000", "000000100000"),
            padding(), padding(), padding(), padding(), padding()
        );
        assertNoErrors(validate(f));
    }

    @Test
    @DisplayName("file with both debit and credit entries passes when totals match")
    void mixedEntries_matchingTotals_passes() throws Exception {
        // credit 100.00, debit 50.00 → hash = 10200345 * 2, credit=100000, debit=50000
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            debitEntry(),
            batchControl("000002", "0020400690", "000000050000", "000000100000", "000001"),
            fileControl("000001", "000001", "00000002", "0020400690",
                        "000000050000", "000000100000"),
            padding(), padding(), padding(), padding()
        );
        assertNoErrors(validate(f));
    }

    // -----------------------------------------------------------------------
    // Structural errors
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("line not 94 chars produces LINE_LENGTH error")
    void invalidLineLength_error() throws Exception {
        Path f = write(fileHeader(), batchHeader(), "6too short");
        assertHasCode(validate(f), NachaValidationError.LINE_LENGTH);
    }

    @Test
    @DisplayName("file with no File Header produces MISSING_FILE_HEADER")
    void missingFileHeader_error() throws Exception {
        Path f = write(
            batchHeader(),
            creditEntry(),
            batchControl("000001", "0010200345", "000000000000", "000000100000", "000001"),
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000000000000", "000000100000")
        );
        assertHasCode(validate(f), NachaValidationError.MISSING_FILE_HEADER);
    }

    @Test
    @DisplayName("file with no File Control produces MISSING_FILE_CONTROL")
    void missingFileControl_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            batchControl("000001", "0010200345", "000000000000", "000000100000", "000001")
        );
        assertHasCode(validate(f), NachaValidationError.MISSING_FILE_CONTROL);
    }

    @Test
    @DisplayName("Entry Detail outside a batch produces ORPHAN_ENTRY_DETAIL")
    void orphanEntryDetail_error() throws Exception {
        Path f = write(fileHeader(), creditEntry());
        assertHasCode(validate(f), NachaValidationError.ORPHAN_ENTRY_DETAIL);
    }

    @Test
    @DisplayName("Addenda record not following Entry Detail produces ORPHAN_ADDENDA")
    void orphanAddenda_error() throws Exception {
        String addenda = "7" + "05" + pad("SOME INFO", 80) + "0001" + "0000001";
        Path f = write(fileHeader(), batchHeader(), addenda);
        assertHasCode(validate(f), NachaValidationError.ORPHAN_ADDENDA);
    }

    @Test
    @DisplayName("Batch Header without matching Batch Control produces UNCLOSED_BATCH")
    void unclosedBatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry()
            // no batch control
        );
        assertHasCode(validate(f), NachaValidationError.UNCLOSED_BATCH);
    }

    @Test
    @DisplayName("batch number mismatch between type-5 and type-8 produces BATCH_NUMBER_MISMATCH")
    void batchNumberMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader("COMPANY", "PPD", "000001"),   // opens batch 000001
            creditEntry(),
            batchControl("000001", "0010200345", "000000000000", "000000100000", "000099"), // closes 000099
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000000000000", "000000100000")
        );
        assertHasCode(validate(f), NachaValidationError.BATCH_NUMBER_MISMATCH);
    }

    // -----------------------------------------------------------------------
    // Count checks
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("wrong entry/addenda count in batch control produces ENTRY_COUNT_MISMATCH")
    void entryCountMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            // claims 2 entries but only 1 is present
            batchControl("000002", "0010200345", "000000000000", "000000100000", "000001"),
            fileControl("000001", "000001", "00000002", "0010200345",
                        "000000000000", "000000100000")
        );
        assertHasCode(validate(f), NachaValidationError.ENTRY_COUNT_MISMATCH);
    }

    @Test
    @DisplayName("wrong entry hash in batch control produces ENTRY_HASH_MISMATCH")
    void entryHashMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            // routing is 10200345, so hash should be 0010200345, not 0099999999
            batchControl("000001", "0099999999", "000000000000", "000000100000", "000001"),
            fileControl("000001", "000001", "00000001", "0099999999",
                        "000000000000", "000000100000")
        );
        assertHasCode(validate(f), NachaValidationError.ENTRY_HASH_MISMATCH);
    }

    @Test
    @DisplayName("count checks disabled: wrong count does not produce an error")
    void countChecks_disabled_noError() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            batchControl("000099", "0099999999", "000000000000", "000000100000", "000001"),
            fileControl("000001", "000001", "00000099", "0099999999",
                        "000000000000", "000000100000")
        );
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        List<NachaValidationError> errors = new NachaValidator().validate(f, cfg);
        assertNoErrors(errors);
    }

    // -----------------------------------------------------------------------
    // Amount checks
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("wrong credit total in batch control produces CREDIT_AMOUNT_MISMATCH")
    void creditAmountMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),  // amount = 100.00 = 0000100000
            // reports 200.00 instead
            batchControl("000001", "0010200345", "000000000000", "000000200000", "000001"),
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000000000000", "000000200000")
        );
        assertHasCode(validate(f), NachaValidationError.CREDIT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("wrong debit total in batch control produces DEBIT_AMOUNT_MISMATCH")
    void debitAmountMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            debitEntry(),   // amount = 50.00 = 0000050000
            // reports 100.00 instead
            batchControl("000001", "0010200345", "000000100000", "000000000000", "000001"),
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000000100000", "000000000000")
        );
        assertHasCode(validate(f), NachaValidationError.DEBIT_AMOUNT_MISMATCH);
    }

    @Test
    @DisplayName("wrong file-level credit total produces FILE_CREDIT_MISMATCH")
    void fileCreditMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            batchControl("000001", "0010200345", "000000000000", "000000100000", "000001"),
            // file control reports wrong credit total
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000000000000", "000000999999"),
            padding(), padding(), padding(), padding(), padding()
        );
        assertHasCode(validate(f), NachaValidationError.FILE_CREDIT_MISMATCH);
    }

    @Test
    @DisplayName("wrong file batch count produces FILE_BATCH_COUNT_MISMATCH")
    void fileBatchCountMismatch_error() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            batchControl("000001", "0010200345", "000000000000", "000000100000", "000001"),
            // claims 5 batches but only 1
            fileControl("000005", "000001", "00000001", "0010200345",
                        "000000000000", "000000100000"),
            padding(), padding(), padding(), padding(), padding()
        );
        assertHasCode(validate(f), NachaValidationError.FILE_BATCH_COUNT_MISMATCH);
    }

    @Test
    @DisplayName("amount checks disabled: wrong totals do not produce an error")
    void amountChecks_disabled_noError() throws Exception {
        Path f = write(
            fileHeader(),
            batchHeader(),
            creditEntry(),
            batchControl("000001", "0010200345", "000099999999", "000099999999", "000001"),
            fileControl("000001", "000001", "00000001", "0010200345",
                        "000099999999", "000099999999")
        );
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertNoErrors(new NachaValidator().validate(f, cfg));
    }

    // -----------------------------------------------------------------------
    // Header config checks
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("matching expected immediate destination passes")
    void headerDestination_match_passes() throws Exception {
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setExpectedImmediateDestination("021000021");
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertNoErrors(new NachaValidator().validate(SAMPLE, cfg));
    }

    @Test
    @DisplayName("wrong expected immediate destination produces HEADER_DESTINATION_MISMATCH")
    void headerDestination_mismatch_error() throws Exception {
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setExpectedImmediateDestination("999999999");
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertHasCode(new NachaValidator().validate(SAMPLE, cfg),
            NachaValidationError.HEADER_DESTINATION_MISMATCH);
    }

    @Test
    @DisplayName("matching expected company name passes")
    void headerCompanyName_match_passes() throws Exception {
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setExpectedCompanyName("ACME CORP");
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertNoErrors(new NachaValidator().validate(SAMPLE, cfg));
    }

    @Test
    @DisplayName("wrong company name produces HEADER_COMPANY_MISMATCH")
    void headerCompanyName_mismatch_error() throws Exception {
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setExpectedCompanyName("WRONG COMPANY");
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertHasCode(new NachaValidator().validate(SAMPLE, cfg),
            NachaValidationError.HEADER_COMPANY_MISMATCH);
    }

    @Test
    @DisplayName("wrong SEC code produces HEADER_SEC_CODE_MISMATCH")
    void headerSecCode_mismatch_error() throws Exception {
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setExpectedSecCode("CCD");  // file has PPD
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertHasCode(new NachaValidator().validate(SAMPLE, cfg),
            NachaValidationError.HEADER_SEC_CODE_MISMATCH);
    }

    @Test
    @DisplayName("empty expected header values are skipped (no false positives)")
    void emptyHeaderConfig_noFalsePositives() throws Exception {
        NachaValidationConfig cfg = new NachaValidationConfig();
        cfg.setExpectedImmediateDestination("");
        cfg.setExpectedImmediateOrigin("");
        cfg.setExpectedCompanyName("");
        cfg.setExpectedSecCode("");
        cfg.setCheckCounts(false);
        cfg.setCheckAmounts(false);
        assertNoErrors(new NachaValidator().validate(SAMPLE, cfg));
    }

    // -----------------------------------------------------------------------
    // Transaction code classification
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("isCredit() returns true for codes 22, 32, 42, 52")
    void isCredit_correctCodes() {
        assertTrue(NachaValidator.isCredit("22"));
        assertTrue(NachaValidator.isCredit("32"));
        assertTrue(NachaValidator.isCredit("42"));
        assertTrue(NachaValidator.isCredit("52"));
        assertFalse(NachaValidator.isCredit("27"));
        assertFalse(NachaValidator.isCredit("37"));
    }

    @Test
    @DisplayName("isDebit() returns true for codes 27, 37, 47, 55")
    void isDebit_correctCodes() {
        assertTrue(NachaValidator.isDebit("27"));
        assertTrue(NachaValidator.isDebit("37"));
        assertTrue(NachaValidator.isDebit("47"));
        assertTrue(NachaValidator.isDebit("55"));
        assertFalse(NachaValidator.isDebit("22"));
        assertFalse(NachaValidator.isDebit("32"));
    }
}
