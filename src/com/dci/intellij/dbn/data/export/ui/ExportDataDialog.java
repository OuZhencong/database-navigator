package com.dci.intellij.dbn.data.export.ui;

import com.dci.intellij.dbn.common.ui.dialog.DBNDialog;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.export.DataExportInstructions;
import com.dci.intellij.dbn.data.export.DataExportManager;
import com.dci.intellij.dbn.data.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.execution.ExecutionResult;
import com.dci.intellij.dbn.object.common.DBObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;

public class ExportDataDialog extends DBNDialog {
    private ExportDataForm exportDataForm;
    private ResultSetTable table;
    private ConnectionHandler connectionHandler;

    public ExportDataDialog(ResultSetTable table, @NotNull DBObject sourceObject) {
        this(table, sourceObject, sourceObject.getConnectionHandler());
    }

    public ExportDataDialog(ResultSetTable table, @NotNull ExecutionResult executionResult) {
        this(table, null, executionResult.getConnectionHandler());
    }


    private ExportDataDialog(ResultSetTable table, @Nullable DBObject sourceObject, ConnectionHandler connectionHandler) {
        super(connectionHandler.getProject(), "Export Data", true);
        this.table = table;
        this.connectionHandler = connectionHandler;
        DataExportManager exportManager = DataExportManager.getInstance(connectionHandler.getProject());
        DataExportInstructions instructions = exportManager.getExportInstructions();
        boolean hasSelection = table.getSelectedRowCount() > 1 || table.getSelectedColumnCount() > 1;
        instructions.setBaseName(table.getName());
        exportDataForm = new ExportDataForm(instructions, hasSelection, connectionHandler, sourceObject);
        init();
    }


    protected String getDimensionServiceKey() {
        return "DBNavigator.ExportData";
    }

    @Nullable
    protected JComponent createCenterPanel() {
        return exportDataForm.getComponent();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                new DialogWrapperAction("Export") {
                    @Override
                    protected void doAction(ActionEvent actionEvent) {
                        doOKAction();
                    }
                },
                getCancelAction()};
    }

    protected void doOKAction() {
        if (exportDataForm.validateEntries()) {
            DataExportManager exportManager = DataExportManager.getInstance(connectionHandler.getProject());
            DataExportInstructions exportInstructions = exportDataForm.getExportInstructions();
            exportManager.setExportInstructions(exportInstructions);
            boolean success = exportManager.exportSortableTableContent(
                    table,
                    exportInstructions,
                    connectionHandler);
            if (success) super.doOKAction();
        }
    }
}
