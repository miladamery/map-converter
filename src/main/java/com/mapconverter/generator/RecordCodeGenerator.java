package com.mapconverter.generator;

import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized code generator for Java Record handling.
 */
public class RecordCodeGenerator extends AbstractCodeGenerator {
    
    private final TemporalCodeGenerator temporalGenerator;
    private final NestedObjectCodeGenerator nestedObjectGenerator;
    private final CollectionCodeGenerator collectionGenerator;
    
    public RecordCodeGenerator(Elements elementUtils, Types typeUtils,
                              TemporalCodeGenerator temporalGenerator,
                              NestedObjectCodeGenerator nestedObjectGenerator,
                              CollectionCodeGenerator collectionGenerator) {
        super(elementUtils, typeUtils);
        this.temporalGenerator = temporalGenerator;
        this.nestedObjectGenerator = nestedObjectGenerator;
        this.collectionGenerator = collectionGenerator;
    }

    /**
     * Generates the toMap method for Records (uses accessor methods instead of getters).
     */
    public MethodSpec generateToMapMethodForRecord(ClassName recordClass, List<FieldInfo> fields, 
                                                  boolean needsCircularRefCheck) {
        TypeName mapType = createMapType();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toMap")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(mapType)
            .addParameter(recordClass, "obj")
            .addJavadoc("Converts a $T record to a Map&lt;String, Object&gt;\n", recordClass)
            .addJavadoc("@param obj the record to convert\n")
            .addJavadoc("@return the resulting map\n");
        
        if (needsCircularRefCheck) {
            methodBuilder.addStatement("return toMap(obj, getVisitedSet())");
        } else {
            methodBuilder.addStatement("return toMapDirect(obj)");
        }
        
        return methodBuilder.build();
    }

    /**
     * Generates fast toMapDirect method for records without circular reference checking.
     */
    public MethodSpec generateToMapDirectMethodForRecord(ClassName recordClass, List<FieldInfo> fields) {
        TypeName mapType = createMapType();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toMapDirect")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(mapType)
            .addParameter(recordClass, "obj")
            .addJavadoc("Fast direct record conversion without circular reference checking\n")
            .addJavadoc("@param obj the record to convert\n")
            .addJavadoc("@return the resulting map\n");
        
        addNullCheck(methodBuilder, "obj");
        addMapCreation(methodBuilder, fields);
        addFieldsToMap(methodBuilder, fields, false);
        
        methodBuilder.addStatement("return map");
        return methodBuilder.build();
    }

    /**
     * Generates the toMap method with tracking for Records.
     */
    public MethodSpec generateToMapWithTrackingMethodForRecord(ClassName recordClass, List<FieldInfo> fields) {
        TypeName mapType = createMapType();
        TypeName setType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(Object.class));
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toMap")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(mapType)
            .addParameter(recordClass, "obj")
            .addParameter(setType, "visited")
            .addJavadoc("Converts a $T record to a Map&lt;String, Object&gt; with circular reference tracking\n", recordClass)
            .addJavadoc("@param obj the record to convert\n")
            .addJavadoc("@param visited set of already visited objects to prevent circular references\n")
            .addJavadoc("@return the resulting map\n");
        
        addNullCheck(methodBuilder, "obj");
        addCircularReferenceCheck(methodBuilder);
        addToVisitedSet(methodBuilder);
        addMapCreation(methodBuilder, fields);
        addFieldsToMap(methodBuilder, fields, true);
        addRemoveFromVisitedSet(methodBuilder);
        
