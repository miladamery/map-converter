package com.mapconverter.generator;

import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Map;

/**
 * Specialized code generator for nested object handling.
 */
public class NestedObjectCodeGenerator extends AbstractCodeGenerator {
    
    public NestedObjectCodeGenerator(Elements elementUtils, Types typeUtils) {
        super(elementUtils, typeUtils);
    }

    /**
     * Generates code for converting nested objects to map.
     */
    public void generateNestedObjectToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        if (field.isNestedObjectGenerated()) {
            ClassName mapperClass = createMapperClassName(field.getNestedObjectType());
            
            codeBlock.beginControlFlow("if (obj.$L() != null)", getterName)
                     .addStatement("map.put($L, $T.toMap(obj.$L()))", 
                         getKeyConstantReference(field.getFieldName()), mapperClass, getterName)
                     .endControlFlow();
        } else {
            generateRegularObjectToMapCode(codeBlock, field, getterName);
        }
    }

    /**
     * Generates code for converting map to nested objects.
     */
    public void generateNestedObjectFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        if (field.isNestedObjectGenerated()) {
            ClassName mapperClass = createMapperClassName(field.getNestedObjectType());
            TypeName nestedObjectType = TypeName.get(field.getNestedObjectType());
            
            String valueName = field.getFieldName() + "Value";
            codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
                     .beginControlFlow("if ($L instanceof $T)", valueName, Map.class)
                     .addStatement("$T<String, Object> nestedMap = ($T<String, Object>) $L", 
                         Map.class, Map.class, valueName)
                     .addStatement("$T nestedObj = $T.fromMap(nestedMap)", 
                         nestedObjectType, mapperClass)
                     .addStatement("obj.$L(nestedObj)", setterName)
                     .endControlFlow();
        } else {
            generateRegularObjectFromMapCode(codeBlock, field, setterName);
        }
    }

    /**
     * Generates code for converting nested objects to map with circular reference tracking.
     */
    public void generateNestedObjectToMapCodeWithTracking(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        if (field.isNestedObjectGenerated()) {
            ClassName mapperClass = createMapperClassName(field.getNestedObjectType());
            
            codeBlock.beginControlFlow("if (obj.$L() != null)", getterName)
                     .addStatement("$T<String, Object> nestedMap = $T.toMap(obj.$L())", 
                         Map.class, mapperClass, getterName)
                     .beginControlFlow("if (nestedMap != null)")
                     .addStatement("map.put($L, nestedMap)", getKeyConstantReference(field.getFieldName()))
                     .endControlFlow()
                     .endControlFlow();
        } else {
            generateRegularObjectToMapCode(codeBlock, field, getterName);
        }
    }

    /**
     * Generates nested object from map code for record constructor parameters.
     */
    public void generateRecordNestedObjectFromMapCode(TypeName fieldType, String paramName, FieldInfo field, 
                                                     com.palantir.javapoet.MethodSpec.Builder methodBuilder) {
        if (field.isNestedObjectGenerated()) {
            ClassName mapperClass = createMapperClassName(field.getNestedObjectType());
            TypeName nestedObjectType = TypeName.get(field.getNestedObjectType());

            methodBuilder.addStatement("$T $L", nestedObjectType, paramName);
            
            String nestedValueName = field.getFieldName() + "NestedValue";
            methodBuilder.addStatement("Object $L = map.get($L)", nestedValueName, 
                         getKeyConstantReference(field.getFieldName()))
                         .beginControlFlow("if ($L instanceof $T)", nestedValueName, Map.class)
                         .addStatement("$T<String, Object> nestedMap = ($T<String, Object>) $L", 
                             Map.class, Map.class, nestedValueName)
                         .addStatement("$L = $T.fromMap(nestedMap)", paramName, mapperClass)
                         .nextControlFlow("else")
                         .addStatement("$L = null", paramName)
                         .endControlFlow();
        } else {
            generateRegularObjectFromMapCodeForRecord(fieldType, paramName, field, methodBuilder);
        }
    }

    private void generateRegularObjectToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName)
                 .addStatement("map.put($L, obj.$L())", getKeyConstantReference(field.getFieldName()), getterName)
                 .endControlFlow();
    }

    private void generateRegularObjectFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        TypeName fieldType = getResolvedTypeName(field.getFieldType());
        String valueName = field.getFieldName() + "Value";
        
        codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
                 .beginControlFlow("if ($L != null)", valueName)
                 .addStatement("obj.$L(($T) $L)", setterName, fieldType, valueName)
                 .endControlFlow();
    }

    private void generateRegularObjectFromMapCodeForRecord(TypeName fieldType, String paramName, FieldInfo field, 
                                                          com.palantir.javapoet.MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$T $L = map.containsKey($L) ? ($T) map.get($L) : null", 
            fieldType, paramName, getKeyConstantReference(field.getFieldName()), fieldType, 
            getKeyConstantReference(field.getFieldName()));
    }

    private ClassName createMapperClassName(javax.lang.model.type.TypeMirror nestedObjectType) {
        String nestedPackage = getPackageName(nestedObjectType);
        String nestedTypeName = getSimpleTypeName(nestedObjectType);
        String mapperClassName = nestedTypeName + "FastMapper_";
        return ClassName.get(nestedPackage, mapperClassName);
    }
}