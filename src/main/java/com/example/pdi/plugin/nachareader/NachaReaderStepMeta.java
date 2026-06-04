package com.example.pdi.plugin.nachareader;

import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

@Step(
  id          = "NachaReaderStep",
  name        = "NACHA ACH File Reader",
  description = "Reads a NACHA ACH file from a path field and outputs one row per Entry Detail record",
  categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Input",
  image       = "ui/images/TFI.svg"
)
public class NachaReaderStepMeta extends BaseStepMeta implements StepMetaInterface {

    public enum OutputMode { ENTRY_DETAILS, ALL_RECORDS }

    // ---- configurable fields ----
    private String     filePathField = "file_path";
    private OutputMode outputMode    = OutputMode.ENTRY_DETAILS;

    // ---- accessors ----
    public String     getFilePathField()         { return filePathField; }
    public void       setFilePathField(String v) { filePathField = v; }
    public OutputMode getOutputMode()            { return outputMode; }
    public void       setOutputMode(OutputMode v){ outputMode = v; }

    // ---- lifecycle ----

    @Override
    public void setDefault() {
        filePathField = "file_path";
        outputMode    = OutputMode.ENTRY_DETAILS;
    }

    @Override
    public Object clone() {
        return (NachaReaderStepMeta) super.clone();
    }

    @Override
    public void getFields(RowMetaInterface rowMeta, String origin,
            RowMetaInterface[] info, StepMeta nextStep,
            VariableSpace space, Repository repository, IMetaStore metaStore)
            throws KettleStepException {

        if (outputMode == OutputMode.ENTRY_DETAILS) {
            addField(rowMeta, origin, "nacha_record_type");
            // File header context
            addField(rowMeta, origin, "nacha_file_immediate_destination");
            addField(rowMeta, origin, "nacha_file_immediate_origin");
            addField(rowMeta, origin, "nacha_file_creation_date");
            addField(rowMeta, origin, "nacha_file_creation_time");
            addField(rowMeta, origin, "nacha_file_id_modifier");
            addField(rowMeta, origin, "nacha_file_destination_name");
            addField(rowMeta, origin, "nacha_file_origin_name");
            // Batch header context
            addField(rowMeta, origin, "nacha_batch_service_class_code");
            addField(rowMeta, origin, "nacha_batch_company_name");
            addField(rowMeta, origin, "nacha_batch_company_discretionary_data");
            addField(rowMeta, origin, "nacha_batch_company_identification");
            addField(rowMeta, origin, "nacha_batch_sec_code");
            addField(rowMeta, origin, "nacha_batch_company_entry_description");
            addField(rowMeta, origin, "nacha_batch_effective_entry_date");
            addField(rowMeta, origin, "nacha_batch_odfi_identification");
            addField(rowMeta, origin, "nacha_batch_number");
            // Entry detail
            addField(rowMeta, origin, "nacha_entry_transaction_code");
            addField(rowMeta, origin, "nacha_entry_receiving_dfi_routing");
            addField(rowMeta, origin, "nacha_entry_check_digit");
            addField(rowMeta, origin, "nacha_entry_dfi_account_number");
            addField(rowMeta, origin, "nacha_entry_amount");
            addField(rowMeta, origin, "nacha_entry_individual_id_number");
            addField(rowMeta, origin, "nacha_entry_individual_name");
            addField(rowMeta, origin, "nacha_entry_discretionary_data");
            addField(rowMeta, origin, "nacha_entry_addenda_indicator");
            addField(rowMeta, origin, "nacha_entry_trace_number");
            addField(rowMeta, origin, "nacha_addenda_concatenated");
        } else {
            // ALL_RECORDS
            addField(rowMeta, origin, "nacha_record_type");
            // File header
            addField(rowMeta, origin, "nacha_file_immediate_destination");
            addField(rowMeta, origin, "nacha_file_immediate_origin");
            addField(rowMeta, origin, "nacha_file_creation_date");
            addField(rowMeta, origin, "nacha_file_creation_time");
            addField(rowMeta, origin, "nacha_file_id_modifier");
            addField(rowMeta, origin, "nacha_file_destination_name");
            addField(rowMeta, origin, "nacha_file_origin_name");
            // Batch header / control
            addField(rowMeta, origin, "nacha_batch_service_class_code");
            addField(rowMeta, origin, "nacha_batch_company_name");
            addField(rowMeta, origin, "nacha_batch_company_discretionary_data");
            addField(rowMeta, origin, "nacha_batch_company_identification");
            addField(rowMeta, origin, "nacha_batch_sec_code");
            addField(rowMeta, origin, "nacha_batch_company_entry_description");
            addField(rowMeta, origin, "nacha_batch_effective_entry_date");
            addField(rowMeta, origin, "nacha_batch_odfi_identification");
            addField(rowMeta, origin, "nacha_batch_number");
            addField(rowMeta, origin, "nacha_batch_entry_addenda_count");
            addField(rowMeta, origin, "nacha_batch_entry_hash");
            addField(rowMeta, origin, "nacha_batch_total_debit");
            addField(rowMeta, origin, "nacha_batch_total_credit");
            // Entry detail
            addField(rowMeta, origin, "nacha_entry_transaction_code");
            addField(rowMeta, origin, "nacha_entry_receiving_dfi_routing");
            addField(rowMeta, origin, "nacha_entry_check_digit");
            addField(rowMeta, origin, "nacha_entry_dfi_account_number");
            addField(rowMeta, origin, "nacha_entry_amount");
            addField(rowMeta, origin, "nacha_entry_individual_id_number");
            addField(rowMeta, origin, "nacha_entry_individual_name");
            addField(rowMeta, origin, "nacha_entry_discretionary_data");
            addField(rowMeta, origin, "nacha_entry_addenda_indicator");
            addField(rowMeta, origin, "nacha_entry_trace_number");
            // Addenda
            addField(rowMeta, origin, "nacha_addenda_type_code");
            addField(rowMeta, origin, "nacha_addenda_payment_info");
            addField(rowMeta, origin, "nacha_addenda_sequence_number");
            addField(rowMeta, origin, "nacha_addenda_entry_sequence_number");
            // File control
            addField(rowMeta, origin, "nacha_file_batch_count");
            addField(rowMeta, origin, "nacha_file_block_count");
            addField(rowMeta, origin, "nacha_file_entry_addenda_count");
            addField(rowMeta, origin, "nacha_file_entry_hash");
            addField(rowMeta, origin, "nacha_file_total_debit");
            addField(rowMeta, origin, "nacha_file_total_credit");
        }
    }

