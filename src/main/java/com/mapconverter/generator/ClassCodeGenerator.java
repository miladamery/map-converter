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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized code generator for regular Java Class handling.
 */
public class ClassCodeGenerator extends AbstractCodeGenerator {
    
    private final TemporalCodeGenerator temporalGenerator;
    private final NestedObjectCodeGenerator nestedObjectGenerator;
    private final CollectionCodeGenerator collectionGenerator;
    
    public ClassCodeGenerator(Elements elementUtils, Types typeUtils,
                             TemporalCodeGenerator temporalGenerator,
                             NestedObjectCodeGenerator nestedObjectGenerator,
                             CollectionCodeGenerator collectionGenerator) {
        super(elementUtils, typeUtils);
        this.temporalGenerator = temporalGenerator;
        this.nestedObjectGenerator = nestedObjectGenerator;
        this.collectionGenerator = collectionGenerator;
    }

    /**
     * Generates the toMap method for classes.
     */
    public MethodSpec generateToMapMethod(ClassName originalClass, List<FieldInfo> fields, boolean needsCircularRefCheck) {
        TypeName mapType = createMapType();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toMap")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(mapType)
            .addParameter(originalClass, "obj")
            .addJavadoc("Converts a $T instance to a Map&lt;String, Object&gt;\n", originalClass)
            .addJavadoc("@param obj the object to convert\n")
            .addJavadoc("@return the resulting map\n");
        
        if (needsCircularRefCheck) {
            methodBuilder.addStatement("return toMap(obj, getVisitedSet())");
        } else {
            methodBuilder.addStatement("return toMapDirect(obj)");
        }
        
        return methodBuilder.build();
    }

    /**
     * Generates fast toMapDirect method for simple objects without circular reference checking.
     */
    public MethodSpec generateToMapDirectMethod(ClassName originalClass, List<FieldInfo> fields) {
        TypeName mapType = createMapType();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toMapDirect")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(mapType)
            .addParameter(originalClass, "obj")
            .addJavadoc("Fast direct conversion without circular reference checking\n")
            .addJavadoc("@param obj the object to convert\n")
            .addJavadoc("@return the resulting map\n");
        
        addNullCheck(methodBuilder, "obj");
        addMapCreation(methodBuilder, fields);
        addFieldsToMap(methodBuilder, fields, false);
        
        methodBuilder.addStatement("return map");
        return methodBuilder.build();
    }

    /**
     * Generates toMap method with circular reference tracking for classes.
     */
    public MethodSpec generateToMapWithTrackingMethod(ClassName originalClass, List<FieldInfo> fields) {
        TypeName mapType = createMapType();
        TypeName setType = ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(Object.class));
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toMap")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(mapType)
            .addParameter(originalClass, "obj")
            .addParameter(setType, "visited")
            .addJavadoc("Converts a $T instance to a Map&lt;String, Object&gt; with circular reference tracking\n", originalClass)
            .addJavadoc("@param obj the object to convert\n")
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
     * Generates fromMap method for classes.
     */
    public MethodSpec generateFromMapMethod(ClassName originalClass, List<FieldInfo> fields) {
        TypeName mapType = createMapType();
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("fromMap")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(originalClass)
            .addParameter(mapType, "map")
            .addJavadoc("Creates a $T instance from a Map&lt;String, Object&gt;\n", originalClass)
            .addJavadoc("@param map the map to convert\n")
            .addJavadoc("@return the resulting object\n");
        
        addNullCheck(methodBuilder, "map");
        addInstanceCreation(methodBuilder, originalClass);
        addSetFieldsFromMap(methodBuilder, fields);
        
        methodBuilder.addStatement("return obj");
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

    private void addInstanceCreation(MethodSpec.Builder methodBuilder, ClassName originalClass) {
        methodBuilder.addStatement("$T obj = new $T()", originalClass, originalClass);
    }

    private void addFieldsToMap(MethodSpec.Builder methodBuilder, List<FieldInfo> fields, boolean withTracking) {
        for (FieldInfo field : fields) {
            if (field.isIgnored()) {
                continue;
            }
            
            String getterName = generateGetterName(field.getFieldName(), field.getFieldType());
            CodeBlock.Builder codeBlock = CodeBlock.builder();
            
            addFieldToMapCode(codeBlock, field, getterName, withTracking);
            methodBuilder.addCode(codeBlock.build());
        }
    }

    private void addSetFieldsFromMap(MethodSpec.Builder methodBuilder, List<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            if (field.isIgnored()) {
                continue;
            }
            
            String setterName = generateSetterName(field.getFieldName());
            CodeBlock.Builder codeBlock = CodeBlock.builder();
            
            addFieldFromMapCode(codeBlock, field, setterName);
            methodBuilder.addCode(codeBlock.build());
        }
    }

    private void addFieldToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName, boolean withTracking) {
        if (field.isCollection()) {
            if (withTracking) {
                collectionGenerator.generateCollectionToMapCodeWithTracking(codeBlock, field, getterName);
            } else {
                collectionGenerator.generateCollectionToMapCode(codeBlock, field, getterName);
            }
        } else if (field.isNestedObject()) {
            if (withTracking) {
                nestedObjectGenerator.generateNestedObjectToMapCodeWithTracking(codeBlock, field, getterName);
            } else {
                nestedObjectGenerator.generateNestedObjectToMapCode(codeBlock, field, getterName);
            }
        } else if (field.isTemporal()) {
            temporalGenerator.generateTemporalToMapCode(codeBlock, field, getterName);
        } else if (field.isEnum()) {
            // Enum to map: convert enum to string using name()
            codeBlock.beginControlFlow("if (obj.$L() != null)", getterName)
                     .addStatement("map.put($L, obj.$L().name())", getKeyConstantReference(field.getFieldName()), getterName)
                     .endControlFlow();
        } else if (isPrimitive(field.getFieldType())) {
            codeBlock.addStatement("map.put($L, obj.$L())", getKeyConstantReference(field.getFieldName()), getterName);
        } else {
            codeBlock.beginControlFlow("if (obj.$L() != null)", getterName)
                     .addStatement("map.put($L, obj.$L())", getKeyConstantReference(field.getFieldName()), getterName)
                     .endControlFlow();
        }
    }

    private void addFieldFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        if (field.isCollection()) {
            collectionGenerator.generateCollectionFromMapCode(codeBlock, field, setterName);
        } else if (field.isNestedObject()) {
            nestedObjectGenerator.generateNestedObjectFromMapCode(codeBlock, field, setterName);
        } else if (field.isTemporal()) {
            temporalGenerator.generateTemporalFromMapCode(codeBlock, field, setterName);
        } else if (field.isEnum()) {
            // Enum from map: convert string to enum using valueOf()
            TypeName fieldType = getResolvedTypeName(field.getFieldType());
            String valueName = field.getFieldName() + "Value";
            
            codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
                     .beginControlFlow("if ($L != null)", valueName)
                     .addStatement("obj.$L($T.valueOf($L.toString()))", setterName, fieldType, valueName)
                     .endControlFlow();
        } else {
            TypeName fieldType = getResolvedTypeName(field.getFieldType());
            String valueName = field.getFieldName() + "Value";
            
            codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
                     .beginControlFlow("if ($L != null)", valueName)
                     .addStatement("obj.$L(($T) $L)", setterName, fieldType, valueName)
                     .endControlFlow();
        }
    }
}