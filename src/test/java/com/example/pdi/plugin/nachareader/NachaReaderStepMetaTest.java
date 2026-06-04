package com.example.pdi.plugin.nachareader;

import static org.junit.jupiter.api.Assertions.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pentaho.di.core.row.RowMeta;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@DisplayName("NachaReaderStepMeta")
class NachaReaderStepMetaTest {

    // -----------------------------------------------------------------------
    // Defaults
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("setDefault() produces 'file_path' as the filePathField")
    void defaults_filePathField() {
        NachaReaderStepMeta meta = new NachaReaderStepMeta();
        meta.setDefault();
        assertEquals("file_path", meta.getFilePathField());
    }

    @Test
    @DisplayName("setDefault() produces ENTRY_DETAILS as the outputMode")
    void defaults_outputMode() {
        NachaReaderStepMeta meta = new NachaReaderStepMeta();
        meta.setDefault();
        assertEquals(NachaReaderStepMeta.OutputMode.ENTRY_DETAILS, meta.getOutputMode());
    }

    // -----------------------------------------------------------------------
    // XML round-trip
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getXML / loadXML round-trip preserves filePathField")
    void xmlRoundTrip_filePathField() throws Exception {
        NachaReaderStepMeta original = defaultMeta();
        original.setFilePathField("ach_file");

        NachaReaderStepMeta loaded = roundTrip(original);
        assertEquals("ach_file", loaded.getFilePathField());
    }

    @Test
    @DisplayName("getXML / loadXML round-trip preserves outputMode ALL_RECORDS")
    void xmlRoundTrip_outputMode() throws Exception {
        NachaReaderStepMeta original = defaultMeta();
        original.setOutputMode(NachaReaderStepMeta.OutputMode.ALL_RECORDS);

        NachaReaderStepMeta loaded = roundTrip(original);
        assertEquals(NachaReaderStepMeta.OutputMode.ALL_RECORDS, loaded.getOutputMode());
    }

    // -----------------------------------------------------------------------
    // Clone
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("clone() returns a distinct object")
    void clone_isDistinct() {
        NachaReaderStepMeta meta = defaultMeta();
        NachaReaderStepMeta clone = (NachaReaderStepMeta) meta.clone();
        assertNotSame(meta, clone);
    }

    @Test
    @DisplayName("clone() copies field values")
    void clone_copiesValues() {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setFilePathField("my_path");
        meta.setOutputMode(NachaReaderStepMeta.OutputMode.ALL_RECORDS);

        NachaReaderStepMeta clone = (NachaReaderStepMeta) meta.clone();
        assertEquals("my_path", clone.getFilePathField());
        assertEquals(NachaReaderStepMeta.OutputMode.ALL_RECORDS, clone.getOutputMode());
    }

    @Test
    @DisplayName("mutating clone does not affect original")
    void clone_mutationIsIndependent() {
        NachaReaderStepMeta meta = defaultMeta();
        NachaReaderStepMeta clone = (NachaReaderStepMeta) meta.clone();
        clone.setFilePathField("changed");
        assertEquals("file_path", meta.getFilePathField());
    }

    // -----------------------------------------------------------------------
    // getFields — ENTRY_DETAILS mode
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getFields ENTRY_DETAILS: output row includes nacha_entry_amount field")
    void getFields_entryDetails_containsAmountField() throws Exception {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setOutputMode(NachaReaderStepMeta.OutputMode.ENTRY_DETAILS);

        RowMeta rowMeta = new RowMeta();
        meta.getFields(rowMeta, "step", null, null, null, null, null);

        boolean found = false;
        for (String name : rowMeta.getFieldNames()) {
            if ("nacha_entry_amount".equals(name)) { found = true; break; }
        }
        assertTrue(found, "nacha_entry_amount should be present in ENTRY_DETAILS output schema");
    }

    @Test
    @DisplayName("getFields ENTRY_DETAILS: outputs 28 NACHA fields")
    void getFields_entryDetails_fieldCount() throws Exception {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setOutputMode(NachaReaderStepMeta.OutputMode.ENTRY_DETAILS);

        RowMeta rowMeta = new RowMeta();
        meta.getFields(rowMeta, "step", null, null, null, null, null);
        assertEquals(28, rowMeta.getFieldNames().length);
    }

    // -----------------------------------------------------------------------
    // getFields — ALL_RECORDS mode
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getFields ALL_RECORDS: outputs 41 NACHA fields")
    void getFields_allRecords_fieldCount() throws Exception {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setOutputMode(NachaReaderStepMeta.OutputMode.ALL_RECORDS);

        RowMeta rowMeta = new RowMeta();
        meta.getFields(rowMeta, "step", null, null, null, null, null);
        assertEquals(41, rowMeta.getFieldNames().length);
    }

    @Test
    @DisplayName("getFields ALL_RECORDS: includes batch control and file control fields")
    void getFields_allRecords_includesControlFields() throws Exception {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setOutputMode(NachaReaderStepMeta.OutputMode.ALL_RECORDS);

        RowMeta rowMeta = new RowMeta();
        meta.getFields(rowMeta, "step", null, null, null, null, null);

        String[] names = rowMeta.getFieldNames();
        assertTrue(contains(names, "nacha_batch_entry_hash"), "Should include nacha_batch_entry_hash");
        assertTrue(contains(names, "nacha_file_total_debit"), "Should include nacha_file_total_debit");
        assertTrue(contains(names, "nacha_addenda_type_code"), "Should include nacha_addenda_type_code");
    }

    // -----------------------------------------------------------------------
    // check()
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("check(): blank filePathField adds an ERROR result")
    void check_blankFilePathField_addsError() {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setFilePathField("   ");

        java.util.List<org.pentaho.di.core.CheckResultInterface> remarks = new java.util.ArrayList<>();
        meta.check(remarks, null, null, null, null, null, null, null, null, null);

        assertTrue(remarks.stream().anyMatch(
            r -> r.getType() == org.pentaho.di.core.CheckResultInterface.TYPE_RESULT_ERROR),
            "Expected an ERROR check result for blank filePathField");
    }

    @Test
    @DisplayName("check(): valid filePathField adds an OK result")
    void check_validFilePathField_addsOk() {
        NachaReaderStepMeta meta = defaultMeta();
        meta.setFilePathField("file_path");

        java.util.List<org.pentaho.di.core.CheckResultInterface> remarks = new java.util.ArrayList<>();
        meta.check(remarks, null, null, null, null, null, null, null, null, null);

        assertTrue(remarks.stream().anyMatch(
            r -> r.getType() == org.pentaho.di.core.CheckResultInterface.TYPE_RESULT_OK),
            "Expected an OK check result for valid filePathField");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    static NachaReaderStepMeta defaultMeta() {
        NachaReaderStepMeta meta = new NachaReaderStepMeta();
        meta.setDefault();
        return meta;
    }

    private static NachaReaderStepMeta roundTrip(NachaReaderStepMeta original) throws Exception {
        String xml = "<step>" + original.getXML() + "</step>";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new java.io.ByteArrayInputStream(xml.getBytes()));
        Node stepNode = doc.getDocumentElement();

        NachaReaderStepMeta loaded = new NachaReaderStepMeta();
        loaded.loadXML(stepNode, null, (org.pentaho.metastore.api.IMetaStore) null);
        return loaded;
    }

    private static boolean contains(String[] arr, String target) {
        for (String s : arr) if (target.equals(s)) return true;
        return false;
    }
}
