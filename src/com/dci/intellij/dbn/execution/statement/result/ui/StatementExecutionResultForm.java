package com.dci.intellij.dbn.execution.statement.result.ui;

import com.dci.intellij.dbn.common.thread.ConditionalLaterInvocator;
import com.dci.intellij.dbn.common.thread.ReadActionRunner;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.data.find.DataSearchComponent;
import com.dci.intellij.dbn.data.find.SearchableDataComponent;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModel;
import com.dci.intellij.dbn.data.record.RecordViewInfo;
import com.dci.intellij.dbn.data.ui.table.basic.BasicTable;
import com.dci.intellij.dbn.data.ui.table.resultSet.ResultSetTable;
import com.dci.intellij.dbn.execution.ExecutionManager;
import com.dci.intellij.dbn.execution.common.result.ui.ExecutionResultForm;
import com.dci.intellij.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.UIUtil;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;

public class StatementExecutionResultForm extends DBNFormImpl implements ExecutionResultForm<StatementExecutionCursorResult>, SearchableDataComponent {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel statusPanel;
    private JScrollPane resultScrollPane;
    private ResultSetTable resultTable;
    private JLabel statusLabel;
    private JPanel searchPanel;
    private JPanel resultPanel;
    private StatementExecutionCursorResult executionResult;
    private RecordViewInfo recordViewInfo;
    private DataSearchComponent dataSearchComponent;

    public StatementExecutionResultForm(final StatementExecutionCursorResult executionResult) {
        this.executionResult = executionResult;
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", false, "DBNavigator.ActionGroup.StatementExecutionResult");

        actionsPanel.add(actionToolbar.getComponent());

        recordViewInfo = new ReadActionRunner<RecordViewInfo>() {
            @Override
            protected RecordViewInfo run() {
                return new RecordViewInfo(
                                executionResult.getResultName(),
                                executionResult.getResultIcon());
            }
        }.start();

        resultPanel.setBorder(IdeBorderFactory.createBorder());
        resultTable = new ResultSetTable(executionResult.getTableModel(), true, recordViewInfo);

        resultScrollPane.setViewportView(resultTable);
        resultScrollPane.setRowHeaderView(resultTable.getTableGutter());
        resultScrollPane.getViewport().setBackground(resultTable.getBackground());

        JPanel panel = new JPanel();
        panel.setBorder(UIUtil.getTableHeaderCellBorder());
        resultScrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, panel);
        ActionUtil.registerDataProvider(resultTable, executionResult.getDataProvider(), false);
    }

    public void setExecutionResult(StatementExecutionCursorResult executionResult) {
        if (this.executionResult != executionResult) {
            this.executionResult = executionResult;
            reloadTableModel();
        }
    }

    public StatementExecutionCursorResult getExecutionResult() {
        return executionResult;
    }

    public void reloadTableModel() {
        new SimpleLaterInvocator() {
            public void execute() {
                if (executionResult != null) {
                    resultTable = new ResultSetTable(executionResult.getTableModel(), true, recordViewInfo);
                    resultScrollPane.setViewportView(resultTable);
                    resultScrollPane.setRowHeaderView(resultTable.getTableGutter());
                }
            }
        }.start();
    }

    public ResultSetTable getResultTable() {
        return resultTable;
    }

    public void updateVisibleComponents() {
        new ConditionalLaterInvocator() {
            public void execute() {
                ResultSetDataModel dataModel = executionResult.getTableModel();
                statusLabel.setText(executionResult.getExecutionInput().getConnectionHandler().getPresentableText() + ": " +
                        dataModel.getSize() + " records" + (dataModel.isResultSetExhausted() ? "" : " (Partial)"));
            }
        }.start();

    }

    public void dispose() {
        super.dispose();
        if (executionResult != null) {
            if (executionResult.hasResult()) {
                executionResult.getTableModel().dispose();
            }
            executionResult.getExecutionProcessor().reset();
            executionResult.dispose();
            executionResult = null;
        }
    }

    public void show() {
        Project project = executionResult.getProject();
        ExecutionManager.getInstance(project).selectResultTab(executionResult);
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void highlightLoading(boolean loading) {
        resultTable.setLoading(loading);
        resultTable.repaint();
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/
    public void showSearchHeader() {
        resultTable.clearSelection();

        if (dataSearchComponent == null) {
            dataSearchComponent = new DataSearchComponent(this);
            ActionUtil.registerDataProvider(dataSearchComponent.getSearchField(), executionResult.getDataProvider(), false);
            searchPanel.add(dataSearchComponent, BorderLayout.CENTER);
        } else {
            dataSearchComponent.initializeFindModel();
        }
        if (searchPanel.isVisible()) {
            dataSearchComponent.getSearchField().selectAll();
        } else {
            searchPanel.setVisible(true);
        }
        dataSearchComponent.getSearchField().requestFocus();

    }

    public void hideSearchHeader() {
        dataSearchComponent.resetFindModel();
        searchPanel.setVisible(false);
        resultTable.repaint();
        resultTable.requestFocus();
    }

    @Override
    public void cancelEditActions() {
    }

    @Override
    public BasicTable getTable() {
        return resultTable;
    }

    @Override
    public String getSelectedText() {
        return null;
    }
}
