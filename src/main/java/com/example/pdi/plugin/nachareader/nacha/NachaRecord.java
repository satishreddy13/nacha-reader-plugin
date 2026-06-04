package com.example.pdi.plugin.nachareader.nacha;

import java.io.IOException;

/**
 * Base class for all NACHA record types.
 * Each record is exactly 94 characters (the NACHA fixed-width line length).
 */
public abstract class NachaRecord {

    public static final int LINE_LENGTH = 94;

    private final RecordType type;
    private final String rawLine;

    protected NachaRecord(RecordType type, String rawLine) throws IOException {
        if (rawLine.length() != LINE_LENGTH) {
            throw new IOException(
                "Invalid NACHA line length: expected " + LINE_LENGTH +
                " but got " + rawLine.length() + " for record type " + type);
        }
        this.type = type;
        this.rawLine = rawLine;
    }

    public RecordType getType() { return type; }
    public String getRawLine()  { return rawLine; }

    /**
     * Extracts a field using 1-based inclusive positions from the NACHA spec,
     * trimming leading/trailing spaces from the result.
     *
     * @param start 1-based start position (inclusive)
     * @param end   1-based end position (inclusive)
     */
    protected String field(int start, int end) {
        return rawLine.substring(start - 1, end).trim();
    }

    /** Single-character field at 1-based position. */
    protected String field(int pos) {
        return field(pos, pos);
    }
}
