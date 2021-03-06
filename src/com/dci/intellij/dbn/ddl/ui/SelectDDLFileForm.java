package com.dci.intellij.dbn.ddl.ui;

import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.DBNHeaderForm;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

public class SelectDDLFileForm extends DBNFormImpl implements DBNForm {
    private JPanel mainPanel;
    private JTextArea hintTextArea;
    private JList filesList;
    private JPanel headerPanel;
    private JCheckBox doNotPromptCheckBox;

    public SelectDDLFileForm(DBSchemaObject object, List<VirtualFile> virtualFiles, String hint, boolean isFileOpenEvent) {
        Project project = object.getProject();
        String headerTitle = object.getSchema().getName() + "." + object.getName();
        Icon headerIcon = object.getOriginalIcon();
        Color headerBackground = UIUtil.getPanelBackground();
        if (getEnvironmentSettings(project).getVisibilitySettings().getDialogHeaders().value()) {
            headerBackground = object.getEnvironmentType().getColor();
        }
        DBNHeaderForm headerForm = new DBNHeaderForm(
                headerTitle,
                headerIcon,
                headerBackground);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        hintTextArea.setText(hint);
        hintTextArea.setBackground(mainPanel.getBackground());
        hintTextArea.setFont(mainPanel.getFont());
        DefaultListModel listModel = new DefaultListModel();
        for (VirtualFile virtualFile : virtualFiles) {
            listModel.addElement(virtualFile);
        }
        filesList.setModel(listModel);
        filesList.setCellRenderer(new FileListCellRenderer(project));
        filesList.setSelectedIndex(0);

        if (!isFileOpenEvent) mainPanel.remove(doNotPromptCheckBox);
    }

    public Object[] getSelection() {
        return filesList.getSelectedValues();
    }

    public void selectAll() {
        filesList.setSelectionInterval(0, filesList.getModel().getSize() -1);
    }

    public void selectNone() {
        filesList.clearSelection();
    }

    public boolean isDoNotPromptSelected() {
        return doNotPromptCheckBox.isSelected();
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void dispose() {
        super.dispose();
    }
}
