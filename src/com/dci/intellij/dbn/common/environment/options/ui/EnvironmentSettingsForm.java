package com.dci.intellij.dbn.common.environment.options.ui;

import com.dci.intellij.dbn.common.environment.EnvironmentChangeListener;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.environment.EnvironmentTypeBundle;
import com.dci.intellij.dbn.common.environment.options.EnvironmentSettings;
import com.dci.intellij.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EnvironmentSettingsForm extends ConfigurationEditorForm<EnvironmentSettings> {
    private JPanel mainPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton resetDefaultsButton;
    private JBScrollPane environmentTypesTableScrollPane;
    private JCheckBox connectionTabsCheckBox;
    private JCheckBox objectEditorTabsCheckBox;
    private JCheckBox scriptEditorTabsCheckBox;
    private JCheckBox dialogHeadersCheckBox;
    private JCheckBox executionResultTabsCheckBox;
    private JPanel environmentTypesPanel;
    private JPanel environmentApplicabilityPanel;
    private EnvironmentTypesEditorTable environmentTypesTable;

    public EnvironmentSettingsForm(EnvironmentSettings settings) {
        super(settings);
        environmentTypesTable = new EnvironmentTypesEditorTable(settings.getProject(), settings.getEnvironmentTypes());
        environmentTypesTableScrollPane.setViewportView(environmentTypesTable);
        environmentTypesTableScrollPane.setPreferredSize(new Dimension(200, 80));

        updateBorderTitleForeground(environmentTypesPanel);
        updateBorderTitleForeground(environmentApplicabilityPanel);

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        visibilitySettings.getConnectionTabs().resetChanges(connectionTabsCheckBox);
        visibilitySettings.getObjectEditorTabs().resetChanges(objectEditorTabsCheckBox);
        visibilitySettings.getScriptEditorTabs().resetChanges(scriptEditorTabsCheckBox);
        visibilitySettings.getDialogHeaders().resetChanges(dialogHeadersCheckBox);
        visibilitySettings.getExecutionResultTabs().resetChanges(executionResultTabsCheckBox);

        environmentTypesTable.getSelectionModel().addListSelectionListener(selectionListener);
        updateButtons();

        addButton.addActionListener(actionListener);
        removeButton.addActionListener(actionListener);
        moveUpButton.addActionListener(actionListener);
        moveDownButton.addActionListener(actionListener);
        resetDefaultsButton.addActionListener(actionListener);

        registerComponents(
                addButton,
                removeButton,
                moveUpButton,
                moveDownButton,
                resetDefaultsButton,
                connectionTabsCheckBox,
                objectEditorTabsCheckBox,
                scriptEditorTabsCheckBox,
                dialogHeadersCheckBox,
                executionResultTabsCheckBox,
                environmentTypesTable);
    }
    
    public JPanel getComponent() {
        return mainPanel;
    }
    
    private ListSelectionListener selectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            updateButtons();
        }
    };

    private ActionListener actionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == addButton) {
                environmentTypesTable.insertRow();
            } else if (source == removeButton) {
                environmentTypesTable.removeRow();
            }  else if (source == moveUpButton) {
                environmentTypesTable.moveRowUp();
            } else if (source == moveDownButton) {
                environmentTypesTable.moveRowDown();
            } else if (source == resetDefaultsButton) {
                TableCellEditor cellEditor = environmentTypesTable.getCellEditor();
                if (cellEditor != null) {
                    cellEditor.cancelCellEditing();
                }
                environmentTypesTable.setEnvironmentTypes(EnvironmentTypeBundle.DEFAULT);
            }
            updateButtons();
        }
    };

    private void updateButtons() {
        removeButton.setEnabled(
                environmentTypesTable.getModel().getRowCount()  > 0 &&
                environmentTypesTable.getSelectedRowCount() > 0);
        moveUpButton.setEnabled(
                environmentTypesTable.getSelectedRowCount() > 0 &&
                environmentTypesTable.getSelectedRow() > 0);
        moveDownButton.setEnabled(
                environmentTypesTable.getSelectedRowCount() > 0 &&
                environmentTypesTable.getSelectedRow() < environmentTypesTable.getModel().getRowCount() - 1);
    }


    public void applyChanges() throws ConfigurationException {
        EnvironmentSettings settings = getConfiguration();
        EnvironmentTypesTableModel model = environmentTypesTable.getModel();
        model.validate();
        EnvironmentTypeBundle environmentTypeBundle = model.getEnvironmentTypes();
        boolean settingsChanged = settings.setEnvironmentTypes(environmentTypeBundle);

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        boolean visibilityChanged =
            visibilitySettings.getConnectionTabs().applyChanges(connectionTabsCheckBox) ||
            visibilitySettings.getObjectEditorTabs().applyChanges(objectEditorTabsCheckBox) ||
            visibilitySettings.getScriptEditorTabs().applyChanges(scriptEditorTabsCheckBox)||
            visibilitySettings.getDialogHeaders().applyChanges(dialogHeadersCheckBox)||
            visibilitySettings.getExecutionResultTabs().applyChanges(executionResultTabsCheckBox);

        if (visibilityChanged) {
            EnvironmentChangeListener listener = EventManager.notify(getConfiguration().getProject(), EnvironmentChangeListener.TOPIC);
            listener.environmentVisibilitySettingsChanged();
        }

        if (settingsChanged) {
            EnvironmentChangeListener listener = EventManager.notify(getConfiguration().getProject(), EnvironmentChangeListener.TOPIC);
            for (EnvironmentType environmentType : environmentTypeBundle.getEnvironmentTypes()) {
                listener.environmentConfigChanged(environmentType.getId());
            }
        }
        
    }

    public void resetChanges() {
        EnvironmentSettings settings = getConfiguration();
        environmentTypesTable.getModel().setEnvironmentTypes(settings.getEnvironmentTypes());

        EnvironmentVisibilitySettings visibilitySettings = settings.getVisibilitySettings();
        visibilitySettings.getConnectionTabs().resetChanges(connectionTabsCheckBox);
        visibilitySettings.getObjectEditorTabs().resetChanges(objectEditorTabsCheckBox);
        visibilitySettings.getScriptEditorTabs().resetChanges(scriptEditorTabsCheckBox);
        visibilitySettings.getDialogHeaders().resetChanges(dialogHeadersCheckBox);
        visibilitySettings.getExecutionResultTabs().resetChanges(executionResultTabsCheckBox);
    }
}
