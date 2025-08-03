package com.mapconverter.generator;

import com.mapconverter.converter.DateTimeConverter;
import com.mapconverter.enumeration.DateTimeStrategy;
import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Specialized code generator for temporal (date/time) field handling.
 */
public class TemporalCodeGenerator extends AbstractCodeGenerator {
    
    public TemporalCodeGenerator(Elements elementUtils, Types typeUtils) {
        super(elementUtils, typeUtils);
    }

    /**
     * Generates code for converting temporal fields to map values.
     */
    public void generateTemporalToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName);
        
        CodeBlock converterCall = buildDateTimeConverterToMapCall(field, getterName);
        
        codeBlock.addStatement("Object value = $L", converterCall)
                 .beginControlFlow("if (value != null)")
                 .addStatement("map.put($L, value)", getKeyConstantReference(field.getFieldName()))
                 .endControlFlow()
                 .endControlFlow();
    }

    /**
     * Generates code for converting map values to temporal fields.
     */
    public void generateTemporalFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        TypeName fieldType = getResolvedTypeName(field.getFieldType());
        String valueName = field.getFieldName() + "Value";
        
        codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
                 .beginControlFlow("if ($L != null)", valueName);
        
        CodeBlock converterCall = buildDateTimeConverterFromMapCall(field, valueName, fieldType);
        
        codeBlock.addStatement("$T converted = $L", fieldType, converterCall)
                 .beginControlFlow("if (converted != null)")
                 .addStatement("obj.$L(converted)", setterName)
                 .endControlFlow()
                 .endControlFlow();
    }

    /**
     * Generates temporal to map code for records (uses accessor methods).
     */
    public void generateTemporalToMapCodeForRecord(CodeBlock.Builder codeBlock, FieldInfo field, String accessorName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", accessorName);
        
        CodeBlock converterCall = buildDateTimeConverterToMapCall(field, accessorName);
        
        codeBlock.addStatement("Object value = $L", converterCall)
                 .beginControlFlow("if (value != null)")
                 .addStatement("map.put($L, value)", getKeyConstantReference(field.getFieldName()))
                 .endControlFlow()
                 .endControlFlow();
    }

    /**
     * Generates temporal from map code for record constructor parameters.
     */
    public void generateRecordTemporalFromMapCode(TypeName fieldType, String paramName, FieldInfo field, 
                                                 com.palantir.javapoet.MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("$T $L", fieldType, paramName);
        
        String tempValueName = field.getFieldName() + "TempValue";
        methodBuilder.addStatement("Object $L = map.get($L)", tempValueName, getKeyConstantReference(field.getFieldName()))
                     .beginControlFlow("if ($L != null)", tempValueName);
        
        CodeBlock converterCall = buildDateTimeConverterFromMapCall(field, tempValueName, fieldType);
        
        methodBuilder.addStatement("$L = $L", paramName, converterCall)
                     .nextControlFlow("else")
                     .addStatement("$L = null", paramName)
                     .endControlFlow();
    }

    /**
     * Generates code for converting collections of temporal objects to map values.
     */
    public void generateTemporalCollectionToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        TypeName elementType = TypeName.get(field.getElementType());
        String fieldName = field.getFieldName();
        
        switch (field.getCollectionType()) {
            case LIST:
                generateTemporalCollectionConversion(codeBlock, field, getterName, elementType, 
                    ArrayList.class, fieldName + "List");
                break;
            case SET:
                generateTemporalCollectionConversion(codeBlock, field, getterName, elementType, 
                    HashSet.class, fieldName + "Set");
                break;
            case ARRAY:
                generateTemporalCollectionConversion(codeBlock, field, getterName, elementType, 
                    ArrayList.class, fieldName + "List");
                break;
        }
    }

    /**
     * Generates code for converting map values to collections of temporal objects.
     */
    public void generateTemporalCollectionFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        TypeName elementType = TypeName.get(field.getElementType());
        String fieldName = field.getFieldName();
        
        switch (field.getCollectionType()) {
            case LIST:
                generateTemporalCollectionFromMapConversion(codeBlock, field, setterName, elementType, 
                    ArrayList.class, fieldName + "List");
                break;
            case SET:
                generateTemporalCollectionFromMapConversion(codeBlock, field, setterName, elementType, 
                    HashSet.class, fieldName + "Set");
                break;
            case ARRAY:
                codeBlock.addStatement("$T<$T> tempList = new $T<>()", 
                    ArrayList.class, elementType, ArrayList.class);
                codeBlock.beginControlFlow("for (Object item : list)");
                generateTemporalCollectionItemFromMapCode(codeBlock, field, "item", "tempList");
                codeBlock.endControlFlow();
                codeBlock.addStatement("$T[] $LArray = tempList.toArray(new $T[0])", 
                    elementType, fieldName, elementType);
                codeBlock.addStatement("obj.$L($LArray)", setterName, fieldName);
                break;
        }
    }

    private void generateTemporalCollectionConversion(CodeBlock.Builder codeBlock, FieldInfo field, String getterName,
                                                    TypeName elementType, Class<?> collectionClass, String collectionVar) {
        codeBlock.addStatement("$T<Object> $L = new $T<>()", collectionClass, collectionVar, collectionClass);
        codeBlock.beginControlFlow("for ($T item : obj.$L())", elementType, getterName);
        generateTemporalCollectionItemToMapCode(codeBlock, field, "item", collectionVar);
        codeBlock.endControlFlow();
        codeBlock.addStatement("map.put($L, $L)", getKeyConstantReference(field.getFieldName()), collectionVar);
    }

    private void generateTemporalCollectionFromMapConversion(CodeBlock.Builder codeBlock, FieldInfo field, String setterName,
                                                           TypeName elementType, Class<?> collectionClass, String collectionVar) {
        codeBlock.addStatement("$T<$T> $L = new $T<>()", collectionClass, elementType, collectionVar, collectionClass);
        codeBlock.beginControlFlow("for (Object item : list)");
        generateTemporalCollectionItemFromMapCode(codeBlock, field, "item", collectionVar);
        codeBlock.endControlFlow();
        codeBlock.addStatement("obj.$L($L)", setterName, collectionVar);
    }

    private void generateTemporalCollectionItemToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, 
                                                        String itemVar, String collectionVar) {
        codeBlock.beginControlFlow("if ($L != null)", itemVar);
        
        CodeBlock converterCall = buildDateTimeConverterToMapCallForItem(field, itemVar);
        
        codeBlock.addStatement("Object converted = $L", converterCall)
                 .beginControlFlow("if (converted != null)")
                 .addStatement("$L.add(converted)", collectionVar)
                 .nextControlFlow("else")
                 .addStatement("$L.add(null)", collectionVar)
                 .endControlFlow()
                 .nextControlFlow("else")
                 .addStatement("$L.add(null)", collectionVar)
                 .endControlFlow();
    }

    private void generateTemporalCollectionItemFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, 
                                                          String itemVar, String collectionVar) {
        TypeName elementType = TypeName.get(field.getElementType());
        
        codeBlock.beginControlFlow("if ($L != null)", itemVar);
        
        CodeBlock converterCall = buildDateTimeConverterFromMapCallForItem(field, itemVar, elementType);
        
        codeBlock.addStatement("$T converted = $L", elementType, converterCall)
                 .beginControlFlow("if (converted != null)")
                 .addStatement("$L.add(converted)", collectionVar)
                 .nextControlFlow("else")
                 .addStatement("$L.add(null)", collectionVar)
                 .endControlFlow()
                 .nextControlFlow("else")
                 .addStatement("$L.add(null)", collectionVar)
                 .endControlFlow();
    }

    private CodeBlock buildDateTimeConverterToMapCall(FieldInfo field, String accessorName) {
        return buildConverterCall(DateTimeConverter.class, "toMapValue", 
            "obj." + accessorName + "()", field, null);
    }

    private CodeBlock buildDateTimeConverterFromMapCall(FieldInfo field, String valueName, TypeName fieldType) {
        return buildConverterCall(DateTimeConverter.class, "fromMapValue", 
            valueName, field, fieldType + ".class");
    }

    private CodeBlock buildDateTimeConverterToMapCallForItem(FieldInfo field, String itemVar) {
        return buildConverterCall(DateTimeConverter.class, "toMapValue", itemVar, field, null);
    }

    private CodeBlock buildDateTimeConverterFromMapCallForItem(FieldInfo field, String itemVar, TypeName elementType) {
        return buildConverterCall(DateTimeConverter.class, "fromMapValue", 
            itemVar, field, elementType + ".class");
    }

    private CodeBlock buildConverterCall(Class<?> converterClass, String methodName, String valueParam, 
                                       FieldInfo field, String classParam) {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.add("$T.$L($L", converterClass, methodName, valueParam);
        
        if (classParam != null) {
            builder.add(", $L", classParam);
        }
        
        // Add strategy parameter
        if (field.getDateTimeStrategy() != null) {
            builder.add(", $T.$L", DateTimeStrategy.class, field.getDateTimeStrategy().name());
        } else {
            builder.add(", null");
        }
        
        // Add pattern parameter
        builder.add(", $S", field.getDateTimePattern() != null ? field.getDateTimePattern() : "");
        
        // Add timezone parameter
        builder.add(", $S", field.getDateTimeTimezone() != null ? field.getDateTimeTimezone() : "");
        
        // Add preserveNanos parameter
        builder.add(", $L", field.isPreserveNanos());
        
        // Add lenientParsing parameter
        builder.add(", $L)", field.isLenientParsing());
        
        return builder.build();
    }
}