        methodBuilder.addStatement("return map");
        return methodBuilder.build();
    }

    /**
     * Generates the fromMap method for Records (uses canonical constructor).
     */
    public MethodSpec generateFromMapMethodForRecord(ClassName recordClass, List<FieldInfo> fields) {
        TypeName mapType = createMapType();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("fromMap")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(recordClass)
            .addParameter(mapType, "map")
            .addJavadoc("Creates a $T record from a Map&lt;String, Object&gt;\n", recordClass)
            .addJavadoc("@param map the map to convert\n")
            .addJavadoc("@return the resulting record\n");
        
        methodBuilder.addStatement("if (map == null) return null");
        
        List<String> paramNames = prepareConstructorParameters(methodBuilder, fields);
        createRecordInstance(methodBuilder, recordClass, paramNames);
        
        return methodBuilder.build();
    }

    private TypeName createMapType() {
        return ParameterizedTypeName.get(
            ClassName.get(Map.class),
            ClassName.get(String.class),
            ClassName.get(Object.class)
        );
    }

    private void addNullCheck(MethodSpec.Builder methodBuilder, String variableName) {
        methodBuilder.addStatement("if ($L == null) return null", variableName);
    }

    private void addCircularReferenceCheck(MethodSpec.Builder methodBuilder) {
        methodBuilder.beginControlFlow("if (visited.contains(obj))")
                     .addStatement("return null")
                     .endControlFlow();
    }

    private void addToVisitedSet(MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("visited.add(obj)");
    }

    private void addRemoveFromVisitedSet(MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("visited.remove(obj)");
    }

    private void addMapCreation(MethodSpec.Builder methodBuilder, List<FieldInfo> fields) {
        int nonIgnoredFieldCount = (int) fields.stream().filter(field -> !field.isIgnored()).count();
        int initialCapacity = calculateOptimalCapacity(nonIgnoredFieldCount);
        TypeName mapType = createMapType();
        
        methodBuilder.addStatement("$T map = new $T<>($L)", mapType, HashMap.class, initialCapacity);
    }

    private void addFieldsToMap(MethodSpec.Builder methodBuilder, List<FieldInfo> fields, boolean withTracking) {
        for (FieldInfo field : fields) {
            if (field.isIgnored()) {
                continue;
            }
            
            String accessorName = field.getFieldName(); // Records use componentName() not getComponentName()
            CodeBlock.Builder codeBlock = CodeBlock.builder();
            
            addFieldToMapCode(codeBlock, field, accessorName, withTracking);
            methodBuilder.addCode(codeBlock.build());
        }
    }

    private void addFieldToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String accessorName, boolean withTracking) {
        if (field.isCollection()) {
            if (withTracking) {
                collectionGenerator.generateCollectionToMapCodeWithTracking(codeBlock, field, accessorName);
            } else {
                collectionGenerator.generateCollectionToMapCode(codeBlock, field, accessorName);
            }
        } else if (field.isNestedObject()) {
            if (withTracking) {
                nestedObjectGenerator.generateNestedObjectToMapCodeWithTracking(codeBlock, field, accessorName);
            } else {
                nestedObjectGenerator.generateNestedObjectToMapCode(codeBlock, field, accessorName);
            }
        } else if (field.isTemporal()) {
            temporalGenerator.generateTemporalToMapCodeForRecord(codeBlock, field, accessorName);
        } else if (field.isEnum()) {
            // Enum to map: convert enum to string using name()
            codeBlock.beginControlFlow("if (obj.$L() != null)", accessorName)
                     .addStatement("map.put($L, obj.$L().name())", getKeyConstantReference(field.getFieldName()), accessorName)
                     .endControlFlow();
        } else if (isPrimitive(field.getFieldType())) {
            codeBlock.addStatement("map.put($L, obj.$L())", getKeyConstantReference(field.getFieldName()), accessorName);
        } else {
            codeBlock.beginControlFlow("if (obj.$L() != null)", accessorName)
                     .addStatement("map.put($L, obj.$L())", getKeyConstantReference(field.getFieldName()), accessorName)
                     .endControlFlow();
        }
    }

    private List<String> prepareConstructorParameters(MethodSpec.Builder methodBuilder, List<FieldInfo> fields) {
        List<String> paramNames = new ArrayList<>();
        
        for (FieldInfo field : fields) {
            String paramName = field.getFieldName() + "Value";
            
            if (field.isIgnored()) {
                handleIgnoredField(methodBuilder, field, paramName);
            } else {
                handleRegularField(methodBuilder, field, paramName);
            }
            
            paramNames.add(paramName);
        }
        
        return paramNames;
    }

    private void handleIgnoredField(MethodSpec.Builder methodBuilder, FieldInfo field, String paramName) {
        TypeName fieldType = getResolvedTypeName(field.getFieldType());
        String defaultValue = getDefaultValue(field.getFieldType());
        methodBuilder.addStatement("$T $L = $L", fieldType, paramName, defaultValue);
    }

    private void handleRegularField(MethodSpec.Builder methodBuilder, FieldInfo field, String paramName) {
        TypeName fieldType = getResolvedTypeName(field.getFieldType());
        
        if (field.isCollection()) {
            generateRecordCollectionFromMapCode(fieldType, paramName, field, methodBuilder);
        } else if (field.isNestedObject()) {
            nestedObjectGenerator.generateRecordNestedObjectFromMapCode(fieldType, paramName, field, methodBuilder);
        } else if (field.isTemporal()) {
            temporalGenerator.generateRecordTemporalFromMapCode(fieldType, paramName, field, methodBuilder);
        } else if (field.isEnum()) {
            generateEnumFieldFromMapCode(fieldType, paramName, field, methodBuilder);
        } else {
            generateSimpleFieldFromMapCode(fieldType, paramName, field, methodBuilder);
        }
    }

    private void generateEnumFieldFromMapCode(TypeName fieldType, String paramName, FieldInfo field, 
                                            MethodSpec.Builder methodBuilder) {
        String valueName = paramName + "Value";
        methodBuilder.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()));
        methodBuilder.addStatement("$T $L = $L != null ? $T.valueOf($L.toString()) : null", 
            fieldType, paramName, valueName, fieldType, valueName);
    }

    private void generateSimpleFieldFromMapCode(TypeName fieldType, String paramName, FieldInfo field, 
                                              MethodSpec.Builder methodBuilder) {
        methodBuilder.addStatement("Object $LValue = map.get($L)", paramName, getKeyConstantReference(field.getFieldName()));
        methodBuilder.addStatement("$T $L = $LValue != null ? ($T) $LValue : $L", 
            fieldType, paramName, paramName, fieldType, paramName, getDefaultValue(field.getFieldType()));
    }

    private void generateRecordCollectionFromMapCode(TypeName fieldType, String paramName, FieldInfo field,
                                                   MethodSpec.Builder methodBuilder) {
        String tempVarName = field.getFieldName() + "TempList";
        TypeName elementType = TypeName.get(field.getElementType());
        
        methodBuilder.addStatement("$T $L", fieldType, paramName);
        methodBuilder.addStatement("Object $LRaw = map.get($L)", tempVarName, getKeyConstantReference(field.getFieldName()));
        methodBuilder.beginControlFlow("if ($LRaw instanceof $T)", tempVarName, List.class);
        methodBuilder.addStatement("$T<?> $L = ($T<?>) $LRaw", List.class, tempVarName, List.class);
        
        if (field.isNestedCollection() && field.isNestedObjectGenerated()) {
            generateNestedCollectionFromMapForRecord(methodBuilder, field, paramName, tempVarName, elementType);
        } else {
            generateRegularCollectionFromMapForRecord(methodBuilder, field, paramName, tempVarName, elementType);
        }
        
        methodBuilder.nextControlFlow("else");
        methodBuilder.addStatement("$L = $L", paramName, getDefaultValue(field.getFieldType()));
        methodBuilder.endControlFlow();
    }

    private void generateNestedCollectionFromMapForRecord(MethodSpec.Builder methodBuilder, FieldInfo field, 
                                                        String paramName, String tempVarName, TypeName elementType) {
        String nestedPackage = getPackageName(field.getNestedElementType());
        String nestedTypeName = getSimpleTypeName(field.getNestedElementType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        
        switch (field.getCollectionType()) {
            case LIST:
                methodBuilder.addStatement("$T<$T> $LResult = new $T<>()", 
                    ArrayList.class, elementType, field.getFieldName(), ArrayList.class);
                break;
            case SET:
                methodBuilder.addStatement("$T<$T> $LResult = new $T<>()", 
                    HashSet.class, elementType, field.getFieldName(), HashSet.class);
                break;
            case ARRAY:
                methodBuilder.addStatement("$T<$T> $LResult = new $T<>()", 
                    ArrayList.class, elementType, field.getFieldName(), ArrayList.class);
                break;
        }
        
        methodBuilder.beginControlFlow("for (Object item : $L)", tempVarName)
                     .beginControlFlow("if (item instanceof $T)", Map.class)
                     .addStatement("$T<String, Object> itemMap = ($T<String, Object>) item", Map.class, Map.class)
                     .addStatement("$T nestedObj = $T.fromMap(itemMap)", elementType, mapperClass)
                     .addStatement("$LResult.add(nestedObj)", field.getFieldName())
                     .endControlFlow()
                     .endControlFlow();
                     
        if (field.getCollectionType() == FieldInfo.CollectionType.ARRAY) {
            methodBuilder.addStatement("$L = $LResult.toArray(new $T[0])", 
                paramName, field.getFieldName(), elementType);
        } else {
            methodBuilder.addStatement("$L = $LResult", paramName, field.getFieldName());
        }
    }

    private void generateRegularCollectionFromMapForRecord(MethodSpec.Builder methodBuilder, FieldInfo field, 
                                                         String paramName, String tempVarName, TypeName elementType) {
        switch (field.getCollectionType()) {
            case LIST:
                methodBuilder.addStatement("$T<$T> $LResult = new $T<>()", 
                    ArrayList.class, elementType, field.getFieldName(), ArrayList.class);
                methodBuilder.beginControlFlow("for (Object item : $L)", tempVarName)
                             .addStatement("$LResult.add(($T) item)", field.getFieldName(), elementType)
                             .endControlFlow();
                methodBuilder.addStatement("$L = $LResult", paramName, field.getFieldName());
                break;
            case SET:
                methodBuilder.addStatement("$T<$T> $LResult = new $T<>()", 
                    HashSet.class, elementType, field.getFieldName(), HashSet.class);
                methodBuilder.beginControlFlow("for (Object item : $L)", tempVarName)
                             .addStatement("$LResult.add(($T) item)", field.getFieldName(), elementType)
                             .endControlFlow();
                methodBuilder.addStatement("$L = $LResult", paramName, field.getFieldName());
                break;
            case ARRAY:
                methodBuilder.addStatement("$T[] $LArray = new $T[$L.size()]", 
                    elementType, field.getFieldName(), tempVarName);
                methodBuilder.beginControlFlow("for (int i = 0; i < $L.size(); i++)", tempVarName)
                             .addStatement("$LArray[i] = ($T) $L.get(i)", field.getFieldName(), elementType, tempVarName)
                             .endControlFlow();
                methodBuilder.addStatement("$L = $LArray", paramName, field.getFieldName());
                break;
        }
    }

    private void createRecordInstance(MethodSpec.Builder methodBuilder, ClassName recordClass, List<String> paramNames) {
        String constructorCall = String.join(", ", paramNames);
        methodBuilder.addStatement("return new $T($L)", recordClass, constructorCall);
    }
}