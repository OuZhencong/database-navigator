package com.dci.intellij.dbn.data.model.resultSet;

import com.dci.intellij.dbn.data.model.basic.BasicColumnInfo;
import com.dci.intellij.dbn.data.type.DBDataType;
import com.dci.intellij.dbn.data.type.DBNativeDataType;
import com.dci.intellij.dbn.object.common.DBObjectBundle;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ResultSetColumnInfo extends BasicColumnInfo {
    int resultSetColumnIndex;
    public ResultSetColumnInfo(DBObjectBundle objectBundle, ResultSet resultSet, int columnIndex) throws SQLException {
        this(columnIndex);
        ResultSetMetaData metaData = resultSet.getMetaData();
        name = metaData.getColumnName(resultSetColumnIndex);

        String dataTypeName = metaData.getColumnTypeName(resultSetColumnIndex);
        int precision = getPrecision(metaData);
        int scale = metaData.getScale(resultSetColumnIndex);
        DBNativeDataType nativeDataType = objectBundle.getNativeDataType(dataTypeName);
        dataType = new DBDataType(nativeDataType, precision, scale);
    }

    public ResultSetColumnInfo(int columnIndex) {
        super(null, null, columnIndex);
        resultSetColumnIndex = columnIndex + 1;
    }

    public ResultSetColumnInfo(int columnIndex, int resultSetColumnIndex ) {
        super(null, null, columnIndex);
        this.resultSetColumnIndex = resultSetColumnIndex;
    }


    // lenient approach for oracle bug returning the size of LOBs instead of the precision.
    private int getPrecision(ResultSetMetaData metaData) throws SQLException {
        try {
            return metaData.getPrecision(resultSetColumnIndex);
        } catch (NumberFormatException e) {
            return 4000;
        }
    }

    public int getResultSetColumnIndex() {
        return resultSetColumnIndex;
    }
}
