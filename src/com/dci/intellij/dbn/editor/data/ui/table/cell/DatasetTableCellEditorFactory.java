package com.dci.intellij.dbn.editor.data.ui.table.cell;

import com.dci.intellij.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dci.intellij.dbn.data.model.ColumnInfo;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.data.type.DataTypeDefinition;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.editor.data.model.DatasetEditorColumnInfo;
import com.dci.intellij.dbn.editor.data.options.DataEditorSettings;
import com.dci.intellij.dbn.editor.data.options.DataEditorValueListPopupSettings;
import com.dci.intellij.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dci.intellij.dbn.object.DBColumn;
import com.intellij.openapi.Disposable;

import javax.swing.table.TableCellEditor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetTableCellEditorFactory implements Disposable {
    private Map<ColumnInfo, TableCellEditor> cache = new HashMap<ColumnInfo, TableCellEditor>();

    public TableCellEditor getCellEditor(ColumnInfo columnInfo, DatasetEditorTable table) {
        TableCellEditor tableCellEditor = cache.get(columnInfo);
        if (tableCellEditor == null) {
            DBDataType dataType = columnInfo.getDataType();
            tableCellEditor =
                dataType.getNativeDataType() != null ? createEditorForNativeType(columnInfo, table) :
                dataType.getDeclaredType() != null ? createEditorForDeclaredType(columnInfo, table) : null;
            cache.put(columnInfo, tableCellEditor);
        }
        return tableCellEditor;
    }

    private TableCellEditor createEditorForNativeType(ColumnInfo columnInfo, DatasetEditorTable table) {
        DataEditorSettings dataEditorSettings = DataEditorSettings.getInstance(table.getDatasetEditor().getProject());
        DBDataType dataType = columnInfo.getDataType();
        DBNativeDataType nativeDataType = dataType.getNativeDataType();
        DataTypeDefinition dataTypeDefinition = nativeDataType.getDataTypeDefinition();
        GenericDataType genericDataType = dataTypeDefinition.getGenericDataType();
        if (genericDataType == GenericDataType.NUMERIC) {
            return new DatasetTableCellEditor(table);
        }
        else if (genericDataType == GenericDataType.DATE_TIME) {
            DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);
            tableCellEditor.getEditorComponent().createCalendarPopup(false);
            return tableCellEditor;
        }
        else if (genericDataType == GenericDataType.LITERAL) {
            long dataLength = dataType.getLength();


            if (dataLength < dataEditorSettings.getQualifiedEditorSettings().getTextLengthThreshold()) {
                DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);

                tableCellEditor.getEditorComponent().createTextAreaPopup(true);

                final DatasetEditorColumnInfo dseColumnInfo = (DatasetEditorColumnInfo) columnInfo;
                DBColumn column = dseColumnInfo.getColumn();
                DataEditorValueListPopupSettings valueListPopupSettings = dataEditorSettings.getValueListPopupSettings();

                if (column.isForeignKey() || (dataLength <= valueListPopupSettings.getDataLengthThreshold() &&
                        (!column.isSinglePrimaryKey() || valueListPopupSettings.isActiveForPrimaryKeyColumns()))) {

                    ListPopupValuesProvider valuesProvider = new ListPopupValuesProvider() {
                        public List<String> getValues() {
                            return dseColumnInfo.getPossibleValues();
                        }
                    };
                    tableCellEditor.getEditorComponent().createValuesListPopup(valuesProvider, true);
                }
                return tableCellEditor;
            } else {
                DatasetTableCellEditorWithTextEditor tableCellEditor = new DatasetTableCellEditorWithTextEditor(table);
                tableCellEditor.setEditable(false);
                return tableCellEditor;
            }

        } else if (genericDataType.isLOB()) {
            DatasetTableCellEditorWithTextEditor tableCellEditor = new DatasetTableCellEditorWithTextEditor(table);
            tableCellEditor.setEditable(false);
            return tableCellEditor;
        }
        return null;
    }

    private TableCellEditor createEditorForDeclaredType(ColumnInfo columnInfo, DatasetEditorTable table) {
        return null;
    }

    public void dispose() {
        for (TableCellEditor cellEditor : cache.values()) {
            if (cellEditor instanceof Disposable) {
                Disposable disposable = (Disposable) cellEditor;
                disposable.dispose();
            }
        }
        cache.clear();
    }
}
