package com.dci.intellij.dbn.execution.method.ui;

import com.dci.intellij.dbn.common.dispose.DisposerUtil;
import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.data.editor.ui.TextFieldWithPopup;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.data.type.DataTypeDefinition;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.object.DBArgument;
import com.dci.intellij.dbn.object.DBType;
import com.dci.intellij.dbn.object.DBTypeAttribute;
import com.intellij.util.ui.UIUtil;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public class MethodExecutionArgumentForm extends DBNFormImpl implements DBNForm {
    private JPanel mainPanel;
    private JLabel argumentLabel;
    private JLabel argumentTypeLabel;
    private JPanel typeAttributesPanel;
    private JPanel inputFieldPanel;


    private JComponent inputComponent;
    private JTextField inputTextField;

    private DBArgument argument;
    private List<MethodExecutionTypeAttributeForm> typeAttributeForms = new ArrayList<MethodExecutionTypeAttributeForm>();
    private MethodExecutionForm executionComponent;

    public MethodExecutionArgumentForm(DBArgument argument, MethodExecutionForm executionComponent) {
        this.argument = argument;
        this.executionComponent = executionComponent;
        argumentLabel.setText(argument.getName());
        argumentLabel.setIcon(argument.getIcon());

        DBDataType dataType = argument.getDataType();

        argumentTypeLabel.setForeground(UIUtil.getInactiveTextColor());
        if (dataType.isDeclared()) {
            DBType declaredType = dataType.getDeclaredType();
            argumentTypeLabel.setIcon(declaredType.getIcon());
            argumentTypeLabel.setText(declaredType.getName());

            typeAttributesPanel.setLayout(new BoxLayout(typeAttributesPanel, BoxLayout.Y_AXIS));
            List<DBTypeAttribute> attributes = declaredType.getAttributes();
            for (DBTypeAttribute attribute : attributes) {
                addAttributePanel(attribute);
            }

        } else {
            argumentTypeLabel.setText(dataType.getQualifiedName());
            typeAttributesPanel.setVisible(false);
        }


        if (argument.isInput() && !dataType.isDeclared() && dataType.getNativeDataType() != null) {
            DBNativeDataType nativeDataType = dataType.getNativeDataType();
            DataTypeDefinition dataTypeDefinition = nativeDataType.getDataTypeDefinition();
            GenericDataType genericDataType = dataTypeDefinition.getGenericDataType();

            if (genericDataType == GenericDataType.DATE_TIME) {
                TextFieldWithPopup inputField = new TextFieldWithPopup(argument.getProject());
                inputField.setPreferredSize(new Dimension(200, -1));
                inputField.createCalendarPopup(false);
                inputComponent  = inputField;
                inputTextField = inputField.getTextField();

            } else {
                inputTextField = new JTextField();
                inputTextField.setPreferredSize(new Dimension(200, -1));
                inputComponent = inputTextField;
            }
            String value = executionComponent.getExecutionInput().getInputValue(argument);
            inputTextField.setText(value);
            inputFieldPanel.add(inputComponent, BorderLayout.CENTER);
            inputTextField.setDisabledTextColor(inputTextField.getForeground());
        } else {
            inputFieldPanel.setVisible(false);
        }
    }

    private void addAttributePanel(DBTypeAttribute attribute) {
        MethodExecutionTypeAttributeForm argumentComponent = new MethodExecutionTypeAttributeForm(argument, attribute, executionComponent);
        typeAttributesPanel.add(argumentComponent.getComponent());
        typeAttributeForms.add(argumentComponent);
    }


    public JPanel getComponent() {
        return mainPanel;
    }

    public void updateExecutionInput() {
        if (typeAttributeForms.size() >0 ) {
            for (MethodExecutionTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                typeAttributeComponent.updateExecutionInput();
            }
        } else {
            String value = CommonUtil.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
            executionComponent.getExecutionInput().setInputValue(argument, value);
        }
    }

    protected int[] getMetrics(int[] metrics) {
        if (typeAttributeForms.size() > 0) {
            for (MethodExecutionTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                metrics = typeAttributeComponent.getMetrics(metrics);
            }
        }

        return new int[] {
            (int) Math.max(metrics[0], argumentLabel.getPreferredSize().getWidth()),
            (int) Math.max(metrics[1], inputFieldPanel.getPreferredSize().getWidth())};
    }

    protected void adjustMetrics(int[] metrics) {
        if (typeAttributeForms.size() > 0) {
            for (MethodExecutionTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                typeAttributeComponent.adjustMetrics(metrics);
            }
        }
        argumentLabel.setPreferredSize(new Dimension(metrics[0], argumentLabel.getHeight()));
        inputFieldPanel.setPreferredSize(new Dimension(metrics[1], inputFieldPanel.getHeight()));
    }

    public void addDocumentListener(DocumentListener documentListener){
        if (inputTextField != null) {
            inputTextField.getDocument().addDocumentListener(documentListener);
        }

        for (MethodExecutionTypeAttributeForm typeAttributeComponent : typeAttributeForms){
            typeAttributeComponent.addDocumentListener(documentListener);
        }
    }

    public void dispose() {
        super.dispose();
        DisposerUtil.dispose(typeAttributeForms);
        argument = null;
        typeAttributeForms = null;
        executionComponent = null;
    }

    public int getScrollUnitIncrement() {
        return (int) (typeAttributeForms.size() > 0 ?
                typeAttributeForms.get(0).getComponent().getPreferredSize().getHeight() :
                getComponent().getPreferredSize().getHeight());
    }
}
