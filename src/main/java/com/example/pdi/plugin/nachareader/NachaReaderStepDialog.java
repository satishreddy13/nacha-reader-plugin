package com.example.pdi.plugin.nachareader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
 * Spoon dialog for the NACHA ACH File Reader step.
 *
 * Single "Settings" tab:
 *   - File Path Field  : combo populated from upstream step fields
 *   - Output Mode      : radio — Entry Details / All Records
 */
public class NachaReaderStepDialog extends BaseStepDialog implements StepDialogInterface {

    private final NachaReaderStepMeta input;
    private final TransMeta           transMeta;

    private Combo  wFilePathField;
    private Button wRadioEntryDetails;
    private Button wRadioAllRecords;

    public NachaReaderStepDialog(Shell parent, Object baseStepMeta,
            TransMeta transMeta, String stepname) {
        super(parent, (BaseStepMeta) baseStepMeta, transMeta, stepname);
        this.input     = (NachaReaderStepMeta) baseStepMeta;
        this.transMeta = transMeta;
    }

    @Override
    public String open() {
        Shell   parent  = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX);
        props.setLook(shell);
        setShellImage(shell, input);

        ModifyListener lsMod = e -> input.setChanged();
        changed = input.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth  = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;
        shell.setLayout(formLayout);
        shell.setText("NACHA ACH File Reader");
        shell.setSize(520, 340);

        int middle = props.getMiddlePct();
        int margin  = Const.MARGIN;

        // ---- Step name ----
        wlStepname = new Label(shell, SWT.RIGHT);
        wlStepname.setText("Step Name");
        props.setLook(wlStepname);
        fdlStepname = new FormData();
        fdlStepname.left  = new FormAttachment(0, 0);
        fdlStepname.right = new FormAttachment(middle, -margin);
        fdlStepname.top   = new FormAttachment(0, margin);
        wlStepname.setLayoutData(fdlStepname);

        wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepname.setText(stepname);
        props.setLook(wStepname);
        wStepname.addModifyListener(lsMod);
        fdStepname = new FormData();
        fdStepname.left  = new FormAttachment(middle, 0);
        fdStepname.top   = new FormAttachment(0, margin);
        fdStepname.right = new FormAttachment(100, 0);
        wStepname.setLayoutData(fdStepname);

        // ---- OK / Cancel ----
        wOK     = new Button(shell, SWT.PUSH);
        wCancel = new Button(shell, SWT.PUSH);
        wOK.setText("OK");
        wCancel.setText("Cancel");

        FormData fdOK = new FormData();
        fdOK.left   = new FormAttachment(33, 0);
        fdOK.bottom = new FormAttachment(100, -margin);
        wOK.setLayoutData(fdOK);

        FormData fdCancel = new FormData();
        fdCancel.left   = new FormAttachment(66, 0);
        fdCancel.bottom = new FormAttachment(100, -margin);
        wCancel.setLayoutData(fdCancel);

        // ---- Tab folder ----
        CTabFolder tabFolder = new CTabFolder(shell, SWT.BORDER);
        props.setLook(tabFolder);
        FormData fdTabFolder = new FormData();
        fdTabFolder.left   = new FormAttachment(0, 0);
        fdTabFolder.right  = new FormAttachment(100, 0);
        fdTabFolder.top    = new FormAttachment(wStepname, margin);
        fdTabFolder.bottom = new FormAttachment(wOK, -margin);
        tabFolder.setLayoutData(fdTabFolder);

        buildSettingsTab(tabFolder, lsMod, middle, margin);

        tabFolder.setSelection(0);

