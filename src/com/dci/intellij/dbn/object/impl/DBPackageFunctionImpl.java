package com.dci.intellij.dbn.object.impl;

import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.object.DBPackage;
import com.dci.intellij.dbn.object.DBPackageFunction;
import com.dci.intellij.dbn.object.DBProgram;
import com.dci.intellij.dbn.object.common.DBObjectType;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBPackageFunctionImpl extends DBFunctionImpl implements DBPackageFunction {


    public DBPackageFunctionImpl(DBPackage packagee, ResultSet resultSet) throws SQLException {
        super(packagee, resultSet);
    }

    @Override
    public void initStatus(ResultSet resultSet) throws SQLException {}

    @Override
    public void initProperties() {
        getProperties().set(DBObjectProperty.NAVIGABLE);
    }

    public DBPackage getPackage() {
        return (DBPackage) getParentObject();
    }

    @Override
    public DBProgram getProgram() {
        return getPackage();
    }

    public int getOverload() {
        return overload;
    }

    public boolean isProgramMethod() {
        return true;
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.PACKAGE_FUNCTION;
    }

    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {}

    @Override
    public void dispose() {
        super.dispose();
    }
}
