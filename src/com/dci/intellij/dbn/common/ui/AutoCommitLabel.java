package com.dci.intellij.dbn.common.ui;

import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.thread.ConditionalLaterInvocator;
import com.dci.intellij.dbn.common.util.UIUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionSettingsChangeListener;
import com.dci.intellij.dbn.connection.VirtualConnectionHandler;
import com.intellij.openapi.project.Project;
import org.picocontainer.Disposable;

import javax.swing.*;
import java.awt.*;

public class AutoCommitLabel extends JLabel implements ConnectionSettingsChangeListener, Disposable {
    private Project project;
    private ConnectionHandler connectionHandler;
    private boolean subscribed = false;

    public AutoCommitLabel() {
        super("");
        setFont(UIUtil.BOLD_FONT);
    }

    public void setConnectionHandler(ConnectionHandler connectionHandler) {
        if (connectionHandler == null || connectionHandler instanceof VirtualConnectionHandler) {
            this.connectionHandler = null;
        } else {
            this.connectionHandler = connectionHandler;
            if (!subscribed) {
                subscribed = true;
                project = connectionHandler.getProject();
                EventManager.subscribe(project, ConnectionSettingsChangeListener.TOPIC, this);
            }
        }
        update();
    }

    private void update() {
        new ConditionalLaterInvocator() {
            @Override
            public void run() {
                if (connectionHandler != null) {
                    setVisible(true);
                    boolean disconnected = !connectionHandler.isConnected();
                    boolean autoCommit = connectionHandler.isAutoCommit();
                    setText(disconnected ? "[disconnected]" : autoCommit ? "Auto-Commit ON" : "Auto-Commit OFF");
                    setForeground(disconnected ? Color.BLACK : autoCommit ? new Color(210, 0, 0) : new Color(0, 160, 0));
                    setToolTipText(
                            disconnected ? "The connection to database has been closed. No editing possible" :
                                    autoCommit ?
                                            "Auto-Commit is enabled for connection \"" + connectionHandler + "\". Data changes will be automatically committed to the database." :
                                            "Auto-Commit is disabled for connection \"" + connectionHandler + "\". Data changes will need to be manually committed to the database.");
                } else {
                    setVisible(false);
                }
            }
        }.start();
    }

    @Override
    public void connectionSettingsChanged(String connectionId) {
        if (connectionHandler != null && connectionId.equals(connectionHandler.getId())) {
            update();
        }
    }

    @Override
    public void dispose() {
        if (project != null) {
            EventManager.unsubscribe(project, this);
        }
    }


}
