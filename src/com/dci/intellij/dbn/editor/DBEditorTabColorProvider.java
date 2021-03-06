package com.dci.intellij.dbn.editor;

import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.environment.options.EnvironmentSettings;
import com.dci.intellij.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.mapping.FileConnectionMappingManager;
import com.dci.intellij.dbn.language.common.DBLanguageFileType;
import com.dci.intellij.dbn.options.general.GeneralProjectSettings;
import com.dci.intellij.dbn.vfs.DatabaseObjectFile;
import com.dci.intellij.dbn.vfs.SQLConsoleFile;
import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.Color;

public class DBEditorTabColorProvider implements EditorTabColorProvider{

    public static final Color DEFAULT_COLOR = new Color(218, 234, 255);

    @Override
    public Color getEditorTabColor(Project project, VirtualFile file) {
        if (file.getFileType() instanceof DBLanguageFileType) {
            ConnectionHandler connectionHandler = getConnectionHandler(file, project);
            if (connectionHandler == null) {
                return null;
            } else {
                EnvironmentSettings environmentSettings = GeneralProjectSettings.getInstance(connectionHandler.getProject()).getEnvironmentSettings();
                EnvironmentVisibilitySettings visibilitySettings = environmentSettings.getVisibilitySettings();
                EnvironmentType environmentType = connectionHandler.getEnvironmentType();
                if (file instanceof SQLConsoleFile || file instanceof DatabaseObjectFile) {
                    if (visibilitySettings.getObjectEditorTabs().value()) {
                        return environmentType == null ? null : environmentType.getColor();
                    }
                } else {
                    if (visibilitySettings.getScriptEditorTabs().value()) {
                        return environmentType == null ? null : environmentType.getColor();
                    }
                }
                return null;
            }
        }
        return null;
    }
    
    public static ConnectionHandler getConnectionHandler(VirtualFile file, Project project) {
        if (file instanceof SQLConsoleFile) {
            SQLConsoleFile consoleFile = (SQLConsoleFile) file;
            return consoleFile.getConnectionHandler();

        } 
        
        if (file instanceof DatabaseObjectFile) {
            DatabaseObjectFile objectFile = (DatabaseObjectFile) file;
            return objectFile.getConnectionHandler();
        }

        return FileConnectionMappingManager.getInstance(project).getActiveConnection(file);
    }

    private Color getColor(ConnectionHandler connectionHandler) {
        EnvironmentType environmentType = connectionHandler.getEnvironmentType();
        if (environmentType != null) {
            return environmentType.getColor();
        }
        return DEFAULT_COLOR;
    }
}
