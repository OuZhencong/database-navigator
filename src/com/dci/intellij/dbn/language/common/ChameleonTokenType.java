package com.dci.intellij.dbn.language.common;

import com.dci.intellij.dbn.language.common.element.ElementType;
import com.dci.intellij.dbn.language.common.element.ElementTypeBundle;
import com.dci.intellij.dbn.language.common.element.impl.WrappingDefinition;
import com.dci.intellij.dbn.language.common.element.lookup.ElementTypeLookupCache;
import com.dci.intellij.dbn.language.common.element.parser.ElementTypeParser;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttribute;
import com.dci.intellij.dbn.language.common.element.util.ElementTypeAttributesBundle;
import com.dci.intellij.dbn.object.common.DBObjectType;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ChameleonTokenType extends SimpleTokenType implements ElementType {
    private DBLanguageDialect injectedLanguage;
    public ChameleonTokenType(@Nullable DBLanguageDialect hostLanguage, DBLanguageDialect injectedLanguage) {
        super(injectedLanguage.getID() + " block", hostLanguage);
        this.injectedLanguage = injectedLanguage;
    }

    @NotNull
    @Override
    public DBLanguage getLanguage() {
        return getLanguageDialect().getBaseLanguage();
    }

    @Override
    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) super.getLanguage();
    }

    public DBLanguageDialect getInjectedLanguage() {
        return injectedLanguage;
    }

    public String getDebugName() {
        return toString();
    }

    public Icon getIcon() {
        return null;
    }

    public ElementType getParent() {
        return null;
    }

    public ElementTypeLookupCache getLookupCache() {
        return null;
    }

    public ElementTypeParser getParser() {
        return null;
    }

    public boolean is(ElementTypeAttribute attribute) {
        return false;
    }

    public boolean isLeaf() {
        return false;
    }

    public boolean isVirtualObject() {
        return false;
    }

    public boolean isVirtualObjectInsideLookup() {
        return false;
    }

    public DBObjectType getVirtualObjectType() {
        return null;
    }

    public PsiElement createPsiElement(ASTNode astNode) {
        return new ASTWrapperPsiElement(astNode);
    }

    public String getResolveScopeId() {
        return null;
    }

    public ElementTypeBundle getElementBundle() {
        return null;
    }

    public void registerVirtualObject(DBObjectType objectType) {

    }

    @Override
    public ElementTypeAttributesBundle getAttributes() {
        return null;
    }

    @Override
    public WrappingDefinition getWrapping() {
        return null;
    }
}
