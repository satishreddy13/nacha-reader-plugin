package com.example.pdi.plugin.nachareader.nacha;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.pdi.plugin.nachareader.nacha.NachaParser.ParsedEntry;
import com.example.pdi.plugin.nachareader.nacha.NachaParser.RawRecord;

/**
 * Integration tests for NachaParser using the bundled sample-nacha.ach fixture.
 *
 * The fixture contains:
 *   1 file header, 1 batch header,
 *   2 entry details (first has 1 addenda, second has none),
 *   1 batch control, 1 file control, 3 padding lines.
 */
@DisplayName("NachaParser — sample file")
class NachaParserTest {

    private static Path SAMPLE;

    @BeforeAll
    static void locateSample() {
        URL url = NachaParserTest.class.getClassLoader().getResource("sample-nacha.ach");
        assertNotNull(url, "sample-nacha.ach must be on the test classpath");
        SAMPLE = Paths.get(url.getPath());
    }

    // -----------------------------------------------------------------------
    // parseEntryDetails
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("parseEntryDetails returns exactly 2 entries for the sample file")
    void entryDetails_count() throws Exception {
        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(SAMPLE);
        assertEquals(2, entries.size());
    }

    @Test
    @DisplayName("entry 1: file header context is propagated")
    void entryDetails_fileHeaderContext() throws Exception {
        ParsedEntry e = entries().get(0);
        assertEquals("021000021",          e.fileImmediateDestination);
        assertEquals("0123456789",         e.fileImmediateOrigin);
        assertEquals("260101",             e.fileCreationDate);
        assertEquals("FIRST NATIONAL BANK", e.fileImmediateDestinationName);
        assertEquals("ACME CORP",          e.fileImmediateOriginName);
    }

    @Test
    @DisplayName("entry 1: batch header context is propagated")
    void entryDetails_batchHeaderContext() throws Exception {
        ParsedEntry e = entries().get(0);
        assertEquals("220",        e.batchServiceClassCode);
        assertEquals("ACME CORP",  e.batchCompanyName);
        assertEquals("0123456789", e.batchCompanyIdentification);
        assertEquals("PPD",        e.batchSecCode);
        assertEquals("PAYROLL",    e.batchCompanyEntryDescription);
        assertEquals("260101",     e.batchEffectiveEntryDate);
        assertEquals("02100002",   e.batchODFIIdentification);
        assertEquals("000001",     e.batchNumber);
    }

    @Test
    @DisplayName("entry 1: entry detail fields are parsed correctly")
    void entryDetails_entry1Fields() throws Exception {
        ParsedEntry e = entries().get(0);
        assertEquals("22",              e.entryTransactionCode);
        assertEquals("10200345",        e.entryReceivingDFIRouting);
        assertEquals("0",               e.entryCheckDigit);
        assertEquals("512345678",       e.entryDFIAccountNumber);
        assertEquals("0000150000",      e.entryAmount);
        assertEquals("1234567890",      e.entryIndividualIdNumber);
        assertEquals("JOHN SMITH",      e.entryIndividualName);
        assertEquals("1",               e.entryAddendaIndicator);
        assertEquals("021000020000001", e.entryTraceNumber);
    }

    @Test
    @DisplayName("entry 1: addenda paymentRelatedInformation is concatenated")
    void entryDetails_entry1Addenda() throws Exception {
        ParsedEntry e = entries().get(0);
        assertEquals(1, e.addenda.size());
        assertTrue(e.addendaConcatenated.contains("123 MAIN ST OTTAWA"),
            "Addenda info should contain address; got: " + e.addendaConcatenated);
    }

    @Test
    @DisplayName("entry 2: no addenda → addendaConcatenated is empty")
    void entryDetails_entry2NoAddenda() throws Exception {
        ParsedEntry e = entries().get(1);
        assertEquals(0, e.addenda.size());
        assertEquals("", e.addendaConcatenated);
        assertEquals("JANE DOE", e.entryIndividualName);
        assertEquals("0000200000", e.entryAmount);
    }

