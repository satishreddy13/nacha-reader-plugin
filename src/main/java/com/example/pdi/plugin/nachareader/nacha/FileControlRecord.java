package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/** NACHA record type 9 — File Control. */
public class FileControlRecord extends NachaRecord {

    public final String batchCount;        // 2–7
    public final String blockCount;        // 8–13
    public final String entryAddendaCount; // 14–21
    public final String entryHash;         // 22–31
    public final String totalDebitAmount;  // 32–43
    public final String totalCreditAmount; // 44–55

    public FileControlRecord(String rawLine) throws IOException {
        super(RecordType.FILE_CONTROL, rawLine);
        batchCount        = field(2,  7);
        blockCount        = field(8,  13);
        entryAddendaCount = field(14, 21);
        entryHash         = field(22, 31);
        totalDebitAmount  = field(32, 43);
        totalCreditAmount = field(44, 55);
    }
}
