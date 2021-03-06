package com.dci.intellij.dbn.editor.code;

import com.dci.intellij.dbn.common.AbstractProjectComponent;
import com.dci.intellij.dbn.common.editor.BasicTextEditor;
import com.dci.intellij.dbn.common.editor.document.OverrideReadonlyFragmentModificationHandler;
import com.dci.intellij.dbn.common.thread.BackgroundTask;
import com.dci.intellij.dbn.common.thread.SimpleLaterInvocator;
import com.dci.intellij.dbn.common.thread.WriteActionRunner;
import com.dci.intellij.dbn.common.util.EditorUtil;
import com.dci.intellij.dbn.common.util.MessageUtil;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.database.DatabaseCompatibilityInterface;
import com.dci.intellij.dbn.database.DatabaseDDLInterface;
import com.dci.intellij.dbn.database.DatabaseFeature;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.execution.compiler.DatabaseCompilerManager;
import com.dci.intellij.dbn.language.common.psi.BasePsiElement;
import com.dci.intellij.dbn.language.editor.DBLanguageFileEditorListener;
import com.dci.intellij.dbn.language.psql.PSQLFile;
import com.dci.intellij.dbn.object.common.DBObjectType;
import com.dci.intellij.dbn.object.common.DBSchemaObject;
import com.dci.intellij.dbn.object.common.property.DBObjectProperty;
import com.dci.intellij.dbn.object.common.status.DBObjectStatus;
import com.dci.intellij.dbn.vfs.DatabaseContentFile;
import com.dci.intellij.dbn.vfs.DatabaseEditableObjectFile;
import com.dci.intellij.dbn.vfs.DatabaseFileSystem;
import com.dci.intellij.dbn.vfs.SourceCodeFile;
import com.intellij.openapi.diff.ActionButtonPresentation;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DiffRequestFactory;
import com.intellij.openapi.diff.MergeRequest;
import com.intellij.openapi.diff.impl.mergeTool.DiffRequestFactoryImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Timestamp;

public class SourceCodeManager extends AbstractProjectComponent implements JDOMExternalizable {

    public static SourceCodeManager getInstance(Project project) {
        return project.getComponent(SourceCodeManager.class);
    }

    private SourceCodeManager(Project project) {
        super(project);
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(OverrideReadonlyFragmentModificationHandler.INSTANCE);
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        editorManager.addFileEditorManagerListener(DatabaseFileSystem.getInstance());
        editorManager.addFileEditorManagerListener(new DBLanguageFileEditorListener());
    }

    /**
     * @deprecated
     */

    public void navigateToSpecification(DBSchemaObject parentObject, DBObjectType objectType, String objectName) {
        DatabaseEditableObjectFile databaseFile = parentObject.getVirtualFile();
        PsiManager psiManager = PsiManager.getInstance(parentObject.getProject());
        PSQLFile file = (PSQLFile) psiManager.findFile(databaseFile.getContentFile(DBContentType.CODE_SPEC));
        if (file != null) {
            BasePsiElement basePsiElement = file.lookupObjectSpecification(objectType, objectName);
            if (basePsiElement != null) {
                BasicTextEditor textEditor = EditorUtil.getFileEditor(databaseFile, file.getVirtualFile());
                EditorUtil.selectEditor(databaseFile, textEditor);
                basePsiElement.navigate(true);
            }
        }
    }

    /**
     * @deprecated
     */
    public void navigateToDeclaration(DBSchemaObject parentObject, DBObjectType objectType, String objectName) {
        DatabaseEditableObjectFile databaseFile = parentObject.getVirtualFile();
        PsiManager psiManager = PsiManager.getInstance(parentObject.getProject());
        PSQLFile file = (PSQLFile) psiManager.findFile(databaseFile.getContentFile(DBContentType.CODE_BODY));
        if (file != null) {
            BasePsiElement basePsiElement = file.lookupObjectDeclaration(objectType, objectName);
            if (basePsiElement != null) {
                BasicTextEditor textEditor = EditorUtil.getFileEditor(databaseFile, file.getVirtualFile());
                EditorUtil.selectEditor(databaseFile, textEditor);
                basePsiElement.navigate(true);
            }
        }
    }

