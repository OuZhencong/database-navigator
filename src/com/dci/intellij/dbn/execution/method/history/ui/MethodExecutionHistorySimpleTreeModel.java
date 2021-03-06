package com.dci.intellij.dbn.execution.method.history.ui;

import com.dci.intellij.dbn.execution.method.MethodExecutionInput;
import com.dci.intellij.dbn.object.lookup.DBMethodRef;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class MethodExecutionHistorySimpleTreeModel extends MethodExecutionHistoryTreeModel {
    public MethodExecutionHistorySimpleTreeModel(List<MethodExecutionInput> executionInputs) {
        super(executionInputs);
        for (MethodExecutionInput executionInput : executionInputs) {
            RootTreeNode rootNode = getRoot();

            ConnectionTreeNode connectionNode = rootNode.getConnectionNode(executionInput);
            SchemaTreeNode schemaNode = connectionNode.getSchemaNode(executionInput);
            schemaNode.getMethodNode(executionInput);
        }
    }

    public List<MethodExecutionInput> getExecutionInputs() {
        List<MethodExecutionInput> executionInputs = new ArrayList<MethodExecutionInput>();
        for (TreeNode connectionTreeNode : getRoot().getChildren()) {
            ConnectionTreeNode connectionNode = (ConnectionTreeNode) connectionTreeNode;
            for (TreeNode schemaTreeNode : connectionNode.getChildren()) {
                SchemaTreeNode schemaNode = (SchemaTreeNode) schemaTreeNode;
                for (TreeNode node : schemaNode.getChildren()) {
                    MethodTreeNode methodNode = (MethodTreeNode) node;
                    MethodExecutionInput executionInput =
                            getExecutionInput(connectionNode, schemaNode, methodNode);

                    if (executionInput != null) {
                        executionInputs.add(executionInput);
                    }
                }
            }
        }
        return executionInputs;
    }

    private MethodExecutionInput getExecutionInput(
            ConnectionTreeNode connectionNode,
            SchemaTreeNode schemaNode,
            MethodTreeNode methodNode) {
        for (MethodExecutionInput executionInput : executionInputs) {
            DBMethodRef methodRef = executionInput.getMethodRef();
            if (executionInput.getConnectionHandler().getId().equals(connectionNode.getConnectionHandler().getId()) &&
                methodRef.getSchemaName().equalsIgnoreCase(schemaNode.getName()) &&
                methodRef.getQualifiedMethodName().equalsIgnoreCase(methodNode.getName()) &&
                methodRef.getOverload() == methodNode.getOverload() ) {

                return executionInput;
            }
        }
        return null;
    }

    protected String getMethodName(MethodExecutionInput executionInput) {
        return executionInput.getMethodRef().getQualifiedMethodName();
    }

    @Override
    public TreePath getTreePath(MethodExecutionInput executionInput) {
        List<MethodExecutionHistoryTreeNode> path = new ArrayList<MethodExecutionHistoryTreeNode>();
        MethodExecutionHistoryTreeModel.RootTreeNode rootTreeNode = getRoot();
        path.add(rootTreeNode);
        ConnectionTreeNode connectionTreeNode = rootTreeNode.getConnectionNode(executionInput);
        path.add(connectionTreeNode);
        SchemaTreeNode schemaTreeNode = connectionTreeNode.getSchemaNode(executionInput);
        path.add(schemaTreeNode);
        MethodTreeNode methodTreeNode = schemaTreeNode.getMethodNode(executionInput);
        path.add(methodTreeNode);
        
        return new TreePath(path.toArray());
    }
}