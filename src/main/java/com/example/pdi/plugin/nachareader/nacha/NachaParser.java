package com.example.pdi.plugin.nachareader.nacha;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stateless NACHA ACH file parser.
 *
 * <p>Two public entry points:
 * <ul>
 *   <li>{@link #parseEntryDetails(Path)} — returns one {@link ParsedEntry} per
 *       type-6 record, enriched with file/batch header context.</li>
 *   <li>{@link #parseAllRecords(Path)} — returns one {@link RawRecord} per
 *       NACHA line, with all parsed fields.</li>
 * </ul>
 */
public class NachaParser {

    // -----------------------------------------------------------------------
    // Public entry points
    // -----------------------------------------------------------------------

    public List<ParsedEntry> parseEntryDetails(Path filePath) throws IOException {
        List<ParsedEntry> results = new ArrayList<>();

        FileHeaderRecord  currentFileHeader  = null;
        BatchHeaderRecord currentBatchHeader = null;
        EntryDetailRecord currentEntry       = null;
        List<AddendaRecord> pendingAddenda   = new ArrayList<>();

        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.US_ASCII)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;  // skip blank padding lines
                validateLineLength(line, lineNumber);

                RecordType rt = RecordType.fromChar(line.charAt(0));
                switch (rt) {
                    case FILE_HEADER:
                        currentFileHeader = new FileHeaderRecord(line);
                        break;

                    case BATCH_HEADER:
                        currentBatchHeader = new BatchHeaderRecord(line);
                        break;

                    case ENTRY_DETAIL:
                        // Flush the previous entry before starting a new one
                        if (currentEntry != null) {
                            results.add(buildParsedEntry(currentFileHeader, currentBatchHeader,
                                                         currentEntry, pendingAddenda));
                            pendingAddenda = new ArrayList<>();
                        }
                        currentEntry = new EntryDetailRecord(line);
                        break;

                    case ADDENDA:
                        pendingAddenda.add(new AddendaRecord(line));
                        break;

                    case BATCH_CONTROL:
                        // Flush the last entry in the batch
                        if (currentEntry != null) {
                            results.add(buildParsedEntry(currentFileHeader, currentBatchHeader,
                                                         currentEntry, pendingAddenda));
                            currentEntry = null;
                            pendingAddenda = new ArrayList<>();
                        }
                        break;

                    default:
                        // FILE_CONTROL, UNKNOWN, padding type-9 lines — skip
                        break;
                }
            }
        }

        return results;
    }

    public List<RawRecord> parseAllRecords(Path filePath) throws IOException {
        List<RawRecord> results = new ArrayList<>();

        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.US_ASCII)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                validateLineLength(line, lineNumber);

                RecordType rt = RecordType.fromChar(line.charAt(0));
                RawRecord record = new RawRecord(rt);

                switch (rt) {
                    case FILE_HEADER: {
                        FileHeaderRecord r = new FileHeaderRecord(line);
                        record.immediateDestination     = r.immediateDestination;
                        record.immediateOrigin          = r.immediateOrigin;
                        record.fileCreationDate         = r.fileCreationDate;
                        record.fileCreationTime         = r.fileCreationTime;
                        record.fileIdModifier           = r.fileIdModifier;
                        record.immediateDestinationName = r.immediateDestinationName;
                        record.immediateOriginName      = r.immediateOriginName;
                        break;
                    }
                    case BATCH_HEADER: {
                        BatchHeaderRecord r = new BatchHeaderRecord(line);
                        record.batchServiceClassCode         = r.serviceClassCode;
                        record.batchCompanyName              = r.companyName;
                        record.batchCompanyDiscretionaryData = r.companyDiscretionaryData;
                        record.batchCompanyIdentification    = r.companyIdentification;
                        record.batchSecCode                  = r.standardEntryClassCode;
                        record.batchCompanyEntryDescription  = r.companyEntryDescription;
                        record.batchEffectiveEntryDate       = r.effectiveEntryDate;
                        record.batchODFIIdentification       = r.ODFIIdentification;
                        record.batchNumber                   = r.batchNumber;
                        break;
                    }
                    case ENTRY_DETAIL: {
                        EntryDetailRecord r = new EntryDetailRecord(line);
                        record.entryTransactionCode        = r.transactionCode;
                        record.entryReceivingDFIRouting    = r.receivingDFIRoutingNumber;
                        record.entryCheckDigit             = r.checkDigit;
                        record.entryDFIAccountNumber       = r.DFIAccountNumber;
                        record.entryAmount                 = r.amount;
                        record.entryIndividualIdNumber     = r.individualIdentificationNumber;
                        record.entryIndividualName         = r.individualName;
                        record.entryDiscretionaryData      = r.discretionaryData;
                        record.entryAddendaIndicator       = r.addendaRecordIndicator;
                        record.entryTraceNumber            = r.traceNumber;
                        break;
                    }
                    case ADDENDA: {
                        AddendaRecord r = new AddendaRecord(line);
                        record.addendaTypeCode              = r.addendaTypeCode;
                        record.addendaPaymentInfo           = r.paymentRelatedInformation;
                        record.addendaSequenceNumber        = r.sequenceNumber;
                        record.addendaEntrySequenceNumber   = r.entryDetailSequenceNumber;
                        break;
                    }
                    case BATCH_CONTROL: {
                        BatchControlRecord r = new BatchControlRecord(line);
                        record.batchServiceClassCode      = r.serviceClassCode;
                        record.batchCompanyIdentification = r.companyIdentification;
                        record.batchODFIIdentification    = r.ODFIIdentification;
                        record.batchNumber                = r.batchNumber;
                        record.batchCtrlEntryAddendaCount = r.entryAddendaCount;
                        record.batchCtrlEntryHash         = r.entryHash;
                        record.batchCtrlTotalDebit        = r.totalDebitAmount;
                        record.batchCtrlTotalCredit       = r.totalCreditAmount;
                        break;
                    }
                    case FILE_CONTROL: {
                        FileControlRecord r = new FileControlRecord(line);
                        record.fileBatchCount        = r.batchCount;
                        record.fileBlockCount        = r.blockCount;
                        record.fileEntryAddendaCount = r.entryAddendaCount;
                        record.fileEntryHash         = r.entryHash;
                        record.fileTotalDebit        = r.totalDebitAmount;
                        record.fileTotalCredit       = r.totalCreditAmount;
                        break;
                    }
                    default:
                        break;
                }
                results.add(record);
            }
        }

        return results;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static void validateLineLength(String line, int lineNumber) throws IOException {
        if (line.length() != NachaRecord.LINE_LENGTH) {
            throw new IOException(
                "Invalid NACHA line length at line " + lineNumber +
                ": expected " + NachaRecord.LINE_LENGTH + " but got " + line.length());
        }
    }

    private static ParsedEntry buildParsedEntry(
            FileHeaderRecord fileHeader,
            BatchHeaderRecord batchHeader,
            EntryDetailRecord entry,
            List<AddendaRecord> addenda) {

        ParsedEntry pe = new ParsedEntry();

        // File header context (safe defaults if header was missing)
        if (fileHeader != null) {
            pe.fileImmediateDestination     = fileHeader.immediateDestination;
            pe.fileImmediateOrigin          = fileHeader.immediateOrigin;
            pe.fileCreationDate             = fileHeader.fileCreationDate;
            pe.fileCreationTime             = fileHeader.fileCreationTime;
            pe.fileIdModifier               = fileHeader.fileIdModifier;
            pe.fileImmediateDestinationName = fileHeader.immediateDestinationName;
            pe.fileImmediateOriginName      = fileHeader.immediateOriginName;
        }

        // Batch header context
        if (batchHeader != null) {
            pe.batchServiceClassCode         = batchHeader.serviceClassCode;
            pe.batchCompanyName              = batchHeader.companyName;
            pe.batchCompanyDiscretionaryData = batchHeader.companyDiscretionaryData;
            pe.batchCompanyIdentification    = batchHeader.companyIdentification;
            pe.batchSecCode                  = batchHeader.standardEntryClassCode;
            pe.batchCompanyEntryDescription  = batchHeader.companyEntryDescription;
            pe.batchEffectiveEntryDate       = batchHeader.effectiveEntryDate;
            pe.batchODFIIdentification       = batchHeader.ODFIIdentification;
            pe.batchNumber                   = batchHeader.batchNumber;
        }

        // Entry detail fields
        pe.entryTransactionCode        = entry.transactionCode;
        pe.entryReceivingDFIRouting    = entry.receivingDFIRoutingNumber;
        pe.entryCheckDigit             = entry.checkDigit;
        pe.entryDFIAccountNumber       = entry.DFIAccountNumber;
        pe.entryAmount                 = entry.amount;
        pe.entryIndividualIdNumber     = entry.individualIdentificationNumber;
        pe.entryIndividualName         = entry.individualName;
        pe.entryDiscretionaryData      = entry.discretionaryData;
        pe.entryAddendaIndicator       = entry.addendaRecordIndicator;
        pe.entryTraceNumber            = entry.traceNumber;

        // Addenda: concatenate paymentRelatedInformation with pipe separator
        if (!addenda.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (AddendaRecord ar : addenda) {
                if (sb.length() > 0) sb.append('|');
                sb.append(ar.paymentRelatedInformation);
            }
            pe.addendaConcatenated = sb.toString();
        }

        pe.addenda = Collections.unmodifiableList(new ArrayList<>(addenda));
        return pe;
    }

    // -----------------------------------------------------------------------
    // Result holder types
    // -----------------------------------------------------------------------

    /** One parsed ACH transaction with all context. */
    public static class ParsedEntry {
        // File header context
        public String fileImmediateDestination     = "";
        public String fileImmediateOrigin          = "";
        public String fileCreationDate             = "";
        public String fileCreationTime             = "";
        public String fileIdModifier               = "";
        public String fileImmediateDestinationName = "";
        public String fileImmediateOriginName      = "";
        // Batch header context
        public String batchServiceClassCode         = "";
        public String batchCompanyName              = "";
        public String batchCompanyDiscretionaryData = "";
        public String batchCompanyIdentification    = "";
        public String batchSecCode                  = "";
        public String batchCompanyEntryDescription  = "";
        public String batchEffectiveEntryDate       = "";
        public String batchODFIIdentification       = "";
        public String batchNumber                   = "";
        // Entry detail
        public String entryTransactionCode     = "";
        public String entryReceivingDFIRouting = "";
        public String entryCheckDigit          = "";
        public String entryDFIAccountNumber    = "";
        public String entryAmount              = "";
        public String entryIndividualIdNumber  = "";
        public String entryIndividualName      = "";
        public String entryDiscretionaryData   = "";
        public String entryAddendaIndicator    = "";
        public String entryTraceNumber         = "";
        // Addenda
        public String addendaConcatenated = "";
        public List<AddendaRecord> addenda = Collections.emptyList();
    }

    /** One NACHA line with all possible field slots. Unpopulated fields are "". */
    public static class RawRecord {
        public final RecordType recordType;
        // File header
        public String immediateDestination     = "";
        public String immediateOrigin          = "";
        public String fileCreationDate         = "";
        public String fileCreationTime         = "";
        public String fileIdModifier           = "";
        public String immediateDestinationName = "";
        public String immediateOriginName      = "";
        // Batch header / control shared fields
        public String batchServiceClassCode         = "";
        public String batchCompanyName              = "";
        public String batchCompanyDiscretionaryData = "";
        public String batchCompanyIdentification    = "";
        public String batchSecCode                  = "";
        public String batchCompanyEntryDescription  = "";
        public String batchEffectiveEntryDate       = "";
        public String batchODFIIdentification       = "";
        public String batchNumber                   = "";
        // Batch control only
        public String batchCtrlEntryAddendaCount = "";
        public String batchCtrlEntryHash         = "";
        public String batchCtrlTotalDebit        = "";
        public String batchCtrlTotalCredit       = "";
        // Entry detail
        public String entryTransactionCode     = "";
        public String entryReceivingDFIRouting = "";
        public String entryCheckDigit          = "";
        public String entryDFIAccountNumber    = "";
        public String entryAmount              = "";
        public String entryIndividualIdNumber  = "";
        public String entryIndividualName      = "";
        public String entryDiscretionaryData   = "";
        public String entryAddendaIndicator    = "";
        public String entryTraceNumber         = "";
        // Addenda
        public String addendaTypeCode            = "";
        public String addendaPaymentInfo         = "";
        public String addendaSequenceNumber      = "";
        public String addendaEntrySequenceNumber = "";
        // File control
        public String fileBatchCount        = "";
        public String fileBlockCount        = "";
        public String fileEntryAddendaCount = "";
        public String fileEntryHash         = "";
        public String fileTotalDebit        = "";
        public String fileTotalCredit       = "";

        public RawRecord(RecordType recordType) {
            this.recordType = recordType;
        }
    }
}