    public void updateSourceToDatabase(final Editor editor, final SourceCodeFile virtualFile) {
        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(virtualFile.getProject());
        final DBSchemaObject object = virtualFile.getObject();
        if (object != null) {
            if (!debuggerManager.checkForbiddenOperation(virtualFile.getActiveConnection())) {
                object.getStatus().set(DBObjectStatus.SAVING, false);
                return;
            }

            final Project project = virtualFile.getProject();
            final DBContentType contentType = virtualFile.getContentType();
            object.getStatus().set(DBObjectStatus.SAVING, true);

            new BackgroundTask(project, "Checking for third party changes on " + object.getQualifiedNameWithType(), true) {
                public void execute(@NotNull ProgressIndicator progressIndicator) {
                    try {
                        String content = editor.getDocument().getText();
                        if (isValidObjectTypeAndName(content, object, contentType)) {
                            Timestamp lastUpdated = object.loadChangeTimestamp(contentType);
                            if (lastUpdated != null && lastUpdated.after(virtualFile.getChangeTimestamp())) {

                                virtualFile.setContent(content);
                                String message =
                                        "The " + object.getQualifiedNameWithType() +
                                                " has been changed by another user. \nYou will be prompted to merge the changes";
                                MessageUtil.showErrorDialog(message, "Version conflict");

                                String databaseContent = loadSourceCodeFromDatabase(object, contentType);
                                showSourceDiffDialog(databaseContent, virtualFile, editor);
                            } else {
                                doUpdateSourceToDatabase(object, virtualFile, editor);
                                //sourceCodeEditor.afterSave();
                            }

                        } else {
                            String message = "You are not allowed to change the name or the type of the object";
                            object.getStatus().set(DBObjectStatus.SAVING, false);
                            MessageUtil.showErrorDialog(message, "Illegal action");
                        }
                    } catch (SQLException ex) {
                        if (!DatabaseCompatibilityInterface.getInstance(object).supportsFeature(DatabaseFeature.OBJECT_REPLACING)) {
                            virtualFile.updateChangeTimestamp();
                        }
                        MessageUtil.showErrorDialog("Could not save changes to database.", ex);
                        object.getStatus().set(DBObjectStatus.SAVING, false);
                    }
                }
            }.start();
        }

    }

    public String loadSourceCodeFromDatabase(DBSchemaObject object, DBContentType contentType) throws SQLException {
        return loadSourceFromDatabase(object, contentType).getSourceCode();
    }

    public SourceCodeContent loadSourceFromDatabase(DBSchemaObject object, DBContentType contentType) throws SQLException {
        String sourceCode = object.loadCodeFromDatabase(contentType);
        SourceCodeContent sourceCodeContent = new SourceCodeContent(sourceCode);
        DatabaseDDLInterface ddlInterface = object.getConnectionHandler().getInterfaceProvider().getDDLInterface();
        ddlInterface.computeSourceCodeOffsets(sourceCodeContent, object.getObjectType().getTypeId(), object.getName());
        return sourceCodeContent;
    }

    private boolean isValidObjectTypeAndName(String text, DBSchemaObject object, DBContentType contentType) {
        DatabaseDDLInterface ddlInterface = object.getConnectionHandler().getInterfaceProvider().getDDLInterface();
        if (ddlInterface.includesTypeAndNameInSourceContent(object.getObjectType().getTypeId())) {
            int typeIndex = StringUtil.indexOfIgnoreCase(text, object.getTypeName(), 0);
            if (typeIndex == -1 || !StringUtil.isEmptyOrSpaces(text.substring(0, typeIndex))) {
                return false;
            }

            int typeEndIndex = typeIndex + object.getTypeName().length();
            if (!Character.isWhitespace(text.charAt(typeEndIndex))) return false;

            if (contentType.getObjectTypeSubname() != null) {
                int subnameIndex = StringUtil.indexOfIgnoreCase(text, contentType.getObjectTypeSubname(), typeEndIndex);
                typeEndIndex = subnameIndex + contentType.getObjectTypeSubname().length();
                if (!Character.isWhitespace(text.charAt(typeEndIndex))) return false;
            }

            ConnectionHandler connectionHandler = object.getConnectionHandler();
            char quotes = DatabaseCompatibilityInterface.getInstance(connectionHandler).getIdentifierQuotes();


            String objectName = object.getName();
            int nameIndex = StringUtil.indexOfIgnoreCase(text, objectName, typeEndIndex);
            if (nameIndex == -1) return false;
            int nameEndIndex = nameIndex + objectName.length();

            if (text.charAt(nameIndex -1) == quotes) {
                if (text.charAt(nameEndIndex) != quotes) return false;
                nameIndex = nameIndex -1;
                nameEndIndex = nameEndIndex + 1;
            }

            String typeNameGap = text.substring(typeEndIndex, nameIndex);
            typeNameGap = StringUtil.replaceIgnoreCase(typeNameGap, object.getSchema().getName(), "").replace(".", " ").replace(quotes, ' ');
            if (!StringUtil.isEmptyOrSpaces(typeNameGap)) return false;
            if (!Character.isWhitespace(text.charAt(nameEndIndex)) && text.charAt(nameEndIndex) != '(') return false;
        }

        return true;
    }