    private static void addField(RowMetaInterface rowMeta, String origin, String name)
            throws KettleStepException {
        ValueMetaString v = new ValueMetaString(name);
        v.setOrigin(origin);
        rowMeta.addValueMeta(v);
    }

    // ---- XML serialisation ----

    @Override
    public String getXML() throws KettleException {
        StringBuilder sb = new StringBuilder();
        sb.append(XMLHandler.addTagValue("filePathField", filePathField));
        sb.append(XMLHandler.addTagValue("outputMode",    outputMode.name()));
        return sb.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore)
            throws KettleXMLException {
        try {
            filePathField = nvl(XMLHandler.getTagValue(stepnode, "filePathField"), "file_path");
            String om     = XMLHandler.getTagValue(stepnode, "outputMode");
            outputMode    = (om != null) ? OutputMode.valueOf(om) : OutputMode.ENTRY_DETAILS;
        } catch (Exception e) {
            throw new KettleXMLException("Unable to load NachaReaderStep metadata from XML", e);
        }
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore,
            ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
        // Repository persistence not implemented — transformations saved as XML files
    }

    @Override
    public void saveRep(Repository rep, IMetaStore metaStore,
            ObjectId id_transformation, ObjectId id_step) throws KettleException {
        // Repository persistence not implemented
    }

    @Override
    public void check(List<CheckResultInterface> remarks, TransMeta transMeta,
            StepMeta stepMeta, RowMetaInterface prev, String[] input, String[] output,
            RowMetaInterface info, VariableSpace space, Repository repository, IMetaStore metaStore) {
        if (filePathField == null || filePathField.trim().isEmpty()) {
            remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
                "File Path Field must not be empty.", stepMeta));
        } else {
            remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_OK,
                "Configuration looks good.", stepMeta));
        }
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface,
            int copyNr, TransMeta transMeta, Trans trans) {
        return new NachaReaderStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new NachaReaderStepData();
    }

    private static String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }
}
