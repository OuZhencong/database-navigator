package com.dci.intellij.dbn.ddl;

import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.language.common.DBLanguageFileType;
import com.dci.intellij.dbn.common.util.StringUtil;

import java.util.List;
import java.util.ArrayList;

public class DDLFileType {
    private DBLanguageFileType languageFileType;
    private String id;
    private String description;
    private DBContentType contentType;
    private List<String> extensions = new ArrayList<String>();

    public DDLFileType(String id, String description, String extension, DBLanguageFileType languageFileType, DBContentType contentType) {
        this.id = id;
        this.description = description;
        this.extensions.add(extension);
        this.languageFileType = languageFileType;
        this.contentType = contentType;
    }

    public DBLanguageFileType getLanguageFileType() {
        return languageFileType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public DBContentType getContentType() {
        return contentType;
    }

    public void setContentType(DBContentType contentType) {
        this.contentType = contentType;
    }

    public String getExtensionsAsString() {
        return StringUtil.concatenate(extensions, ", ");
    }

    public void setExtensionsAsString(String extensions) {
        setExtensions(StringUtil.tokenize(extensions, ","));
    }

}
