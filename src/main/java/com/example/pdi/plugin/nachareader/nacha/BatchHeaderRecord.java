package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/** NACHA record type 5 — Batch Header. */
public class BatchHeaderRecord extends NachaRecord {

    public final String serviceClassCode;           // 2–4
    public final String companyName;                // 5–20
    public final String companyDiscretionaryData;   // 21–40
    public final String companyIdentification;      // 41–50
    public final String standardEntryClassCode;     // 51–53  (PPD, CCD, WEB, …)
    public final String companyEntryDescription;    // 54–63
    public final String effectiveEntryDate;         // 70–75  (YYMMDD)
    public final String ODFIIdentification;         // 80–87
    public final String batchNumber;                // 89–94

    public BatchHeaderRecord(String rawLine) throws IOException {
        super(RecordType.BATCH_HEADER, rawLine);
        serviceClassCode         = field(2,  4);
        companyName              = field(5,  20);
        companyDiscretionaryData = field(21, 40);
        companyIdentification    = field(41, 50);
        standardEntryClassCode   = field(51, 53);
        companyEntryDescription  = field(54, 63);
        effectiveEntryDate       = field(70, 75);
        ODFIIdentification       = field(80, 87);
        batchNumber              = field(89, 94);
    }
}
