package com.dci.intellij.dbn.editor.data.state.column.ui;

import com.dci.intellij.dbn.common.ui.dialog.DBNDialog;
import com.dci.intellij.dbn.editor.data.DatasetEditor;
import com.dci.intellij.dbn.editor.data.DatasetLoadInstructions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;

public class DatasetColumnSetupDialog extends DBNDialog {
    public static final DatasetLoadInstructions LOAD_INSTRUCTIONS = new DatasetLoadInstructions(true, true, true, true);
    private DatasetColumnSetupForm columnSetupForm;
    private DatasetEditor datasetEditor;

    public DatasetColumnSetupDialog(DatasetEditor datasetEditor) {
        super(datasetEditor.getProject(), "Column Setup", true);
        this.datasetEditor = datasetEditor;
        setModal(true);
        setResizable(true);
        columnSetupForm = new DatasetColumnSetupForm(datasetEditor);
        getCancelAction().putValue(Action.NAME, "Cancel");
        init();
    }

    protected String getDimensionServiceKey() {
        return "DBNavigator.DatasetColumnSetup";
    }

    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        boolean changed = columnSetupForm.applyChanges();
        super.doOKAction();
        if (changed) {
            datasetEditor.loadData(LOAD_INSTRUCTIONS);
        }
    }

    @Nullable
    protected JComponent createCenterPanel() {
        return columnSetupForm.getComponent();
    }

    @Override
    public void dispose() {
        if (!isDisposed()) {
            super.dispose();
            columnSetupForm.dispose();
        }
    }
}
