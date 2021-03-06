package com.dci.intellij.dbn.ddl;

import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.ddl.ui.DDLMappedNotificationPanel;
import com.dci.intellij.dbn.editor.ddl.DDLFileEditor;
import com.dci.intellij.dbn.language.common.DBLanguageFileType;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.lookup.DBObjectRef;
import com.dci.intellij.dbn.vfs.DatabaseEditableObjectFile;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.intellij.ide.FrameStateManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DDLMappedNotificationProvider extends EditorNotifications.Provider<DDLMappedNotificationPanel> {
    private static final Key<DDLMappedNotificationPanel> KEY = Key.create("DBNavigator.DDLMappedNotificationPanel");
    private Project project;

    public DDLMappedNotificationProvider(final Project project, @NotNull FrameStateManager frameStateManager) {
        this.project = project;

        EventManager.subscribe(project, DDLMappingListener.TOPIC, ddlMappingListener);
        EventManager.subscribe(project, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerAdapter);
    }

    DDLMappingListener ddlMappingListener = new DDLMappingListener() {
        @Override
        public void ddlFileDetached(VirtualFile virtualFile) {
            if (!project.isDisposed()) {
                EditorNotifications notifications = EditorNotifications.getInstance(project);
                notifications.updateNotifications(virtualFile);
            }
        }

        @Override
        public void ddlFileAttached(VirtualFile virtualFile) {
            if (!project.isDisposed()) {
                EditorNotifications notifications = EditorNotifications.getInstance(project);
                notifications.updateNotifications(virtualFile);
            }
        }
    };

    FileEditorManagerAdapter fileEditorManagerAdapter = new FileEditorManagerAdapter() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            updateDdlFileHeaders(file);
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            updateDdlFileHeaders(file);
        }

        private void updateDdlFileHeaders(VirtualFile file) {
            if (!project.isDisposed() && file instanceof DatabaseEditableObjectFile) {
                DatabaseEditableObjectFile editableObjectFile = (DatabaseEditableObjectFile) file;
                DBSchemaObject schemaObject = editableObjectFile.getObject();
                if (schemaObject != null) {
                    DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
                    List<VirtualFile> boundDDLFiles = attachmentManager.getBoundDDLFiles(schemaObject);
                    if (boundDDLFiles != null) {
                        EditorNotifications notifications = EditorNotifications.getInstance(project);
                        for (VirtualFile virtualFile : boundDDLFiles) {
                            notifications.updateNotifications(virtualFile);
                        }
                    }
                }

            }
        }
    };

    @Override
    public Key<DDLMappedNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public DDLMappedNotificationPanel createNotificationPanel(VirtualFile virtualFile, FileEditor fileEditor) {
        if (virtualFile instanceof DatabaseEditableObjectFile) {
            if (fileEditor instanceof DDLFileEditor) {
                DatabaseEditableObjectFile editableObjectFile = (DatabaseEditableObjectFile) virtualFile;
                DBSchemaObject editableObject = editableObjectFile.getObject();
                DDLFileEditor ddlFileEditor = (DDLFileEditor) fileEditor;
                VirtualFile ddlVirtualFile = ddlFileEditor.getVirtualFile();
                return createPanel(ddlVirtualFile, editableObject);
            }
            return null;
        } else {
            if (virtualFile.getFileType() instanceof DBLanguageFileType) {
                DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
                DBSchemaObject editableObject = attachmentManager.getEditableObject(virtualFile);
                if (editableObject != null) {
                    DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
                    if (databaseFileSystem.isFileOpened(editableObject))
                        return createPanel(virtualFile, editableObject);
                }
            }
        }
        return null;
    }

    private DDLMappedNotificationPanel createPanel(@NotNull final VirtualFile virtualFile, final DBSchemaObject editableObject) {
        final DBObjectRef<DBSchemaObject> editableObjectRef = editableObject.getRef();
        DDLMappedNotificationPanel panel = new DDLMappedNotificationPanel();
        panel.setText("This DDL file is attached to the database " + editableObject.getQualifiedNameWithType() + ". Changes done to the " + editableObject.getObjectType().getName() + " are automatically mirrored to this DDL file, overwriting any changes you may do to it.");
        panel.createActionLabel("Detach", new Runnable() {
            @Override
            public void run() {
                if (!project.isDisposed()) {
                    DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
                    attachmentManager.detachDDLFile(virtualFile);
                    DBSchemaObject editableObject = DBObjectRef.get(editableObjectRef);
                    if (editableObject != null) {
                        DatabaseFileSystem.getInstance().reopenEditor(editableObject);
                    }
                }
            }
        });
        return panel;
    }
}
