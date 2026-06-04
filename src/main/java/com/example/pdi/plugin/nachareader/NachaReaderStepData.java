package com.example.pdi.plugin.nachareader;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class NachaReaderStepData extends BaseStepData implements StepDataInterface {

    /** Full output row schema (input fields + NACHA output fields). */
    public RowMetaInterface outputRowMeta;

    /** Index of the file-path field in the input row meta. Resolved on first row. */
    public int filePathFieldIndex = -1;
}