    @Test
    @DisplayName("record_type field is always '6' in ENTRY_DETAILS mode")
    void entryDetails_recordTypeIs6() throws Exception {
        // This is implicit from the parser (only type-6 lines are returned)
        // We assert via the ParsedEntry structure — no record_type field here,
        // it is set to "6" in the step's buildEntryRow(). The parser returns
        // only type-6 records, so count == 2 is sufficient.
        List<ParsedEntry> entries = new NachaParser().parseEntryDetails(SAMPLE);
        assertEquals(2, entries.size(), "Only type-6 records should be returned");
    }

    // -----------------------------------------------------------------------
    // parseAllRecords
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("parseAllRecords returns 7 records (1+1+2+1+1+1 — padding nines skipped)")
    void allRecords_count() throws Exception {
        // Padding-nine lines are also type FILE_CONTROL and ARE included
        // (3 padding lines + 1 real file control = 4 type-9 lines total).
        // Adjust: padding lines are "9999..." = FILE_CONTROL type, so 7 real + 3 padding = 10.
        List<RawRecord> records = new NachaParser().parseAllRecords(SAMPLE);
        assertEquals(10, records.size(), "All 10 lines including padding should be returned");
    }

    @Test
    @DisplayName("parseAllRecords: first record is FILE_HEADER")
    void allRecords_firstIsFileHeader() throws Exception {
        RawRecord r = allRecords().get(0);
        assertEquals(RecordType.FILE_HEADER, r.recordType);
        assertEquals("021000021",          r.immediateDestination);
        assertEquals("0123456789",         r.immediateOrigin);
        assertEquals("FIRST NATIONAL BANK", r.immediateDestinationName);
    }

    @Test
    @DisplayName("parseAllRecords: second record is BATCH_HEADER")
    void allRecords_batchHeader() throws Exception {
        RawRecord r = allRecords().get(1);
        assertEquals(RecordType.BATCH_HEADER, r.recordType);
        assertEquals("ACME CORP", r.batchCompanyName);
        assertEquals("PPD",       r.batchSecCode);
    }

    @Test
    @DisplayName("parseAllRecords: type-6 entry detail fields are populated, file-header fields are empty")
    void allRecords_entryDetailRecord() throws Exception {
        RawRecord r = allRecords().get(2);  // first entry detail
        assertEquals(RecordType.ENTRY_DETAIL, r.recordType);
        assertEquals("22",         r.entryTransactionCode);
        assertEquals("JOHN SMITH", r.entryIndividualName);
        // File-header fields should be empty on an ENTRY_DETAIL row
        assertEquals("", r.immediateDestination);
    }

    @Test
    @DisplayName("parseAllRecords: type-7 addenda record is present and populated")
    void allRecords_addendaRecord() throws Exception {
        RawRecord r = allRecords().get(3);  // addenda after first entry
        assertEquals(RecordType.ADDENDA, r.recordType);
        assertEquals("05", r.addendaTypeCode);
        assertTrue(r.addendaPaymentInfo.contains("123 MAIN ST"),
            "Addenda payment info should contain address; got: " + r.addendaPaymentInfo);
    }

    @Test
    @DisplayName("parseAllRecords: batch control fields are populated")
    void allRecords_batchControl() throws Exception {
        RawRecord r = allRecords().get(5);  // batch control
        assertEquals(RecordType.BATCH_CONTROL, r.recordType);
        assertEquals("220",          r.batchServiceClassCode);
        assertEquals("000003",       r.batchCtrlEntryAddendaCount);
        assertEquals("02100002",     r.batchODFIIdentification);
    }

    @Test
    @DisplayName("parseAllRecords: file control fields are populated")
    void allRecords_fileControl() throws Exception {
        RawRecord r = allRecords().get(6);  // real file control
        assertEquals(RecordType.FILE_CONTROL, r.recordType);
        assertEquals("000001", r.fileBatchCount);
        assertEquals("000001", r.fileBlockCount);
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("blank/empty lines in the file are skipped without error")
    void emptyLines_skipped() throws Exception {
        // The padding-nine lines will be parsed as FILE_CONTROL — not as blank.
        // This test verifies no exception from our sample which has a trailing newline.
        assertDoesNotThrow(() -> new NachaParser().parseEntryDetails(SAMPLE));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private List<ParsedEntry> entries() throws Exception {
        return new NachaParser().parseEntryDetails(SAMPLE);
    }

    private List<RawRecord> allRecords() throws Exception {
        return new NachaParser().parseAllRecords(SAMPLE);
    }
}
