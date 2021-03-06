package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.object.DBProgram;
import com.dci.intellij.dbn.object.DBType;
import com.dci.intellij.dbn.object.DBTypeFunction;
import com.dci.intellij.dbn.object.common.DBObjectType;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBTypeFunctionImpl extends DBFunctionImpl implements DBTypeFunction {
    public DBTypeFunctionImpl(DBType type, ResultSet resultSet) throws SQLException {
        super(type, resultSet);
    }

    @Override
    public void initStatus(ResultSet resultSet) throws SQLException {}

    @Override
    public void initProperties() {
        getProperties().set(DBObjectProperty.NAVIGABLE);
    }

    public DBType getType() {
        return (DBType) getParentObject();
    }

    @Override
    public DBProgram getProgram() {
        return getType();
    }

    public int getOverload() {
        return overload;
    }

    public boolean isProgramMethod() {
        return true;
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.TYPE_FUNCTION;
    }

    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {}
}