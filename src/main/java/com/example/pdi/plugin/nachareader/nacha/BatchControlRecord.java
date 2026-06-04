package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/** NACHA record type 8 — Batch Control. */
public class BatchControlRecord extends NachaRecord {

    public final String serviceClassCode;      // 2–4
    public final String entryAddendaCount;     // 5–10
    public final String entryHash;             // 11–20
    public final String totalDebitAmount;      // 21–32
    public final String totalCreditAmount;     // 33–44
    public final String companyIdentification; // 45–54
    public final String ODFIIdentification;    // 80–87
    public final String batchNumber;           // 89–94

    public BatchControlRecord(String rawLine) throws IOException {
        super(RecordType.BATCH_CONTROL, rawLine);
        serviceClassCode      = field(2,  4);
        entryAddendaCount     = field(5,  10);
        entryHash             = field(11, 20);
        totalDebitAmount      = field(21, 32);
        totalCreditAmount     = field(33, 44);
        companyIdentification = field(45, 54);
        ODFIIdentification    = field(80, 87);
        batchNumber           = field(89, 94);
    }
}
