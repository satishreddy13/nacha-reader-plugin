package com.example.pdi.plugin.nachareader.nacha;

/**
 * Discriminator character in position 1 of every 94-character NACHA line.
 */
public enum RecordType {
    FILE_HEADER('1'),
    BATCH_HEADER('5'),
    ENTRY_DETAIL('6'),
    ADDENDA('7'),
    BATCH_CONTROL('8'),
    FILE_CONTROL('9'),
    UNKNOWN('?');

    private final char code;

    RecordType(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static RecordType fromChar(char c) {
        for (RecordType rt : values()) {
            if (rt.code == c) return rt;
        }
        return UNKNOWN;
    }
}
