package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/** NACHA record type 6 — Entry Detail. */
public class EntryDetailRecord extends NachaRecord {

    public final String transactionCode;              // 2–3
    public final String receivingDFIRoutingNumber;    // 4–11
    public final String checkDigit;                   // 12
    public final String DFIAccountNumber;             // 13–29
    public final String amount;                       // 30–39  (10 digits, implied 2 decimal places)
    public final String individualIdentificationNumber; // 40–54
    public final String individualName;               // 55–76
    public final String discretionaryData;            // 77–78
    public final String addendaRecordIndicator;       // 79  ("0" or "1")
    public final String traceNumber;                  // 80–94

    public EntryDetailRecord(String rawLine) throws IOException {
        super(RecordType.ENTRY_DETAIL, rawLine);
        transactionCode                = field(2,  3);
        receivingDFIRoutingNumber      = field(4,  11);
        checkDigit                     = field(12);
        DFIAccountNumber               = field(13, 29);
        amount                         = field(30, 39);
        individualIdentificationNumber = field(40, 54);
        individualName                 = field(55, 76);
        discretionaryData              = field(77, 78);
        addendaRecordIndicator         = field(79);
        traceNumber                    = field(80, 94);
    }
}
