package com.dci.intellij.dbn.browser.ui;

import com.dci.intellij.dbn.browser.DatabaseBrowserManager;
import com.dci.intellij.dbn.browser.model.BrowserTreeNode;
import com.dci.intellij.dbn.browser.options.BrowserDisplayMode;
import com.dci.intellij.dbn.browser.options.DatabaseBrowserSettings;
import com.dci.intellij.dbn.common.event.EventManager;
import com.dci.intellij.dbn.common.ui.DBNForm;
import com.dci.intellij.dbn.common.ui.DBNFormImpl;
import com.dci.intellij.dbn.common.ui.GUIUtil;
import com.dci.intellij.dbn.common.util.ActionUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.object.common.DBObject;
import com.dci.intellij.dbn.object.properties.ui.ObjectPropertiesForm;
import com.dci.intellij.dbn.options.ProjectSettingsChangeListener;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.GuiUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class BrowserToolWindowForm extends DBNFormImpl implements DBNForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel browserPanel;
    private JPanel closeActionPanel;
    private JPanel objectPropertiesPanel;
    private DatabaseBrowserForm browserForm;

    private BrowserDisplayMode displayMode;
    private Project project;
    private ObjectPropertiesForm objectPropertiesForm;

    public BrowserToolWindowForm(Project project) {
        this.project = project;
        //toolWindow.setIcon(dbBrowser.getIcon(0));
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);

        displayMode = DatabaseBrowserSettings.getInstance(project).getGeneralSettings().getDisplayMode();
        initBrowserForm();

        ActionToolbar actionToolbar = ActionUtil.createActionToolbar("", true, "DBNavigator.ActionGroup.Browser.Controls");

        actionsPanel.add(actionToolbar.getComponent());

        /*ActionToolbar objectPropertiesActionToolbar = ActionUtil.createActionToolbar("", false, "DBNavigator.ActionGroup.Browser.ObjectProperties");
        closeActionPanel.add(objectPropertiesActionToolbar.getComponent(), BorderLayout.CENTER);*/

        objectPropertiesPanel.setVisible(browserManager.getShowObjectProperties().value());
        objectPropertiesForm = new ObjectPropertiesForm(project);
        objectPropertiesPanel.add(objectPropertiesForm.getComponent());
        GuiUtils.replaceJSplitPaneWithIDEASplitter(mainPanel);
        GUIUtil.updateSplitterProportion(mainPanel, (float) 0.7);


        EventManager.subscribe(project, ProjectSettingsChangeListener.TOPIC, projectSettingsChangeListener);
        Disposer.register(this, objectPropertiesForm);
    }

    private void initBrowserForm() {
        if (browserForm != null) {
            Disposer.dispose(browserForm);
        }
        browserPanel.removeAll();

        browserForm =
                displayMode == BrowserDisplayMode.TABBED ? new TabbedBrowserForm(project) :
                displayMode == BrowserDisplayMode.SIMPLE ? new SimpleBrowserForm(project) : null;


        browserPanel.add(browserForm.getComponent(), BorderLayout.CENTER);
        Disposer.register(this, browserForm);
    }

    public DatabaseBrowserTree getBrowserTree(ConnectionHandler connectionHandler) {
        if (browserForm instanceof TabbedBrowserForm) {
            TabbedBrowserForm tabbedBrowserForm = (TabbedBrowserForm) browserForm;
            return tabbedBrowserForm.getBrowserTree(connectionHandler);
        }

        if (browserForm instanceof SimpleBrowserForm) {
            return browserForm.getBrowserTree();
        }

        return null;
    }



    public void showObjectProperties() {
        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
        BrowserTreeNode treeNode = activeBrowserTree == null ? null : activeBrowserTree.getSelectedNode();
        if (treeNode instanceof DBObject) {
            DBObject object = (DBObject) treeNode;
            objectPropertiesForm.setObject(object);
        }

        objectPropertiesPanel.setVisible(true);
    }

    public void hideObjectProperties() {
        objectPropertiesPanel.setVisible(false);
    }

    private BrowserDisplayMode getConfigDisplayMode() {
        return DatabaseBrowserSettings.getInstance(project).getGeneralSettings().getDisplayMode();
    }

    @Nullable
    public DatabaseBrowserTree getActiveBrowserTree() {
        return browserForm.getBrowserTree();
    }

    public DatabaseBrowserForm getBrowserForm() {
        return browserForm;
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void dispose() {
        EventManager.unsubscribe(projectSettingsChangeListener);
        super.dispose();
        objectPropertiesForm = null;
        browserForm = null;
        project = null;
    }

    /********************************************************
     *                       Listeners                      *
     ********************************************************/
    private ProjectSettingsChangeListener projectSettingsChangeListener = new ProjectSettingsChangeListener() {
        @Override
        public void projectSettingsChanged(Project project) {
            BrowserDisplayMode configDisplayMode = getConfigDisplayMode();
            if (displayMode != configDisplayMode) {
                Disposer.dispose(browserForm);
                displayMode = configDisplayMode;
                initBrowserForm();
                browserPanel.updateUI();
            }
        }
    };


}