    private void showSourceDiffDialog(final String databaseContent, final SourceCodeFile virtualFile, final Editor editor) {
        new SimpleLaterInvocator() {
            public void execute() {
                DiffRequestFactory diffRequestFactory = new DiffRequestFactoryImpl();
                MergeRequest mergeRequest = diffRequestFactory.createMergeRequest(
                        databaseContent,
                        virtualFile.getContent(),
                        virtualFile.getLastSavedContent(),
                        virtualFile,
                        virtualFile.getProject(),
                        ActionButtonPresentation.APPLY,
                        ActionButtonPresentation.CANCEL_WITH_PROMPT);
                mergeRequest.setVersionTitles(new String[]{"Database version", "Merge result", "Your version"});
                final DBSchemaObject object = virtualFile.getObject();
                mergeRequest.setWindowTitle("Version conflict resolution for " + object.getQualifiedNameWithType());

                DiffManager.getInstance().getDiffTool().show(mergeRequest);

                int result = mergeRequest.getResult();
                if (result == 0) {
                    doUpdateSourceToDatabase(object, virtualFile, editor);
                    //sourceCodeEditor.afterSave();
                } else if (result == 1) {
                    new WriteActionRunner() {
                        public void run() {
                            editor.getDocument().setText(virtualFile.getContent());
                            object.getStatus().set(DBObjectStatus.SAVING, false);
                        }
                    }.start();
                }
            }
        }.start();
    }


    private void doUpdateSourceToDatabase(final DBSchemaObject object, final SourceCodeFile virtualFile, final Editor editor) {
        new BackgroundTask(object.getProject(), "Saving " + object.getQualifiedNameWithType() + " to database", true) {

            @Override
            public void execute(@NotNull ProgressIndicator indicator) {
                try {
                    String content = editor.getDocument().getText();
                    virtualFile.setContent(content);
                    virtualFile.updateToDatabase();

                    object.getConnectionHandler().getObjectBundle().refreshObjectsStatus();
                    if (object.getProperties().is(DBObjectProperty.COMPILABLE)) {
                        DatabaseCompilerManager.getInstance(editor.getProject()).createCompilerResult(object);
                    }
                    object.reload();
                } catch (SQLException e) {
                    MessageUtil.showErrorDialog("Could not save changes to database.", e);
                } finally {
                     object.getStatus().set(DBObjectStatus.SAVING, false);
                }

            }
        }.start();
    }

    public BasePsiElement getObjectNavigationElement(DBSchemaObject parentObject, DBContentType contentType, DBObjectType objectType, CharSequence objectName) {
        DatabaseEditableObjectFile databaseFile = parentObject.getVirtualFile();
        PsiManager psiManager = PsiManager.getInstance(parentObject.getProject());
        DatabaseContentFile contentFile = databaseFile.getContentFile(contentType);
        if (contentFile != null) {
            PSQLFile file = (PSQLFile) psiManager.findFile(contentFile);
            if (file != null) {
                return
                    contentType == DBContentType.CODE_BODY ? file.lookupObjectDeclaration(objectType, objectName) :
                    contentType == DBContentType.CODE_SPEC ? file.lookupObjectSpecification(objectType, objectName) : null;
            }
        }
        return null;
    }

    public void navigateToObject(DBSchemaObject parentObject, BasePsiElement basePsiElement) {
        DatabaseEditableObjectFile databaseFile = parentObject.getVirtualFile();
        FileEditor fileEditor = EditorUtil.getFileEditor(databaseFile, basePsiElement.getFile().getVirtualFile());
        EditorUtil.selectEditor(databaseFile, fileEditor);
        basePsiElement.navigate(true);
    }



    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.Project.CodeEditorManager";
    }

    /****************************************
    *            JDOMExternalizable         *
    *****************************************/
    public void readExternal(Element element) throws InvalidDataException {

    }

    public void writeExternal(Element element) throws WriteExternalException {

    }
}
