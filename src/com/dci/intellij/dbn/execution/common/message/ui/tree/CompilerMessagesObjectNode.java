package com.dci.intellij.dbn.execution.common.message.ui.tree;

import com.dci.intellij.dbn.common.ui.tree.TreeEventType;
import com.dci.intellij.dbn.common.ui.tree.TreeUtil;
import com.dci.intellij.dbn.execution.compiler.CompilerMessage;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.vfs.DatabaseEditableObjectFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public class CompilerMessagesObjectNode extends BundleTreeNode {
    private DatabaseEditableObjectFile databaseFile;

    public CompilerMessagesObjectNode(CompilerMessagesNode parent, DatabaseEditableObjectFile databaseFile) {
        super(parent);
        this.databaseFile = databaseFile;
    }

    public DatabaseEditableObjectFile getVirtualFile() {
        return databaseFile;
    }

    @Nullable
    public DBSchemaObject getObject() {
        return databaseFile.getObject();
    }

    public TreePath addCompilerMessage(CompilerMessage compilerMessage) {
        if (children.size() > 0) {
            CompilerMessageNode firstChild = (CompilerMessageNode) children.get(0);
            if (firstChild.getCompilerMessage().getCompilerResult() != compilerMessage.getCompilerResult()) {
                children.clear();
            }
        }
        CompilerMessageNode messageNode = new CompilerMessageNode(this, compilerMessage);
        children.add(messageNode);
        getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        return TreeUtil.createTreePath(messageNode);
    }

    public TreePath getTreePath(CompilerMessage compilerMessage) {
        for (MessagesTreeNode messageNode : children) {
            CompilerMessageNode compilerMessageNode = (CompilerMessageNode) messageNode;
            if (compilerMessageNode.getCompilerMessage() == compilerMessage) {
                return TreeUtil.createTreePath(compilerMessageNode);
            }
        }
        return null;
    }
}
