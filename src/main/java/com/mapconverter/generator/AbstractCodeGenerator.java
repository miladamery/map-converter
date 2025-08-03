package com.mapconverter.generator;

import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * Base class for all code generators providing common functionality.
 */
public abstract class AbstractCodeGenerator {
    
    protected final Elements elementUtils;
    protected final Types typeUtils;
    
    protected AbstractCodeGenerator(Elements elementUtils, Types typeUtils) {
        this.elementUtils = elementUtils;
        this.typeUtils = typeUtils;
    }
    
    /**
     * Generates getter method name for a field.
     */
    protected String generateGetterName(String fieldName, TypeMirror fieldType) {
        String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        
        // Use 'is' prefix for boolean fields
        if (fieldType.getKind() == TypeKind.BOOLEAN || 
            (fieldType.getKind() == TypeKind.DECLARED && fieldType.toString().equals("java.lang.Boolean"))) {
            return "is" + capitalizedFieldName;
        }
        
        return "get" + capitalizedFieldName;
    }

    /**
     * Generates setter method name for a field.
     */
    protected String generateSetterName(String fieldName) {
        String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return "set" + capitalizedFieldName;
    }

    /**
     * Checks if a type is primitive.
     */
    protected boolean isPrimitive(TypeMirror type) {
        return type.getKind().isPrimitive();
    }

    /**
     * Gets the primitive cast type for proper casting in generated code.
     */
    protected String getPrimitiveCastType(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "Boolean";
            case BYTE:
                return "Byte";
            case SHORT:
                return "Short";
            case INT:
                return "Integer";
            case LONG:
                return "Long";
            case CHAR:
                return "Character";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            default:
                return type.toString();
        }
    }

    /**
     * Resolves TypeName for field types, handling generic type parameters by converting them to Object.
     */
    protected TypeName getResolvedTypeName(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR) {
            // Type parameter (T, U, etc.) - use Object instead
            return TypeName.get(Object.class);
        }
        return TypeName.get(type);
    }

    /**
     * Gets the package name of a type.
     */
    protected String getPackageName(TypeMirror type) {
        if (type.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
            javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) type;
            javax.lang.model.element.TypeElement typeElement = (javax.lang.model.element.TypeElement) declaredType.asElement();
            return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        }
        return "";
    }

    /**
     * Extracts simple type name from TypeMirror.
     */
    protected String getSimpleTypeName(TypeMirror type) {
        String fullName = type.toString();
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    /**
     * Generates a constant name for a field's map key.
     */
    protected String generateKeyConstantName(String fieldName) {
        String snakeCase = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        return snakeCase + "_KEY";
    }

    /**
     * Gets the constant reference for a field's map key.
     */
    protected String getKeyConstantReference(String fieldName) {
        return generateKeyConstantName(fieldName);
    }

    /**
     * Gets default value for a type (used for record constructor parameters).
     */
    protected String getDefaultValue(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "false";
            case BYTE:
            case SHORT:
            case INT:
                return "0";
            case LONG:
                return "0L";
            case CHAR:
                return "'\\0'";
            case FLOAT:
                return "0.0f";
            case DOUBLE:
                return "0.0";
            default:
                return "null";
        }
    }

    /**
     * Calculates optimal HashMap initial capacity based on expected size.
     */
    protected int calculateOptimalCapacity(int expectedSize) {
        if (expectedSize == 0) {
            return 16; // Default HashMap capacity
        }
        // Calculate capacity needed to avoid rehashing: expectedSize / loadFactor + 1
        // HashMap default load factor is 0.75
        return (int) Math.ceil(expectedSize / 0.75) + 1;
    }

    /**
     * Adds null check code block.
     */
    protected void addNullCheck(CodeBlock.Builder codeBlock, String objectName, String getterName) {
        codeBlock.beginControlFlow("if ($L.$L() != null)", objectName, getterName);
    }

    /**
     * Adds null check code block and closes it.
     */
    protected void addNullCheckWithEnd(CodeBlock.Builder codeBlock, String objectName, String getterName) {
        codeBlock.beginControlFlow("if ($L.$L() != null)", objectName, getterName);
        // Content should be added by caller
        codeBlock.endControlFlow();
    }

    /**
     * Determines if circular reference checking is needed based on field types.
     */
    protected boolean needsCircularReferenceCheck(List<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            if (field.isIgnored()) {
                continue;
            }
            
            // Nested objects can cause circular references
            if (field.isNestedObject()) {
                return true;
            }
            
            // Collections of nested objects can cause circular references
            if (field.isNestedCollection()) {
                return true;
            }
        }
        return false;
    }
}