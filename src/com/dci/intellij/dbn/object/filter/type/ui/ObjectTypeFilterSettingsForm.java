package com.dci.intellij.dbn.object.filter.type.ui;

import com.dci.intellij.dbn.browser.options.ObjectFilterChangeListener;
import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.options.ui.ConfigurationEditorForm;
import com.dci.intellij.dbn.common.ui.list.CheckBoxList;
import com.dci.intellij.dbn.object.filter.type.ObjectTypeFilterSetting;
import com.dci.intellij.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.ui.UIUtil;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ObjectTypeFilterSettingsForm extends ConfigurationEditorForm<ObjectTypeFilterSettings> {
    private JPanel mainPanel;
    private JScrollPane visibleObjectsScrollPane;
    private JCheckBox useMasterSettingsCheckBox;
    private JLabel visibleObjectTypesLabel;
    private CheckBoxList<ObjectTypeFilterSetting> visibleObjectsList;

    public ObjectTypeFilterSettingsForm(ObjectTypeFilterSettings configuration) {
        super(configuration);
        updateBorderTitleForeground(mainPanel);

        visibleObjectsList = new CheckBoxList<ObjectTypeFilterSetting>(configuration.getSettings());
        visibleObjectsScrollPane.setViewportView(visibleObjectsList);

        boolean masterSettingsAvailable = configuration.getMasterSettings() != null;
        useMasterSettingsCheckBox.setVisible(masterSettingsAvailable);
        if (masterSettingsAvailable) {
            visibleObjectTypesLabel.setVisible(false);
            useMasterSettingsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean enabled = !useMasterSettingsCheckBox.isSelected();
                    visibleObjectsList.setEnabled(enabled);
                    Color background = enabled ? UIUtil.getListBackground() : UIUtil.getComboBoxDisabledBackground();
                    visibleObjectsList.setBackground(background);
                    visibleObjectsList.clearSelection();
                }
            });
        } else {
            mainPanel.setBorder(null);
        }
        configuration.getUseMasterSettings().resetChanges(useMasterSettingsCheckBox);
        boolean enabled = !masterSettingsAvailable || !useMasterSettingsCheckBox.isSelected();
        visibleObjectsList.setEnabled(enabled);
        visibleObjectsList.setBackground(enabled ? UIUtil.getListBackground() : UIUtil.getComboBoxDisabledBackground());

        registerComponents(visibleObjectsList, useMasterSettingsCheckBox);
    }


    public boolean isSelected(ObjectTypeFilterSetting objectFilterEntry) {
        return visibleObjectsList.isSelected(objectFilterEntry);
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void applyChanges() throws ConfigurationException {
        ObjectTypeFilterSettings objectFilterSettings = getConfiguration();
        boolean notifyFilterListeners = objectFilterSettings.isModified();
        visibleObjectsList.applyChanges();
        if (notifyFilterListeners) {
            ObjectFilterChangeListener listener = EventManager.notify(objectFilterSettings.getProject(), ObjectFilterChangeListener.TOPIC);
            listener.filterChanged(objectFilterSettings.getElementFilter());
        }

        objectFilterSettings.getUseMasterSettings().applyChanges(useMasterSettingsCheckBox);
    }

    @Override
    public void resetChanges() {}
}
