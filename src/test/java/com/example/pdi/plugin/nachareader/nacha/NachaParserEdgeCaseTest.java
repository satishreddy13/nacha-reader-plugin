package com.example.pdi.plugin.nachareader.nacha;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.pdi.plugin.nachareader.nacha.NachaParser.ParsedEntry;
import com.example.pdi.plugin.nachareader.nacha.NachaParser.RawRecord;

/** Edge-case and error-path tests for NachaParser. */
@DisplayName("NachaParser — edge cases")
class NachaParserEdgeCaseTest {

    @TempDir
    Path tempDir;

    // ---- Helpers ----

    private static String pad(String s, int len) {
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
    }

    private static String fileHeader() {
        return "1" + "01" + " 021000021" + "0123456789" + "260101" + "    " + "A" +
               "094" + "10" + "1" + pad("DEST BANK", 23) + pad("ORIGIN CO", 23) + pad("", 8);
    }

    private static String batchHeader() {
        return "5" + "220" + pad("COMPANY", 16) + pad("", 20) + pad("1111111111", 10) +
               "PPD" + pad("PAYROLL", 10) + "260101" + "260101" + "   " + "1" +
               "02100002" + " " + "000001";
    }

    private static String entryDetail(String name) {
        return "6" + "22" + "10200345" + "0" + pad("123456789", 17) +
               "0000100000" + pad("ID-001", 15) + pad(name, 22) + "  " + "0" +
               pad("021000020000001", 15);
    }

    private static String batchControl() {
        return "8" + "220" + "000001" + "0010200345" + "000000000000" + "000000100000" +
               pad("1111111111", 10) + pad("", 19) + pad("", 6) + "02100002" + " " + "000001";
    }

    private static String fileControl() {
        return "9" + "000001" + "000001" + "00000001" + "0010200345" +
               "000000000000" + "000000100000" + pad("", 39);
    }

    private Path writeLines(String... lines) throws IOException {
        Path f = tempDir.resolve("test.ach");
        Files.write(f, List.of(lines), StandardCharsets.US_ASCII);
        return f;
    }

    // -----------------------------------------------------------------------

    @Test
    @DisplayName("empty file returns no entries and no records")
    void emptyFile() throws Exception {
        Path f = tempDir.resolve("empty.ach");
        Files.createFile(f);
        assertEquals(0, new NachaParser().parseEntryDetails(f).size());
        assertEquals(0, new NachaParser().parseAllRecords(f).size());
    }

    @Test
    @DisplayName("file with only blank lines returns empty results")
    void blankLinesOnly() throws Exception {
        Path f = writeLines("", "   ", "");
        assertEquals(0, new NachaParser().parseEntryDetails(f).size());
    }

    @Test
    @DisplayName("line that is not 94 characters throws IOException")
    void invalidLineLength_throwsIOException() throws Exception {
        Path f = writeLines(
            fileHeader(),
            batchHeader(),
            "6" + "22" + "short"  // obviously not 94 chars
        );
        assertThrows(IOException.class, () -> new NachaParser().parseEntryDetails(f));
    }

    @Test
    @DisplayName("entry detail before any file header: file header fields default to empty")
    void entryWithoutFileHeader() throws Exception {
        Path f = writeLines(
            batchHeader(),
            entryDetail("NO HEADER"),
            batchControl()
        );
        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(f);
        assertEquals(1, entries.size());
        assertEquals("", entries.get(0).fileImmediateDestination,
            "fileImmediateDestination should default to empty when no file header");
    }

    @Test
    @DisplayName("entry detail before any batch header: batch fields default to empty")
    void entryWithoutBatchHeader() throws Exception {
        Path f = writeLines(
            fileHeader(),
            entryDetail("NO BATCH"),
            batchControl()
        );
        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(f);
        assertEquals(1, entries.size());
        assertEquals("", entries.get(0).batchCompanyName,
            "batchCompanyName should default to empty when no batch header");
    }

    @Test
    @DisplayName("multiple batches in one file: entries from each batch carry their own batch context")
    void multipleBatches() throws Exception {
        String bh2 = "5" + "200" + pad("SECOND CO", 16) + pad("", 20) + pad("2222222222", 10) +
                     "CCD" + pad("EXPENSES", 10) + "260101" + "260101" + "   " + "1" +
                     "02100002" + " " + "000002";
        String bc2 = "8" + "200" + "000001" + "0010200345" + "000000100000" + "000000000000" +
                     pad("2222222222", 10) + pad("", 19) + pad("", 6) + "02100002" + " " + "000002";
        String fc2 = "9" + "000002" + "000001" + "00000002" + "0020400690" +
                     "000000100000" + "000000100000" + pad("", 39);

        Path f = writeLines(
            fileHeader(),
            batchHeader(),       // batch 1 — COMPANY / PPD
            entryDetail("ALICE"),
            batchControl(),
            bh2,                 // batch 2 — SECOND CO / CCD
            entryDetail("BOB"),
            bc2,
            fc2
        );

        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(f);
        assertEquals(2, entries.size());
        assertEquals("COMPANY",   entries.get(0).batchCompanyName);
        assertEquals("PPD",       entries.get(0).batchSecCode);
        assertEquals("SECOND CO", entries.get(1).batchCompanyName);
        assertEquals("CCD",       entries.get(1).batchSecCode);
    }

    @Test
    @DisplayName("multiple addenda on one entry are concatenated with pipe separator")
    void multipleAddenda_concatenatedWithPipe() throws Exception {
        String addenda1 = "7" + "05" + pad("FIRST ADDENDA INFO", 80) + "0001" + "0000001";
        String addenda2 = "7" + "05" + pad("SECOND ADDENDA INFO", 80) + "0002" + "0000001";
        String entryWithAddenda = "6" + "22" + "10200345" + "0" + pad("123456789", 17) +
               "0000100000" + pad("ID-001", 15) + pad("TEST PERSON", 22) + "  " + "1" +
               pad("021000020000001", 15);

        Path f = writeLines(
            fileHeader(), batchHeader(),
            entryWithAddenda, addenda1, addenda2,
            batchControl(), fileControl()
        );

        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(f);
        assertEquals(1, entries.size());
        assertEquals(2, entries.get(0).addenda.size());
        assertTrue(entries.get(0).addendaConcatenated.contains("|"),
            "Multiple addenda should be separated by '|'");
        assertTrue(entries.get(0).addendaConcatenated.contains("FIRST ADDENDA INFO"));
        assertTrue(entries.get(0).addendaConcatenated.contains("SECOND ADDENDA INFO"));
    }

    @Test
    @DisplayName("field trimming: leading/trailing spaces are removed from parsed values")
    void fieldTrimming() throws Exception {
        // companyName field (pos 5-20) = "ACME CORP       " → should trim to "ACME CORP"
        Path f = writeLines(
            fileHeader(), batchHeader(),
            entryDetail("TRIM TEST"),
            batchControl(), fileControl()
        );
        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(f);
        assertEquals("COMPANY", entries.get(0).batchCompanyName,
            "Company name should be trimmed of padding spaces");
    }

    @Test
    @DisplayName("nine-filled padding records do not generate spurious entry rows")
    void paddingNines_notIncludedInEntryDetails() throws Exception {
        String padding = "9".repeat(94);
        Path f = writeLines(
            fileHeader(), batchHeader(),
            entryDetail("SINGLE ENTRY"),
            batchControl(), fileControl(),
            padding, padding, padding
        );
        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(f);
        assertEquals(1, entries.size(), "Padding nines must not generate entry rows");
    }
}
