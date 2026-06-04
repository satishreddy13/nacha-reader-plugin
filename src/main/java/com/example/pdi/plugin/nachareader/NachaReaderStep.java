package com.example.pdi.plugin.nachareader;

import java.nio.file.Paths;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.example.pdi.plugin.nachareader.nacha.NachaParser;
import com.example.pdi.plugin.nachareader.nacha.NachaParser.ParsedEntry;
import com.example.pdi.plugin.nachareader.nacha.NachaParser.RawRecord;

/**
 * Reads a NACHA ACH file whose path is supplied by an upstream field,
 * and emits one output row per ACH transaction (Entry Details mode)
 * or one output row per NACHA line (All Records mode).
 */
public class NachaReaderStep extends BaseStep implements StepInterface {

    private NachaReaderStepMeta meta;
    private NachaReaderStepData data;

    public NachaReaderStep(StepMeta stepMeta, StepDataInterface stepData,
            int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepData, copyNr, transMeta, trans);
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (NachaReaderStepMeta) smi;
        data = (NachaReaderStepData) sdi;

        Object[] r = getRow();
        if (r == null) {
            setOutputDone();
            return false;
        }

        if (first) {
            first = false;
            data.outputRowMeta = getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this, null, null);

            data.filePathFieldIndex = getInputRowMeta().indexOfValue(meta.getFilePathField());
            if (data.filePathFieldIndex < 0) {
                throw new KettleException(
                    "Field '" + meta.getFilePathField() + "' not found in input stream.");
            }
        }

        String filePath = getInputRowMeta().getString(r, data.filePathFieldIndex);
        filePath = environmentSubstitute(filePath);

        try {
            NachaParser parser = new NachaParser();
            if (meta.getOutputMode() == NachaReaderStepMeta.OutputMode.ENTRY_DETAILS) {
                List<ParsedEntry> entries = parser.parseEntryDetails(Paths.get(filePath));
                for (ParsedEntry entry : entries) {
                    putRow(data.outputRowMeta, buildEntryRow(r, entry));
                }
            } else {
                List<RawRecord> records = parser.parseAllRecords(Paths.get(filePath));
                for (RawRecord record : records) {
                    putRow(data.outputRowMeta, buildRawRow(r, record));
                }
            }
        } catch (Exception e) {
            throw new KettleException(
                "Error parsing NACHA file '" + filePath + "': " + e.getMessage(), e);
        }

        if (checkFeedback(getLinesWritten())) {
            logBasic("NACHA Reader processed {0} output rows", getLinesWritten());
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Row builders
    // -----------------------------------------------------------------------

    private Object[] buildEntryRow(Object[] inputRow, ParsedEntry e) {
        int inputSize = getInputRowMeta().size();
        Object[] out = RowDataUtil.resizeArray(inputRow, data.outputRowMeta.size());
        int i = inputSize;
        out[i++] = "6";
        out[i++] = e.fileImmediateDestination;
        out[i++] = e.fileImmediateOrigin;
        out[i++] = e.fileCreationDate;
        out[i++] = e.fileCreationTime;
        out[i++] = e.fileIdModifier;
        out[i++] = e.fileImmediateDestinationName;
        out[i++] = e.fileImmediateOriginName;
        out[i++] = e.batchServiceClassCode;
        out[i++] = e.batchCompanyName;
        out[i++] = e.batchCompanyDiscretionaryData;
        out[i++] = e.batchCompanyIdentification;
        out[i++] = e.batchSecCode;
        out[i++] = e.batchCompanyEntryDescription;
        out[i++] = e.batchEffectiveEntryDate;
        out[i++] = e.batchODFIIdentification;
        out[i++] = e.batchNumber;
        out[i++] = e.entryTransactionCode;
        out[i++] = e.entryReceivingDFIRouting;
        out[i++] = e.entryCheckDigit;
        out[i++] = e.entryDFIAccountNumber;
        out[i++] = e.entryAmount;
        out[i++] = e.entryIndividualIdNumber;
        out[i++] = e.entryIndividualName;
        out[i++] = e.entryDiscretionaryData;
        out[i++] = e.entryAddendaIndicator;
        out[i++] = e.entryTraceNumber;
        out[i]   = e.addendaConcatenated;
        return out;
    }

    private Object[] buildRawRow(Object[] inputRow, RawRecord r) {
        int inputSize = getInputRowMeta().size();
        Object[] out = RowDataUtil.resizeArray(inputRow, data.outputRowMeta.size());
        int i = inputSize;
        out[i++] = r.recordType.getCode() == '?' ? "?" : String.valueOf(r.recordType.getCode());
        out[i++] = r.immediateDestination;
        out[i++] = r.immediateOrigin;
        out[i++] = r.fileCreationDate;
        out[i++] = r.fileCreationTime;
        out[i++] = r.fileIdModifier;
        out[i++] = r.immediateDestinationName;
        out[i++] = r.immediateOriginName;
        out[i++] = r.batchServiceClassCode;
        out[i++] = r.batchCompanyName;
        out[i++] = r.batchCompanyDiscretionaryData;
        out[i++] = r.batchCompanyIdentification;
        out[i++] = r.batchSecCode;
        out[i++] = r.batchCompanyEntryDescription;
        out[i++] = r.batchEffectiveEntryDate;
        out[i++] = r.batchODFIIdentification;
        out[i++] = r.batchNumber;
        out[i++] = r.batchCtrlEntryAddendaCount;
        out[i++] = r.batchCtrlEntryHash;
        out[i++] = r.batchCtrlTotalDebit;
        out[i++] = r.batchCtrlTotalCredit;
        out[i++] = r.entryTransactionCode;
        out[i++] = r.entryReceivingDFIRouting;
        out[i++] = r.entryCheckDigit;
        out[i++] = r.entryDFIAccountNumber;
        out[i++] = r.entryAmount;
        out[i++] = r.entryIndividualIdNumber;
        out[i++] = r.entryIndividualName;
        out[i++] = r.entryDiscretionaryData;
        out[i++] = r.entryAddendaIndicator;
        out[i++] = r.entryTraceNumber;
        out[i++] = r.addendaTypeCode;
        out[i++] = r.addendaPaymentInfo;
        out[i++] = r.addendaSequenceNumber;
        out[i++] = r.addendaEntrySequenceNumber;
        out[i++] = r.fileBatchCount;
        out[i++] = r.fileBlockCount;
        out[i++] = r.fileEntryAddendaCount;
        out[i++] = r.fileEntryHash;
        out[i++] = r.fileTotalDebit;
        out[i]   = r.fileTotalCredit;
        return out;
    }
}
