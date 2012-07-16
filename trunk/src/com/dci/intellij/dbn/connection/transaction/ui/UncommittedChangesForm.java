package com.dci.intellij.dbn.connection.transaction.ui;

import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.common.ui.UIFormImpl;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.transaction.DatabaseTransactionManager;
import com.dci.intellij.dbn.connection.transaction.TransactionListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

public class UncommittedChangesForm extends UIFormImpl implements TransactionListener {
    private JTable changesTable;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JLabel connectionLabel;
    private JBScrollPane changesTableScrollPane;
    private JTextArea hintTextArea;
    private JButton commitButton;
    private JButton rollbackButton;
    private JPanel transactionActionsPanel;

    private ConnectionHandler connectionHandler;

    public UncommittedChangesForm(final ConnectionHandler connectionHandler, @Nullable String hintText, boolean showTransactionActions) {
        this.connectionHandler = connectionHandler;
        Project project = connectionHandler.getProject();
        if (getEnvironmentSettings(project).getVisibilitySettings().getDialogHeaders().value()) {
            headerPanel.setBackground(connectionHandler.getEnvironmentType().getColor());
        }

        connectionLabel.setIcon(connectionHandler.getIcon());
        connectionLabel.setText(connectionHandler.getName());
        changesTableScrollPane.getViewport().setBackground(changesTable.getBackground());

        hintTextArea.setBackground(mainPanel.getBackground());
        hintTextArea.setFont(mainPanel.getFont());
        if (StringUtil.isEmpty(hintText)) {
            hintTextArea.setVisible(false);
        } else {
            hintTextArea.setText(StringUtil.wrap(hintText, 90, ": ,."));
            hintTextArea.setVisible(true);
        }

        transactionActionsPanel.setVisible(showTransactionActions);
        if (showTransactionActions) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DatabaseTransactionManager transactionManager = DatabaseTransactionManager.getInstance(connectionHandler.getProject());
                    Object source = e.getSource();
                    if (source == commitButton) {
                        transactionManager.commit(connectionHandler, false);
                    } else if (source == rollbackButton) {
                        transactionManager.rollback(connectionHandler, false);
                    }
                }
            };

            commitButton.addActionListener(actionListener);
            rollbackButton.addActionListener(actionListener);

        }
        EventManager.subscribe(project, TransactionListener.TOPIC, this);
    }

    private void createUIComponents() {
        UncommittedChangesTableModel model = new UncommittedChangesTableModel(connectionHandler);
        changesTable = new UncommittedChangesTable(model);
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void dispose() {
        super.dispose();
        EventManager.unsubscribe(connectionHandler.getProject(), this);
        connectionHandler = null;
    }

    /********************************************************
     *                Transaction Listener                  *
     ********************************************************/
    @Override
    public void beforeCommit(ConnectionHandler connectionHandler) throws SQLException {
    }

    @Override
    public void beforeRollback(ConnectionHandler connectionHandler) throws SQLException {
    }

    @Override
    public void afterCommit(ConnectionHandler connectionHandler, boolean succeeded) throws SQLException {
        if (connectionHandler == this.connectionHandler && succeeded) {
            refreshForm(connectionHandler);
        }
    }

    @Override
    public void afterRollback(ConnectionHandler connectionHandler, boolean succeeded) throws SQLException {
        if (connectionHandler == this.connectionHandler && succeeded) {
            refreshForm(connectionHandler);
        }
    }

    private void refreshForm(final ConnectionHandler connectionHandler) {
        new SimpleLaterInvocator() {
            @Override
            public void run() {
                if (!isDisposed()) {
                    UncommittedChangesTableModel model = new UncommittedChangesTableModel(connectionHandler);
                    changesTable.setModel(model);
                    commitButton.setEnabled(false);
                    rollbackButton.setEnabled(false);
                }
            }
        }.start();
    }
}
