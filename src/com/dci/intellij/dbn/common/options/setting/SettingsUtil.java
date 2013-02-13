package com.dci.intellij.dbn.common.options.setting;

import com.dci.intellij.dbn.DatabaseNavigator;
import com.dci.intellij.dbn.common.util.StringUtil;
import org.jdom.Element;

import java.awt.Color;

public class SettingsUtil {
    public static boolean isDebugEnabled() {
        return DatabaseNavigator.getInstance().isDebugModeEnabled();
    }

    public static int getInteger(Element parent, String childName, int originalValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? originalValue : Integer.parseInt(stringValue);
    }

    public static String getString(Element parent, String childName, String originalValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? originalValue : stringValue;
    }

    public static boolean getBoolean(Element parent, String childName, boolean originalValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? originalValue : Boolean.parseBoolean(stringValue);
    }

    public static <T extends Enum> T getEnum(Element parent, String childName, T originalValue) {
        Element element = parent.getChild(childName);
        String stringValue = getStringValue(element);
        return stringValue == null ? originalValue : (T) T.valueOf(originalValue.getClass(), stringValue);
    }

    private static String getStringValue(Element element) {
        if (element != null) {
            String value = element.getAttributeValue("value");
            if (StringUtil.isNotEmptyOrSpaces(value)) {
                return value;
            }
        }
        return null;
    }


    public static void setInteger(Element parent, String childName, int value) {
        Element element = new Element(childName);
        element.setAttribute("value", Integer.toString(value));
        parent.addContent(element);
    }

    public static void setString(Element parent, String childName, String value) {
        Element element = new Element(childName);
        element.setAttribute("value", value == null ? "" : value);
        parent.addContent(element);
    }

    public static void setBoolean(Element parent, String childName, boolean value) {
        Element element = new Element(childName);
        element.setAttribute("value", Boolean.toString(value));
        parent.addContent(element);
    }

    public static <T extends Enum> void setEnum(Element parent, String childName, T value) {
        Element element = new Element(childName);
        element.setAttribute("value",value.name());
        parent.addContent(element);
    }

    public static boolean getBooleanAttribute(Element element, String attributeName, boolean defaultValue) {
        String attributeValue = element.getAttributeValue(attributeName);
        return StringUtil.isEmptyOrSpaces(attributeValue) ? defaultValue : Boolean.parseBoolean(attributeValue);
    }

    public static void setBooleanAttribute(Element element, String attributeName, boolean value) {
        element.setAttribute(attributeName, Boolean.toString(value));
    }

    public static int getIntegerAttribute(Element element, String attributeName, int defaultValue) {
        String attributeValue = element.getAttributeValue(attributeName);
        if (attributeValue == null || attributeValue.trim().length() == 0) {
            return defaultValue;
        }
        return Integer.parseInt(attributeValue);
    }

    public static void setIntegerAttribute(Element element, String attributeName, int value) {
        element.setAttribute(attributeName, Integer.toString(value));
    }

    public static Color getColorAttribute(Element element, String attributeName, Color defaultValue) {
        int rgb = getIntegerAttribute(element, attributeName, 0);
        return rgb == 0 ? defaultValue : new Color(rgb);
    }

    public static void setColorAttribute(Element element, String attributeName, Color value) {
        setIntegerAttribute(element, attributeName, value.getRGB());
    }
    

    public static <T extends Enum<T>> T getEnumAttribute(Element element, String attributeName, T defaultValue) {
        String attributeValue = element.getAttributeValue(attributeName);
        Class<T> enumClass = (Class<T>) defaultValue.getClass();
        return StringUtil.isEmpty(attributeValue) ? defaultValue : T.valueOf(enumClass, attributeValue);
    }

    public static <T extends Enum<T>> T getEnumAttribute(Element element, String attributeName, Class<T> enumClass) {
        String attributeValue = element.getAttributeValue(attributeName);
        return StringUtil.isEmpty(attributeValue) ? null : T.valueOf(enumClass, attributeValue);
    }

    public static <T extends Enum<T>> void setEnumAttribute(Element element, String attributeName, T value) {
        element.setAttribute(attributeName, value.name());
    }
}