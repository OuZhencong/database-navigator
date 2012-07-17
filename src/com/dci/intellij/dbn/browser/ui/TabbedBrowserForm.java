package com.dci.intellij.dbn.browser.ui;

import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.common.environment.EnvironmentChangeListener;
import com.dci.intellij.dbn.common.environment.EnvironmentType;
import com.dci.intellij.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.ui.tab.TabbedPane;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class TabbedBrowserForm extends DatabaseBrowserForm{
    private TabbedPane connectionTabs;
    private JPanel mainPanel;

    public TabbedBrowserForm(Project project) {
        super(project);
        connectionTabs = new TabbedPane(project);
        //connectionTabs.setBackground(UIUtil.getListBackground());
        mainPanel.add(connectionTabs, BorderLayout.CENTER);
        initTabs();
        connectionTabs.addListener(new TabsListener() {
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
/*
                ToolWindow toolWindow = browserManager.getBrowserToolWindow();
                if (toolWindow.isVisible()) {
                    toolWindow.activate(null);
                }
*/
            }

            public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
            }

            public void tabsMoved() {

            }
        });

        EventManager.subscribe(project, EnvironmentChangeListener.TOPIC, environmentChangeListener);
    }


    private void initTabs() {
        Project project = getProject();
        connectionTabs.removeAllTabs();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        for (ConnectionBundle connectionBundle : connectionManager.getConnectionBundles()) {
            for (ConnectionHandler connectionHandler: connectionBundle.getConnectionHandlers()) {
                SimpleBrowserForm browserForm = new SimpleBrowserForm(connectionHandler);
                JComponent component = browserForm.getComponent();
                TabInfo tabInfo = new TabInfo(component);
                tabInfo.setText(connectionHandler.getName());
                tabInfo.setObject(browserForm);
                //tabInfo.setIcon(connectionHandler.getIcon());
                connectionTabs.addTab(tabInfo);

                EnvironmentType environmentType = connectionHandler.getEnvironmentType();
                tabInfo.setTabColor(environmentType.getColor());
            }
        }
    }

    @Nullable
    private SimpleBrowserForm getBrowserForm(ConnectionHandler connectionHandler) {
        for (TabInfo tabInfo : connectionTabs.getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            if (browserForm.getConnectionHandler() == connectionHandler) {
                return browserForm;
            }
        }
        return null;
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public DatabaseBrowserTree getBrowserTree() {
        return getActiveBrowserTree();
    }

    @Nullable
    public DatabaseBrowserTree getBrowserTree(ConnectionHandler connectionHandler) {
        SimpleBrowserForm browserForm = getBrowserForm(connectionHandler);
        return browserForm== null ? null : browserForm.getBrowserTree();
    }

    @Nullable
    public DatabaseBrowserTree getActiveBrowserTree() {
        TabInfo tabInfo = connectionTabs.getSelectedInfo();
        if (tabInfo != null) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            return browserForm.getBrowserTree();
        }
        return null;
    }

    public void selectElement(BrowserTreeNode treeNode, boolean requestFocus) {
        ConnectionHandler connectionHandler = treeNode.getConnectionHandler();
        SimpleBrowserForm browserForm = getBrowserForm(connectionHandler);
        if (browserForm != null) {
            connectionTabs.select(browserForm.getComponent(), requestFocus);
            browserForm.selectElement(treeNode, requestFocus);
        }
    }

    public void updateTree() {
        for (TabInfo tabInfo : connectionTabs.getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            browserForm.updateTree();
        }
    }

    public void rebuild() {
        initTabs();
    }

    public void dispose() {
        Project project = getProject();
        EventManager.unsubscribe(project, environmentChangeListener);
        for (TabInfo tabInfo : connectionTabs.getTabs()) {
            SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
            browserForm.dispose();
        }
        connectionTabs.dispose();
        super.dispose();
    }


    /********************************************************
     *                       Listeners                      *
     ********************************************************/
    private EnvironmentChangeListener environmentChangeListener = new EnvironmentChangeListener() {
        @Override
        public void environmentTypeChanged(EnvironmentType environmentType) {
            Project project = getProject();
            for (TabInfo tabInfo : connectionTabs.getTabs()) {
                SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
                if (browserForm.getConnectionHandler().getEnvironmentType().equals(environmentType)) {
                    EnvironmentVisibilitySettings visibilitySettings = getEnvironmentSettings(project).getVisibilitySettings();
                    if (visibilitySettings.getConnectionTabs().value()) {
                        tabInfo.setTabColor(environmentType.getColor());
                    } else {
                        tabInfo.setTabColor(null);
                    }
                }
            }
        }

        @Override
        public void environmentVisibilitySettingsChanged() {
            Project project = getProject();
            for (TabInfo tabInfo : connectionTabs.getTabs()) {
                SimpleBrowserForm browserForm = (SimpleBrowserForm) tabInfo.getObject();
                EnvironmentType environmentType = browserForm.getConnectionHandler().getEnvironmentType();
                EnvironmentVisibilitySettings visibilitySettings = getEnvironmentSettings(project).getVisibilitySettings();
                if (visibilitySettings.getConnectionTabs().value()) {
                    tabInfo.setTabColor(environmentType.getColor());
                } else {
                    tabInfo.setTabColor(null);
                }
            }
        }
    };
}

