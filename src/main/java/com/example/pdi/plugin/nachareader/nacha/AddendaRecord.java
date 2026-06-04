package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/** NACHA record type 7 — Addenda Record. */
public class AddendaRecord extends NachaRecord {

    public final String addendaTypeCode;            // 2–3
    public final String paymentRelatedInformation;  // 4–83
    public final String sequenceNumber;             // 84–87
    public final String entryDetailSequenceNumber;  // 88–94

    public AddendaRecord(String rawLine) throws IOException {
        super(RecordType.ADDENDA, rawLine);
        addendaTypeCode           = field(2,  3);
        paymentRelatedInformation = field(4,  83);
        sequenceNumber            = field(84, 87);
        entryDetailSequenceNumber = field(88, 94);
    }
}
