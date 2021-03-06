package com.dci.intellij.dbn.editor.console.ui;

import com.dci.intellij.dbn.common.ui.AutoCommitLabel;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.editor.console.SQLConsoleEditor;
import com.intellij.openapi.actionSystem.ActionToolbar;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class SQLConsoleEditorToolbarForm extends DBNFormImpl {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private AutoCommitLabel autoCommitLabel;

    public SQLConsoleEditorToolbarForm(SQLConsoleEditor fileEditor) {
        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true, "DBNavigator.ActionGroup.FileEditor");
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        actionToolbar.setTargetComponent(fileEditor.getComponent());

        ConnectionHandler connectionHandler = fileEditor.getVirtualFile().getConnectionHandler();
        autoCommitLabel.setConnectionHandler(connectionHandler);
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void dispose() {
        super.dispose();
        autoCommitLabel.dispose();
    }
}