        // ---- Listeners ----
        wOK.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { ok(); }
        });
        wCancel.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { cancel(); }
        });
        shell.addShellListener(new ShellAdapter() {
            @Override public void shellClosed(ShellEvent e) { cancel(); }
        });

        getData();
        input.setChanged(changed);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        return stepname;
    }

    // -----------------------------------------------------------------------
    // Tab builder
    // -----------------------------------------------------------------------

    private void buildSettingsTab(CTabFolder folder, ModifyListener lsMod,
            int middle, int margin) {

        CTabItem tab = new CTabItem(folder, SWT.NONE);
        tab.setText("Settings");

        Composite comp = new Composite(folder, SWT.NONE);
        props.setLook(comp);
        FormLayout fl = new FormLayout();
        fl.marginWidth = fl.marginHeight = Const.MARGIN;
        comp.setLayout(fl);

        // ---- File Path Field ----
        Label wlFilePathField = new Label(comp, SWT.RIGHT);
        wlFilePathField.setText("File Path Field");
        props.setLook(wlFilePathField);
        FormData fdlFPF = new FormData();
        fdlFPF.left  = new FormAttachment(0, 0);
        fdlFPF.right = new FormAttachment(middle, -margin);
        fdlFPF.top   = new FormAttachment(0, margin);
        wlFilePathField.setLayoutData(fdlFPF);

        wFilePathField = new Combo(comp, SWT.DROP_DOWN | SWT.BORDER);
        props.setLook(wFilePathField);
        wFilePathField.addModifyListener(lsMod);
        FormData fdFPF = new FormData();
        fdFPF.left  = new FormAttachment(middle, 0);
        fdFPF.top   = new FormAttachment(0, margin);
        fdFPF.right = new FormAttachment(100, 0);
        wFilePathField.setLayoutData(fdFPF);

        // Populate combo with upstream field names
        try {
            org.pentaho.di.trans.step.StepMeta sm = new org.pentaho.di.trans.step.StepMeta();
            sm.setName(stepname);
            RowMetaInterface prevFields = transMeta.getPrevStepFields(sm);
            if (prevFields != null) {
                wFilePathField.setItems(prevFields.getFieldNames());
            }
        } catch (Exception ex) {
            // upstream not connected yet — leave empty; user can type manually
        }

        // ---- Output Mode group ----
        Group gMode = new Group(comp, SWT.SHADOW_ETCHED_IN);
        gMode.setText("Output Mode");
        props.setLook(gMode);
        FormLayout gfl = new FormLayout();
        gfl.marginWidth = gfl.marginHeight = Const.MARGIN;
        gMode.setLayout(gfl);

        FormData fdMode = new FormData();
        fdMode.left  = new FormAttachment(0, 0);
        fdMode.right = new FormAttachment(100, 0);
        fdMode.top   = new FormAttachment(wFilePathField, margin * 2);
        gMode.setLayoutData(fdMode);

        wRadioEntryDetails = new Button(gMode, SWT.RADIO);
        wRadioEntryDetails.setText("Entry Details  (one output row per type-6 record, enriched with file/batch context)");
        props.setLook(wRadioEntryDetails);
        FormData fdED = new FormData();
        fdED.left = new FormAttachment(0, 0);
        fdED.top  = new FormAttachment(0, margin);
        wRadioEntryDetails.setLayoutData(fdED);
        wRadioEntryDetails.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { input.setChanged(); }
        });

        wRadioAllRecords = new Button(gMode, SWT.RADIO);
        wRadioAllRecords.setText("All Records  (one output row per NACHA line — types 1, 5, 6, 7, 8, 9)");
        props.setLook(wRadioAllRecords);
        FormData fdAR = new FormData();
        fdAR.left = new FormAttachment(0, 0);
        fdAR.top  = new FormAttachment(wRadioEntryDetails, margin);
        wRadioAllRecords.setLayoutData(fdAR);
        wRadioAllRecords.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { input.setChanged(); }
        });

        tab.setControl(comp);
    }

    // -----------------------------------------------------------------------
    // Data transfer
    // -----------------------------------------------------------------------

    private void getData() {
        wFilePathField.setText(nvl(input.getFilePathField()));
        boolean allRecords = input.getOutputMode() == NachaReaderStepMeta.OutputMode.ALL_RECORDS;
        wRadioEntryDetails.setSelection(!allRecords);
        wRadioAllRecords.setSelection(allRecords);
    }

    private void ok() {
        String fpf = wFilePathField.getText().trim();
        if (fpf.isEmpty()) {
            wFilePathField.setFocus();
            return;
        }
        stepname = wStepname.getText();
        input.setFilePathField(fpf);
        input.setOutputMode(wRadioAllRecords.getSelection()
            ? NachaReaderStepMeta.OutputMode.ALL_RECORDS
            : NachaReaderStepMeta.OutputMode.ENTRY_DETAILS);
        dispose();
    }

    private void cancel() {
        stepname = null;
        input.setChanged(changed);
        dispose();
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
