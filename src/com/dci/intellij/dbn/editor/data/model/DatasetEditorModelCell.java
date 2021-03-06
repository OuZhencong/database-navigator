package com.dci.intellij.dbn.editor.data.model;


import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.common.util.CommonUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.value.LazyLoadedValue;
import com.dci.intellij.dbn.editor.data.DatasetEditorError;
import com.dci.intellij.dbn.editor.data.ui.DatasetEditorErrorForm;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dci.intellij.dbn.object.DBDataset;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatasetEditorModelCell extends ResultSetDataModelCell implements ChangeListener {
    private Object originalUserValue;
    private DatasetEditorError error;
    private boolean isModified;

    public DatasetEditorModelCell(DatasetEditorModelRow row, ResultSet resultSet, DatasetEditorColumnInfo columnInfo) throws SQLException {
        super(row, resultSet, columnInfo);
        originalUserValue = getUserValue();
    }

    @Override
    public DatasetEditorColumnInfo getColumnInfo() {
        return (DatasetEditorColumnInfo) super.getColumnInfo();
    }

    public void updateUserValue(Object userValue, boolean bulk) {
        boolean sameValue = compareUserValues(userValue, getUserValue());
        if (hasError() || !sameValue) {
            DatasetEditorModelRow row = getRow();
            ResultSet resultSet;
            try {
                resultSet = row.isInsert() ? row.getResultSet() : row.scrollResultSet();
            } catch (Exception e) {
                e.printStackTrace();
                MessageUtil.showErrorDialog("Could not update cell value for " + getColumnInfo().getName() + ".", e);
                return;
            }
            DBDataType dataType = getColumnInfo().getDataType();

            boolean isLargeObject = dataType.getNativeDataType().isLOB();
            try {
                clearError();
                int columnIndex = getColumnInfo().getColumnIndex() + 1;
                if (isLargeObject) {
                    LazyLoadedValue lazyLoadedValue = (LazyLoadedValue) getUserValue();
                    lazyLoadedValue.updateValue(resultSet, columnIndex, (String) userValue);
                } else {
                    dataType.setValueToResultSet(resultSet, columnIndex, userValue);
                }

                if (!row.isInsert()) resultSet.updateRow();
            } catch (Exception e) {
                DatasetEditorError error = new DatasetEditorError(getConnectionHandler(), e);

                // error may affect other cells in the row (e.g. foreign key constraint for multiple primary key)
                if (e instanceof SQLException) getRow().notifyError(error, false, !bulk);

                // if error was not notified yet on row level, notify it on cell isolation level
                if (!error.isNotified()) notifyError(error, !bulk);
            } finally {
                if (!sameValue) {
                    if (!isLargeObject) {
                        setUserValue(userValue);
                    }
                    getConnectionHandler().notifyChanges(getDataset().getVirtualFile());
                    EventManager.notify(getProject(), DatasetEditorModelCellValueListener.TOPIC).valueChanged(this);
                }

            }

            if (!row.isInsert() && !getConnectionHandler().isAutoCommit()) {
                isModified = true;
                row.setModified(true);
            }
        }
    }

    protected DBDataset getDataset() {
        return getRow().getModel().getDataset();
    }

    private boolean compareUserValues(Object value1, Object value2) {
        if (value1 != null && value2 != null) {
            if (value1.equals(value2)) {
                return true;
            }
            // user input may not contain the entire precision (e.g. date time format)
            String formattedValue1 = Formatter.getInstance(getProject()).formatObject(value1);
            String formattedValue2 = Formatter.getInstance(getProject()).formatObject(value2);
            return formattedValue1.equals(formattedValue2);
        }
        
        return CommonUtil.safeEqual(value1, value2);
    }

    public void updateUserValue(Object userValue, String errorMessage) {
        if (!CommonUtil.safeEqual(userValue, getUserValue()) || hasError()) {
            DatasetEditorModelRow row = getRow();
            DatasetEditorError error = new DatasetEditorError(errorMessage, getColumnInfo().getColumn());
            getRow().notifyError(error, true, true);
            setUserValue(userValue);
            ConnectionHandler connectionHandler = getConnectionHandler();
            if (!row.isInsert() && !connectionHandler.isAutoCommit()) {
                isModified = true;
                row.setModified(true);
                connectionHandler.ping(false);
            }
        }
    }

    public boolean matches(DatasetEditorModelCell remoteCell, boolean lenient) {
        if (CommonUtil.safeEqual(getUserValue(), remoteCell.getUserValue())){
            return true;
        }
        if (lenient && (getRow().isNew() || getRow().isModified()) && getUserValue() == null && remoteCell.getUserValue() != null) {
            return true;
        }
        return false;
    }

    private ConnectionHandler getConnectionHandler() {
        return getRow().getModel().getConnectionHandler();
    }

    private DatasetEditorTable getEditorTable() {
        return getRow().getModel().getEditorTable();
    }

    public void edit() {
        if (getIndex() > 0) {
            DatasetEditorTable table = getEditorTable();
            table.editCellAt(getRow().getIndex(), getIndex());
        }
    }

    public void editPrevious() {
        if (getIndex() > 0) {
            DatasetEditorTable table = getEditorTable();
            table.editCellAt(getRow().getIndex(), getIndex() -1);
        }
    }

    public void editNext(){
        if (getIndex() < getRow().getCells().size()-1) {
            DatasetEditorTable table = getEditorTable();
            table.editCellAt(getRow().getIndex(), getIndex() + 1);
        }
    }

    public DatasetEditorModelRow getRow() {
        return (DatasetEditorModelRow) super.getRow();
    }

    public void setOriginalUserValue(Object value) {
        if (originalUserValue == null) {
            isModified = value != null;
        } else {
            isModified = !originalUserValue.equals(value);
        }
        this.originalUserValue = value;
    }

    public Object getOriginalUserValue() {
        return originalUserValue;
    }

    public boolean isModified() {
        return isModified;
    }

    public boolean isEditing() {
        DatasetEditorTable table = getEditorTable();
        return table.isEditing() &&
               table.isCellSelected(getRow().getIndex(), getIndex());
    }

    public boolean isNavigable() {
        return getColumnInfo().getColumn().isForeignKey() && getUserValue() != null;
    }

    public void notifyCellUpdated() {
        getRow().getModel().notifyCellUpdated(getRow().getIndex(), getIndex());
    }

    public void scrollToVisible() {
        DatasetEditorTable table = getEditorTable();
        table.scrollRectToVisible(table.getCellRect(getRow().getIndex(), getIndex(), true));
    }

    /*********************************************************
     *                    ChangeListener                     *
     *********************************************************/
    public void stateChanged(ChangeEvent e) {
        notifyCellUpdated();
    }


    /*********************************************************
     *                        ERROR                          *
     *********************************************************/
    public boolean hasError() {
        if (error != null && error.isDirty()) {
            error = null;
        }
        return error != null;
    }

    public boolean notifyError(DatasetEditorError error, final boolean showPopup) {
        error.setNotified(true);
        if(!CommonUtil.safeEqual(this.error, error)) {
            clearError();
            this.error = error;
            notifyCellUpdated();
            if (showPopup) scrollToVisible();
            if (isEditing()) {
                DatasetEditorTable table = getEditorTable();
                TableCellEditor tableCellEditor = table.getCellEditor();
                if (tableCellEditor instanceof DatasetTableCellEditor) {
                    DatasetTableCellEditor cellEditor = (DatasetTableCellEditor) tableCellEditor;
                    cellEditor.highlight(DatasetTableCellEditor.HIGHLIGHT_TYPE_ERROR);
                }
            }
            error.addChangeListener(this);
            if (showPopup) showErrorPopup();
            return true;
        }
        return false;
    }

    public void showErrorPopup() {
        new SimpleLaterInvocator() {
            public void execute() {
                if (!isDisposed()) {
                    DatasetEditorModel model = getRow().getModel();
                    if (!model.getEditorTable().isShowing()) {
                        DBDataset dataset = getDataset();
                        DatabaseFileSystem.getInstance().openEditor(dataset);
                    }
                    if (error != null) {
                        DatasetEditorErrorForm errorForm = new DatasetEditorErrorForm(DatasetEditorModelCell.this);
                        errorForm.show();
                    }
                }
            }
        }.start();
    }

    public void clearError() {
        if (error != null ) {
            error.markDirty();
            error = null;
        }
    }

    public DatasetEditorError getError() {
        return error;
    }

    @Override
    public void dispose() {
        super.dispose();
        originalUserValue = null;
        error = null;
    }

    public void revertChanges() {
        if (isModified) {
            updateUserValue(originalUserValue, false);
            this.isModified = false;
        }
    }
}
