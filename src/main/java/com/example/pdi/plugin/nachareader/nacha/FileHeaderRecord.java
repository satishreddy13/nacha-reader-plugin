package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/** NACHA record type 1 — File Header. */
public class FileHeaderRecord extends NachaRecord {

    public final String immediateDestination;     // 4–13
    public final String immediateOrigin;          // 14–23
    public final String fileCreationDate;         // 24–29  (YYMMDD)
    public final String fileCreationTime;         // 30–33  (HHMM)
    public final String fileIdModifier;           // 34
    public final String immediateDestinationName; // 41–63
    public final String immediateOriginName;      // 64–86

    public FileHeaderRecord(String rawLine) throws IOException {
        super(RecordType.FILE_HEADER, rawLine);
        immediateDestination     = field(4,  13);
        immediateOrigin          = field(14, 23);
        fileCreationDate         = field(24, 29);
        fileCreationTime         = field(30, 33);
        fileIdModifier           = field(34);
        immediateDestinationName = field(41, 63);
        immediateOriginName      = field(64, 86);
    }
}
