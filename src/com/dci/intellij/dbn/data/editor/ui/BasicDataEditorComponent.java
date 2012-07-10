package com.dci.intellij.dbn.data.editor.ui;

import javax.swing.JTextField;

public class BasicDataEditorComponent extends JTextField implements DataEditorComponent{
    private UserValueHolder userValueHolder;
    public JTextField getTextField() {
        return this;
    }

    public void setUserValueHolder(UserValueHolder userValueHolder) {
        this.userValueHolder = userValueHolder;
    }

    public UserValueHolder getUserValueHolder() {
        return userValueHolder;
    }
}