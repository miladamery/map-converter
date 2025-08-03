package com.fastmapconverter.processor;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Holds information about a field that needs to be mapped.
 */
public class FieldInfo {
    private final String fieldName;
    private final String mapKey;
    private final TypeMirror fieldType;
    private final boolean ignored;
    private final VariableElement element;

    public FieldInfo(String fieldName, String mapKey, TypeMirror fieldType, 
                     boolean ignored, VariableElement element) {
        this.fieldName = fieldName;
        this.mapKey = mapKey;
        this.fieldType = fieldType;
        this.ignored = ignored;
        this.element = element;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMapKey() {
        return mapKey;
    }

    public TypeMirror getFieldType() {
        return fieldType;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public VariableElement getElement() {
        return element;
    }
}