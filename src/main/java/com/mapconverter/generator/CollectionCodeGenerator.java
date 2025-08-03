package com.mapconverter.generator;

import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Specialized code generator for collection handling.
 */
public class CollectionCodeGenerator extends AbstractCodeGenerator {
    
    private TemporalCodeGenerator temporalGenerator;
    
    public CollectionCodeGenerator(Elements elementUtils, Types typeUtils) {
        super(elementUtils, typeUtils);
    }
    
    public void setTemporalGenerator(TemporalCodeGenerator temporalGenerator) {
        this.temporalGenerator = temporalGenerator;
    }

    /**
     * Generates collection to map conversion code based on collection type.
     */
    public void generateCollectionToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName);
        
        if (field.isNestedCollection() && field.isNestedObjectGenerated()) {
            generateNestedCollectionToMapCode(codeBlock, field, getterName);
        } else if (field.isTemporalCollection()) {
            temporalGenerator.generateTemporalCollectionToMapCode(codeBlock, field, getterName);
        } else {
            generateRegularCollectionToMapCode(codeBlock, field, getterName);
        }
        
        codeBlock.endControlFlow();
    }

    /**
     * Generates collection from map conversion code based on collection type.
     */
    public void generateCollectionFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        String valueName = field.getFieldName() + "Value";
        codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
            .beginControlFlow("if ($L instanceof $T)", valueName, List.class)
            .addStatement("$T<?> list = ($T<?>) $L", List.class, List.class, valueName);
        
        if (field.isNestedCollection() && field.isNestedObjectGenerated()) {
            generateNestedCollectionFromMapCode(codeBlock, field, setterName);
        } else if (field.isTemporalCollection()) {
            temporalGenerator.generateTemporalCollectionFromMapCode(codeBlock, field, setterName);
        } else {
            generateRegularCollectionFromMapCode(codeBlock, field, setterName);
        }
        
        codeBlock.endControlFlow()
            .endControlFlow();
    }

    /**
     * Generates collection to map code with circular reference tracking.
     */
    public void generateCollectionToMapCodeWithTracking(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName);
        
        if (field.isNestedCollection() && field.isNestedObjectGenerated()) {
            generateNestedCollectionToMapCodeWithTracking(codeBlock, field, getterName);
        } else if (field.isTemporalCollection()) {
            temporalGenerator.generateTemporalCollectionToMapCode(codeBlock, field, getterName);
        } else {
            generateRegularCollectionToMapCode(codeBlock, field, getterName);
        }
        
        codeBlock.endControlFlow();
    }

    private void generateNestedCollectionToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        String nestedPackage = getPackageName(field.getNestedElementType());
        String nestedTypeName = getSimpleTypeName(field.getNestedElementType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        
        generateCollectionConversionByType(codeBlock, field, getterName, 
            (cb, fieldName, itemVar) -> cb.addStatement("$LList.add($T.toMap(($T) $L))", 
                fieldName, mapperClass, TypeName.get(field.getNestedElementType()), itemVar));
    }

    private void generateNestedCollectionFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        String nestedPackage = getPackageName(field.getNestedElementType());
        String nestedTypeName = getSimpleTypeName(field.getNestedElementType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        TypeName elementType = TypeName.get(field.getNestedElementType());
        
        generateCollectionFromMapByType(codeBlock, field, setterName, elementType,
            (cb, collectionVar) -> {
                cb.beginControlFlow("if (item instanceof $T)", Map.class)
                  .addStatement("$T<String, Object> itemMap = ($T<String, Object>) item", Map.class, Map.class)
                  .addStatement("$T nestedObj = $T.fromMap(itemMap)", elementType, mapperClass)
                  .addStatement("$L.add(nestedObj)", collectionVar)
                  .endControlFlow();
            });
    }

    private void generateRegularCollectionToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        generateCollectionConversionByType(codeBlock, field, getterName,
            (cb, fieldName, itemVar) -> cb.addStatement("$LList.add($L)", fieldName, itemVar));
    }

    private void generateRegularCollectionFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        TypeName elementType = TypeName.get(field.getElementType());
        
        generateCollectionFromMapByType(codeBlock, field, setterName, elementType,
            (cb, collectionVar) -> cb.addStatement("$L.add(($T) item)", collectionVar, elementType));
    }

    private void generateNestedCollectionToMapCodeWithTracking(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        String nestedPackage = getPackageName(field.getNestedElementType());
        String nestedTypeName = getSimpleTypeName(field.getNestedElementType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        
        generateCollectionConversionByType(codeBlock, field, getterName,
            (cb, fieldName, itemVar) -> {
                cb.addStatement("$T<String, Object> itemMap = $T.toMap(($T) $L)", 
                    Map.class, mapperClass, TypeName.get(field.getNestedElementType()), itemVar)
                  .beginControlFlow("if (itemMap != null)")
                  .addStatement("$LList.add(itemMap)", fieldName)
                  .endControlFlow();
            });
    }

    private void generateCollectionConversionByType(CodeBlock.Builder codeBlock, FieldInfo field, String getterName,
                                                   CollectionItemConverter converter) {
        String fieldName = field.getFieldName();
        
        switch (field.getCollectionType()) {
            case LIST:
                codeBlock.addStatement("$T<Object> $LList = new $T<>()", 
                    ArrayList.class, fieldName, ArrayList.class);
                codeBlock.beginControlFlow("for (Object item : obj.$L())", getterName);
                converter.convert(codeBlock, fieldName, "item");
                codeBlock.endControlFlow();
                codeBlock.addStatement("map.put($L, $LList)", getKeyConstantReference(fieldName), fieldName);
                break;
                
            case SET:
                codeBlock.addStatement("$T<Object> $LSet = new $T<>()", 
                    HashSet.class, fieldName, HashSet.class);
                codeBlock.beginControlFlow("for (Object item : obj.$L())", getterName);
                converter.convert(codeBlock, fieldName, "item");
                codeBlock.endControlFlow();
                codeBlock.addStatement("map.put($L, $LSet)", getKeyConstantReference(fieldName), fieldName);
                break;
                
            case ARRAY:
                codeBlock.addStatement("$T<Object> $LList = new $T<>()", 
                    ArrayList.class, fieldName, ArrayList.class);
                codeBlock.beginControlFlow("for (Object item : obj.$L())", getterName);
                converter.convert(codeBlock, fieldName, "item");
                codeBlock.endControlFlow();
                codeBlock.addStatement("map.put($L, $LList)", getKeyConstantReference(fieldName), fieldName);
                break;
        }
    }

    private void generateCollectionFromMapByType(CodeBlock.Builder codeBlock, FieldInfo field, String setterName,
                                               TypeName elementType, CollectionFromMapConverter converter) {
        String fieldName = field.getFieldName();
        
        switch (field.getCollectionType()) {
            case LIST:
                codeBlock.addStatement("$T<$T> $LList = new $T<>()", 
                    ArrayList.class, elementType, fieldName, ArrayList.class);
                codeBlock.beginControlFlow("for (Object item : list)");
                converter.convert(codeBlock, fieldName + "List");
                codeBlock.endControlFlow();
                codeBlock.addStatement("obj.$L($LList)", setterName, fieldName);
                break;
                
            case SET:
                codeBlock.addStatement("$T<$T> $LSet = new $T<>()", 
                    HashSet.class, elementType, fieldName, HashSet.class);
                codeBlock.beginControlFlow("for (Object item : list)");
                converter.convert(codeBlock, fieldName + "Set");
                codeBlock.endControlFlow();
                codeBlock.addStatement("obj.$L($LSet)", setterName, fieldName);
                break;
                
            case ARRAY:
                codeBlock.addStatement("$T<$T> tempList = new $T<>()", 
                    ArrayList.class, elementType, ArrayList.class);
                codeBlock.beginControlFlow("for (Object item : list)");
                converter.convert(codeBlock, "tempList");
                codeBlock.endControlFlow();
                codeBlock.addStatement("$T[] $LArray = tempList.toArray(new $T[0])", 
                    elementType, fieldName, elementType);
                codeBlock.addStatement("obj.$L($LArray)", setterName, fieldName);
                break;
        }
    }

    @FunctionalInterface
    private interface CollectionItemConverter {
        void convert(CodeBlock.Builder codeBlock, String fieldName, String itemVar);
    }

    @FunctionalInterface
    private interface CollectionFromMapConverter {
        void convert(CodeBlock.Builder codeBlock, String collectionVar);
    }
}