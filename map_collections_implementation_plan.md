# Map Collections Support - Implementation Plan

## Overview

This document outlines the comprehensive implementation plan for adding `Map<K,V>` field type support to FastMapConverter. This feature will enable automatic conversion of Map fields in Java objects to/from `Map<String, Object>` representations.

## Phase 1: Type System Extensions

### 1.1 Extend FieldInfo.CollectionType Enum
**File:** `src/main/java/com/mapconverter/processor/FieldInfo.java:69-71`

```java
public enum CollectionType {
    NONE, 
    LIST, 
    SET, 
    ARRAY,
    MAP,           // New: Map<K,V>
    CONCURRENT_MAP // New: ConcurrentHashMap, etc.
}
```

### 1.2 Extend FieldInfo.FieldType Enum  
**File:** `src/main/java/com/mapconverter/processor/FieldInfo.java:58-67`

```java
public enum FieldType {
    PRIMITIVE,           
    COLLECTION,          
    NESTED_OBJECT,       
    NESTED_COLLECTION,   
    TEMPORAL,            
    TEMPORAL_COLLECTION, 
    ENUM,
    MAP,                 // New: Map<K,V> with primitive values
    NESTED_MAP,          // New: Map<K, CustomObject>
    TEMPORAL_MAP,        // New: Map<K, LocalDate>
    UNKNOWN              
}
```

### 1.3 Add Map Support Fields to FieldInfo
**File:** `src/main/java/com/mapconverter/processor/FieldInfo.java`

```java
public class FieldInfo {
    // Existing fields...
    
    // New fields for Map support
    private final TypeMirror mapKeyType;     // K in Map<K,V>
    private final TypeMirror mapValueType;   // V in Map<K,V>
    private final boolean isNestedMapValue;  // true if V is a custom object
    
    // Updated constructor and getters
    public FieldInfo(String fieldName, String mapKey, TypeMirror fieldType, 
                     boolean ignored, Element element, boolean isRecordComponent, 
                     CollectionType collectionType, TypeMirror elementType, 
                     FieldType fieldType2, TypeMirror nestedObjectType,
                     TypeMirror nestedElementType, boolean isNestedObjectGenerated,
                     DateTimeStrategy dateTimeStrategy, String dateTimePattern, 
                     String dateTimeTimezone, boolean preserveNanos, boolean lenientParsing,
                     // New Map parameters
                     TypeMirror mapKeyType, TypeMirror mapValueType, boolean isNestedMapValue) {
        // Constructor implementation...
    }
    
    // New getter methods
    public TypeMirror getMapKeyType() { return mapKeyType; }
    public TypeMirror getMapValueType() { return mapValueType; }
    public boolean isNestedMapValue() { return isNestedMapValue; }
    public boolean isMap() { return fieldType2 == FieldType.MAP; }
    public boolean isNestedMap() { return fieldType2 == FieldType.NESTED_MAP; }
    public boolean isTemporalMap() { return fieldType2 == FieldType.TEMPORAL_MAP; }
}
```

## Phase 2: Type Analysis Enhancement

### 2.1 Extend MapperProcessor Collection Analysis
**File:** `src/main/java/com/mapconverter/processor/MapperProcessor.java:277-302`

```java
private FieldInfo.CollectionType analyzeCollectionType(TypeMirror type) {
    if (type.getKind() == TypeKind.ARRAY) {
        return FieldInfo.CollectionType.ARRAY;
    }
    
    if (type.getKind() == TypeKind.DECLARED) {
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();
        
        // New Map detection
        if (qualifiedName.equals("java.util.Map") || 
            qualifiedName.equals("java.util.HashMap") ||
            qualifiedName.equals("java.util.LinkedHashMap") ||
            qualifiedName.equals("java.util.TreeMap")) {
            return FieldInfo.CollectionType.MAP;
        }
        
        if (qualifiedName.equals("java.util.concurrent.ConcurrentHashMap") ||
            qualifiedName.equals("java.util.concurrent.ConcurrentMap")) {
            return FieldInfo.CollectionType.CONCURRENT_MAP;
        }
        
        // Existing List/Set logic...
        if (qualifiedName.equals("java.util.List") || 
            qualifiedName.equals("java.util.ArrayList") ||
            qualifiedName.equals("java.util.LinkedList")) {
            return FieldInfo.CollectionType.LIST;
        }
        
        if (qualifiedName.equals("java.util.Set") ||
            qualifiedName.equals("java.util.HashSet") ||
            qualifiedName.equals("java.util.TreeSet") ||
            qualifiedName.equals("java.util.LinkedHashSet")) {
            return FieldInfo.CollectionType.SET;
        }
    }
    
    return FieldInfo.CollectionType.NONE;
}
```

### 2.2 Add Map Type Extraction
**File:** `src/main/java/com/mapconverter/processor/MapperProcessor.java`

```java
private MapTypeInfo extractMapTypeInfo(TypeMirror type, FieldInfo.CollectionType collectionType) {
    if (collectionType == FieldInfo.CollectionType.MAP || 
        collectionType == FieldInfo.CollectionType.CONCURRENT_MAP) {
        
        DeclaredType declaredType = (DeclaredType) type;
        List<? extends TypeMirror> typeArgs = declaredType.getTypeArguments();
        
        if (typeArgs.size() >= 2) {
            TypeMirror keyType = typeArgs.get(0);    // K
            TypeMirror valueType = typeArgs.get(1);  // V
            
            return new MapTypeInfo(keyType, valueType);
        }
        
        // Default to <String, Object> if no generics
        return new MapTypeInfo(
            elementUtils.getTypeElement("java.lang.String").asType(),
            elementUtils.getTypeElement("java.lang.Object").asType()
        );
    }
    
    return null;
}

/**
 * Helper class to hold Map key/value type information.
 */
public static class MapTypeInfo {
    public final TypeMirror keyType;
    public final TypeMirror valueType;
    
    public MapTypeInfo(TypeMirror keyType, TypeMirror valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }
}

/**
 * Enhanced field type analysis to include Map types.
 */
private FieldInfo.FieldType analyzeFieldType(TypeMirror type) {
    if (isPrimitive(type) || isStandardJavaType(type)) {
        return FieldInfo.FieldType.PRIMITIVE;
    }
    
    if (isEnumType(type)) {
        return FieldInfo.FieldType.ENUM;
    }
    
    if (isTemporalType(type)) {
        return FieldInfo.FieldType.TEMPORAL;
    }
    
    FieldInfo.CollectionType collectionType = analyzeCollectionType(type);
    
    // New Map type analysis
    if (collectionType == FieldInfo.CollectionType.MAP || 
        collectionType == FieldInfo.CollectionType.CONCURRENT_MAP) {
        
        MapTypeInfo mapInfo = extractMapTypeInfo(type, collectionType);
        if (mapInfo != null) {
            TypeMirror valueType = mapInfo.valueType;
            
            if (isTemporalType(valueType)) {
                return FieldInfo.FieldType.TEMPORAL_MAP;
            } else if (isCustomObject(valueType)) {
                return FieldInfo.FieldType.NESTED_MAP;
            } else {
                return FieldInfo.FieldType.MAP;
            }
        }
        return FieldInfo.FieldType.MAP;
    }
    
    // Existing collection logic...
    if (isCollectionType(type)) {
        TypeMirror elementType = extractElementTypeForAnalysis(type);
        if (elementType != null && isTemporalType(elementType)) {
            return FieldInfo.FieldType.TEMPORAL_COLLECTION;
        } else if (elementType != null && isCustomObject(elementType)) {
            return FieldInfo.FieldType.NESTED_COLLECTION;
        }
        return FieldInfo.FieldType.COLLECTION;
    }
    
    if (isCustomObject(type)) {
        return FieldInfo.FieldType.NESTED_OBJECT;
    }
    
    return FieldInfo.FieldType.UNKNOWN;
}
```

## Phase 3: Serialization Strategy Design

### 3.1 Map Serialization Strategies

**Strategy 1: Direct Embedding (for Map<String, Primitive>)**
```java
// Input: Map<String, Integer> scores = {"math": 95, "english": 87}
// Output: {"scores": {"math": 95, "english": 87}}

// Generated code:
if (obj.getScores() != null) {
    map.put("scores", new HashMap<>(obj.getScores()));
}
```

**Strategy 2: Nested Object Conversion (for Map<String, CustomObject>)**
```java
// Input: Map<String, Address> addresses = {"home": homeAddr, "work": workAddr}  
// Output: {"addresses": {"home": {...}, "work": {...}}}

// Generated code:
if (obj.getAddresses() != null) {
    Map<String, Object> addressesMap = new HashMap<>();
    for (Map.Entry<String, Address> entry : obj.getAddresses().entrySet()) {
        if (entry.getValue() != null) {
            addressesMap.put(entry.getKey(), AddressFastMapper_.toMap(entry.getValue()));
        }
    }
    map.put("addresses", addressesMap);
}
```

**Strategy 3: Key Serialization (for Map<CustomKey, V>)**
```java
// Input: Map<UserId, String> userNames = {userId1: "John", userId2: "Jane"}
// Output: {"userNames": {"user_123": "John", "user_456": "Jane"}}

// Generated code with toString() key conversion:
if (obj.getUserNames() != null) {
    Map<String, Object> userNamesMap = new HashMap<>();
    for (Map.Entry<UserId, String> entry : obj.getUserNames().entrySet()) {
        String keyString = entry.getKey() != null ? entry.getKey().toString() : "null";
        userNamesMap.put(keyString, entry.getValue());
    }
    map.put("userNames", userNamesMap);
}
```

### 3.2 Deserialization Strategies

**Strategy 1: Direct Map Assignment**
```java
// For Map<String, Primitive>
Object scoresValue = map.get("scores");
if (scoresValue instanceof Map) {
    Map<?, ?> scoresMap = (Map<?, ?>) scoresValue;
    Map<String, Integer> scores = new HashMap<>();
    for (Map.Entry<?, ?> entry : scoresMap.entrySet()) {
        if (entry.getKey() instanceof String && entry.getValue() instanceof Integer) {
            scores.put((String) entry.getKey(), (Integer) entry.getValue());
        }
    }
    obj.setScores(scores);
}
```

**Strategy 2: Nested Object Reconstruction**
```java
// For Map<String, CustomObject>
Object addressesValue = map.get("addresses");
if (addressesValue instanceof Map) {
    Map<?, ?> addressesMap = (Map<?, ?>) addressesValue;
    Map<String, Address> addresses = new HashMap<>();
    for (Map.Entry<?, ?> entry : addressesMap.entrySet()) {
        if (entry.getKey() instanceof String && entry.getValue() instanceof Map) {
            String key = (String) entry.getKey();
            Address address = AddressFastMapper_.fromMap((Map<String, Object>) entry.getValue());
            addresses.put(key, address);
        }
    }
    obj.setAddresses(addresses);
}
```

## Phase 4: Map Code Generator

### 4.1 Create MapCodeGenerator Class
**File:** `src/main/java/com/mapconverter/generator/MapCodeGenerator.java`

```java
package com.mapconverter.generator;

import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Specialized code generator for Map<K,V> handling.
 */
public class MapCodeGenerator extends AbstractCodeGenerator {
    
    private TemporalCodeGenerator temporalGenerator;
    private NestedObjectCodeGenerator nestedObjectGenerator;
    
    public MapCodeGenerator(Elements elementUtils, Types typeUtils) {
        super(elementUtils, typeUtils);
    }
    
    public void setTemporalGenerator(TemporalCodeGenerator temporalGenerator) {
        this.temporalGenerator = temporalGenerator;
    }
    
    public void setNestedObjectGenerator(NestedObjectCodeGenerator nestedObjectGenerator) {
        this.nestedObjectGenerator = nestedObjectGenerator;
    }
    
    /**
     * Generates Map to Map conversion code based on Map type.
     */
    public void generateMapToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName);
        
        switch (field.getFieldType2()) {
            case MAP:
                generatePrimitiveMapToMapCode(codeBlock, field, getterName);
                break;
            case NESTED_MAP:
                generateNestedMapToMapCode(codeBlock, field, getterName);
                break;
            case TEMPORAL_MAP:
                generateTemporalMapToMapCode(codeBlock, field, getterName);
                break;
            default:
                // Fallback to primitive map
                generatePrimitiveMapToMapCode(codeBlock, field, getterName);
                break;
        }
        
        codeBlock.endControlFlow();
    }
    
    /**
     * Generates Map from Map conversion code.
     */
    public void generateMapFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        String valueName = field.getFieldName() + "Value";
        codeBlock.addStatement("Object $L = map.get($L)", valueName, getKeyConstantReference(field.getFieldName()))
            .beginControlFlow("if ($L instanceof $T)", valueName, Map.class)
            .addStatement("$T<?, ?> $LMap = ($T<?, ?>) $L", Map.class, field.getFieldName(), Map.class, valueName);
        
        switch (field.getFieldType2()) {
            case MAP:
                generatePrimitiveMapFromMapCode(codeBlock, field, setterName);
                break;
            case NESTED_MAP:
                generateNestedMapFromMapCode(codeBlock, field, setterName);
                break;
            case TEMPORAL_MAP:
                generateTemporalMapFromMapCode(codeBlock, field, setterName);
                break;
            default:
                generatePrimitiveMapFromMapCode(codeBlock, field, setterName);
                break;
        }
        
        codeBlock.endControlFlow();
    }
    
    private void generatePrimitiveMapToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        String mapFieldName = field.getFieldName() + "Map";
        
        if (isStringKey(field.getMapKeyType())) {
            // Direct embedding for Map<String, Primitive>
            codeBlock.addStatement("map.put($L, new $T<>(obj.$L()))", 
                getKeyConstantReference(field.getFieldName()), HashMap.class, getterName);
        } else {
            // Key conversion for Map<NonString, Primitive>
            codeBlock.addStatement("$T<$T, $T> $L = new $T<>()", 
                    Map.class, String.class, Object.class, mapFieldName, HashMap.class)
                .beginControlFlow("for ($T.Entry<$T, $T> entry : obj.$L().entrySet())", 
                    Map.class, 
                    TypeName.get(field.getMapKeyType()), 
                    TypeName.get(field.getMapValueType()), 
                    getterName)
                .addStatement("String keyString = entry.getKey() != null ? entry.getKey().toString() : \"null\"")
                .addStatement("$L.put(keyString, entry.getValue())", mapFieldName)
                .endControlFlow()
                .addStatement("map.put($L, $L)", getKeyConstantReference(field.getFieldName()), mapFieldName);
        }
    }
    
    private void generateNestedMapToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        String mapFieldName = field.getFieldName() + "Map";
        String nestedPackage = getPackageName(field.getMapValueType());
        String nestedTypeName = getSimpleTypeName(field.getMapValueType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        
        codeBlock.addStatement("$T<$T, $T> $L = new $T<>()", 
                Map.class, String.class, Object.class, mapFieldName, HashMap.class)
            .beginControlFlow("for ($T.Entry<$T, $T> entry : obj.$L().entrySet())", 
                Map.class, 
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()), 
                getterName)
            .beginControlFlow("if (entry.getValue() != null)")
            .addStatement("String keyString = entry.getKey() != null ? entry.getKey().toString() : \"null\"")
            .addStatement("$L.put(keyString, $T.toMap(entry.getValue()))", mapFieldName, mapperClass)
            .endControlFlow()
            .endControlFlow()
            .addStatement("map.put($L, $L)", getKeyConstantReference(field.getFieldName()), mapFieldName);
    }
    
    private void generateTemporalMapToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        String mapFieldName = field.getFieldName() + "Map";
        
        codeBlock.addStatement("$T<$T, $T> $L = new $T<>()", 
                Map.class, String.class, Object.class, mapFieldName, HashMap.class)
            .beginControlFlow("for ($T.Entry<$T, $T> entry : obj.$L().entrySet())", 
                Map.class, 
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()), 
                getterName)
            .addStatement("String keyString = entry.getKey() != null ? entry.getKey().toString() : \"null\"");
        
        // Use temporal generator for value conversion
        temporalGenerator.generateSingleTemporalToMapCode(codeBlock, field, "entry.getValue()", mapFieldName + ".put(keyString, ", ")");
        
        codeBlock.endControlFlow()
            .addStatement("map.put($L, $L)", getKeyConstantReference(field.getFieldName()), mapFieldName);
    }
    
    private void generatePrimitiveMapFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        String mapFieldName = field.getFieldName() + "Result";
        
        codeBlock.addStatement("$T<$T, $T> $L = new $T<>()", 
                Map.class, 
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()), 
                mapFieldName, 
                HashMap.class)
            .beginControlFlow("for ($T.Entry<?, ?> entry : $LMap.entrySet())", 
                Map.class, field.getFieldName())
            .beginControlFlow("if (entry.getKey() instanceof $T && entry.getValue() instanceof $T)", 
                TypeName.get(field.getMapKeyType()).box(), 
                TypeName.get(field.getMapValueType()).box())
            .addStatement("$L.put(($T) entry.getKey(), ($T) entry.getValue())", 
                mapFieldName,
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()))
            .endControlFlow()
            .endControlFlow()
            .addStatement("obj.$L($L)", setterName, mapFieldName);
    }
    
    private void generateNestedMapFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        String mapFieldName = field.getFieldName() + "Result";
        String nestedPackage = getPackageName(field.getMapValueType());
        String nestedTypeName = getSimpleTypeName(field.getMapValueType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        
        codeBlock.addStatement("$T<$T, $T> $L = new $T<>()", 
                Map.class, 
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()), 
                mapFieldName, 
                HashMap.class)
            .beginControlFlow("for ($T.Entry<?, ?> entry : $LMap.entrySet())", 
                Map.class, field.getFieldName())
            .beginControlFlow("if (entry.getKey() instanceof $T && entry.getValue() instanceof $T)", 
                TypeName.get(field.getMapKeyType()).box(), Map.class)
            .addStatement("$T key = ($T) entry.getKey()", 
                TypeName.get(field.getMapKeyType()), TypeName.get(field.getMapKeyType()))
            .addStatement("$T value = $T.fromMap(($T<$T, $T>) entry.getValue())", 
                TypeName.get(field.getMapValueType()), mapperClass, Map.class, String.class, Object.class)
            .addStatement("$L.put(key, value)", mapFieldName)
            .endControlFlow()
            .endControlFlow()
            .addStatement("obj.$L($L)", setterName, mapFieldName);
    }
    
    private void generateTemporalMapFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        String mapFieldName = field.getFieldName() + "Result";
        
        codeBlock.addStatement("$T<$T, $T> $L = new $T<>()", 
                Map.class, 
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()), 
                mapFieldName, 
                HashMap.class)
            .beginControlFlow("for ($T.Entry<?, ?> entry : $LMap.entrySet())", 
                Map.class, field.getFieldName())
            .beginControlFlow("if (entry.getKey() instanceof $T)", 
                TypeName.get(field.getMapKeyType()).box())
            .addStatement("$T key = ($T) entry.getKey()", 
                TypeName.get(field.getMapKeyType()), TypeName.get(field.getMapKeyType()));
        
        // Use temporal generator for value conversion
        temporalGenerator.generateSingleTemporalFromMapCode(codeBlock, field, "entry.getValue()", 
            mapFieldName + ".put(key, ", ")");
        
        codeBlock.endControlFlow()
            .endControlFlow()
            .addStatement("obj.$L($L)", setterName, mapFieldName);
    }
    
    /**
     * Generates Map to Map code with circular reference tracking.
     */
    public void generateMapToMapCodeWithTracking(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        if (field.getFieldType2() == FieldInfo.FieldType.NESTED_MAP) {
            generateNestedMapToMapCodeWithTracking(codeBlock, field, getterName);
        } else {
            // Non-nested maps don't need tracking
            generateMapToMapCode(codeBlock, field, getterName);
        }
    }
    
    private void generateNestedMapToMapCodeWithTracking(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        String mapFieldName = field.getFieldName() + "Map";
        String nestedPackage = getPackageName(field.getMapValueType());
        String nestedTypeName = getSimpleTypeName(field.getMapValueType());
        String mapperClassName = nestedTypeName + "FastMapper_";
        ClassName mapperClass = ClassName.get(nestedPackage, mapperClassName);
        
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName)
            .addStatement("$T<$T, $T> $L = new $T<>()", 
                Map.class, String.class, Object.class, mapFieldName, HashMap.class)
            .beginControlFlow("for ($T.Entry<$T, $T> entry : obj.$L().entrySet())", 
                Map.class, 
                TypeName.get(field.getMapKeyType()), 
                TypeName.get(field.getMapValueType()), 
                getterName)
            .beginControlFlow("if (entry.getValue() != null && !visited.contains(entry.getValue()))")
            .addStatement("String keyString = entry.getKey() != null ? entry.getKey().toString() : \"null\"")
            .addStatement("$L.put(keyString, $T.toMapWithTracking(entry.getValue(), visited))", 
                mapFieldName, mapperClass)
            .endControlFlow()
            .endControlFlow()
            .addStatement("map.put($L, $L)", getKeyConstantReference(field.getFieldName()), mapFieldName)
            .endControlFlow();
    }
    
    private boolean isStringKey(javax.lang.model.type.TypeMirror keyType) {
        if (keyType.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
            javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) keyType;
            javax.lang.model.element.TypeElement typeElement = (javax.lang.model.element.TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString().equals("java.lang.String");
        }
        return false;
    }
}
```

## Phase 5: CollectionCodeGenerator Integration

### 5.1 Extend CollectionCodeGenerator
**File:** `src/main/java/com/mapconverter/generator/CollectionCodeGenerator.java`

```java
public class CollectionCodeGenerator extends AbstractCodeGenerator {
    
    private TemporalCodeGenerator temporalGenerator;
    private MapCodeGenerator mapGenerator; // New dependency
    
    public void setMapGenerator(MapCodeGenerator mapGenerator) {
        this.mapGenerator = mapGenerator;
    }
    
    /**
     * Enhanced collection handling to include Maps
     */
    public void generateCollectionToMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName);
        
        // Check if it's a Map type
        if (field.getCollectionType() == FieldInfo.CollectionType.MAP || 
            field.getCollectionType() == FieldInfo.CollectionType.CONCURRENT_MAP) {
            mapGenerator.generateMapToMapCode(codeBlock, field, getterName);
        }
        // Existing List/Set/Array logic
        else if (field.isNestedCollection() && field.isNestedObjectGenerated()) {
            generateNestedCollectionToMapCode(codeBlock, field, getterName);
        } else if (field.isTemporalCollection()) {
            temporalGenerator.generateTemporalCollectionToMapCode(codeBlock, field, getterName);
        } else {
            generateRegularCollectionToMapCode(codeBlock, field, getterName);
        }
        
        codeBlock.endControlFlow();
    }
    
    /**
     * Enhanced collection from map handling to include Maps
     */
    public void generateCollectionFromMapCode(CodeBlock.Builder codeBlock, FieldInfo field, String setterName) {
        // Check if it's a Map type
        if (field.getCollectionType() == FieldInfo.CollectionType.MAP || 
            field.getCollectionType() == FieldInfo.CollectionType.CONCURRENT_MAP) {
            mapGenerator.generateMapFromMapCode(codeBlock, field, setterName);
        } else {
            // Existing List/Set/Array logic
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
    }
    
    /**
     * Enhanced collection with tracking to include Maps
     */
    public void generateCollectionToMapCodeWithTracking(CodeBlock.Builder codeBlock, FieldInfo field, String getterName) {
        codeBlock.beginControlFlow("if (obj.$L() != null)", getterName);
        
        // Check if it's a Map type
        if (field.getCollectionType() == FieldInfo.CollectionType.MAP || 
            field.getCollectionType() == FieldInfo.CollectionType.CONCURRENT_MAP) {
            mapGenerator.generateMapToMapCodeWithTracking(codeBlock, field, getterName);
        }
        // Existing List/Set/Array logic with tracking
        else if (field.isNestedCollection() && field.isNestedObjectGenerated()) {
            generateNestedCollectionToMapCodeWithTracking(codeBlock, field, getterName);
        } else if (field.isTemporalCollection()) {
            temporalGenerator.generateTemporalCollectionToMapCode(codeBlock, field, getterName);
        } else {
            generateRegularCollectionToMapCode(codeBlock, field, getterName);
        }
        
        codeBlock.endControlFlow();
    }
}
```

### 5.2 Update MapperCodeGenerator
**File:** `src/main/java/com/mapconverter/generator/MapperCodeGenerator.java:34-48`

```java
public MapperCodeGenerator(Filer filer, Elements elementUtils, Types typeUtils) {
    super(elementUtils, typeUtils);
    this.filer = filer;
    
    // Initialize specialized generators
    this.temporalGenerator = new TemporalCodeGenerator(elementUtils, typeUtils);
    this.nestedObjectGenerator = new NestedObjectCodeGenerator(elementUtils, typeUtils);
    this.collectionGenerator = new CollectionCodeGenerator(elementUtils, typeUtils);
    this.mapGenerator = new MapCodeGenerator(elementUtils, typeUtils); // New
    this.recordGenerator = new RecordCodeGenerator(elementUtils, typeUtils, temporalGenerator, nestedObjectGenerator, collectionGenerator);
    this.classGenerator = new ClassCodeGenerator(elementUtils, typeUtils, temporalGenerator, nestedObjectGenerator, collectionGenerator);
    this.externalMapperGenerator = new ExternalMapperGenerator(elementUtils, typeUtils);
    
    // Set up dependencies
    this.collectionGenerator.setTemporalGenerator(temporalGenerator);
    this.collectionGenerator.setMapGenerator(mapGenerator); // New dependency
    this.mapGenerator.setTemporalGenerator(temporalGenerator);
    this.mapGenerator.setNestedObjectGenerator(nestedObjectGenerator);
}
```

## Phase 6: Configuration & Annotations

### 6.1 Enhanced @MapField Support for Maps
```java
@MapperGenerate
public class UserProfile {
    @MapField("user_preferences")  // Custom key for the entire map
    private Map<String, String> preferences;
    
    // Standard map field - no annotation needed
    private Map<String, Integer> scores;
}

// Generated output:
// {"user_preferences": {"theme": "dark", "lang": "en"}}
// {"scores": {"math": 95, "english": 87}}
```

### 6.2 New @MapConfiguration Annotation
**File:** `src/main/java/com/mapconverter/annotations/MapConfiguration.java`

```java
package com.mapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advanced configuration for Map field handling.
 * Provides fine-grained control over Map serialization behavior.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapConfiguration {
    
    /**
     * Strategy for handling Map keys when key type is not String.
     * Default strategy converts keys using toString() method.
     */
    KeyStrategy keyStrategy() default KeyStrategy.TO_STRING;
    
    /**
     * Whether to flatten map entries into parent object instead of nested map.
     * When true, Map<String, String> becomes individual key-value pairs in parent.
     */
    boolean flatten() default false;
    
    /**
     * Prefix for keys when flattening is enabled.
     * Only used when flatten = true.
     */
    String keyPrefix() default "";
    
    /**
     * Maximum depth for nested maps.
     * -1 means unlimited depth (default).
     */
    int maxDepth() default -1;
    
    /**
     * Whether to preserve null values in maps.
     * When false, null values are omitted from serialized maps.
     */
    boolean preserveNullValues() default true;
    
    /**
     * Key conversion strategies for non-String keys.
     */
    enum KeyStrategy {
        /**
         * Convert keys using toString() method (default).
         * Safe for most key types but may lose type information.
         */
        TO_STRING,
        
        /**
         * Serialize keys as JSON strings.
         * Preserves more type information but requires JSON parsing.
         */
        JSON_SERIALIZE,
        
        /**
         * Skip entries with non-String keys.
         * Only processes entries where key instanceof String.
         */
        IGNORE_NON_STRING,
        
        /**
         * Throw exception when encountering non-String keys.
         * Strict mode for ensuring type safety.
         */
        STRICT_STRING_ONLY
    }
}
```

### 6.3 Usage Examples with Annotations

```java
@MapperGenerate
public class AdvancedMapExample {
    
    // Basic map - no configuration needed
    private Map<String, String> basicMap;
    
    // Custom key for entire map
    @MapField("user_settings")
    private Map<String, String> settings;
    
    // Advanced configuration
    @MapConfiguration(
        keyStrategy = MapConfiguration.KeyStrategy.TO_STRING,
        preserveNullValues = false
    )
    private Map<UserId, UserProfile> userProfiles;
    
    // Flattened map
    @MapConfiguration(
        flatten = true,
        keyPrefix = "config_"
    )
    private Map<String, String> configuration;
    // Results in: {"config_theme": "dark", "config_lang": "en"}
    
    // Strict string-only keys
    @MapConfiguration(keyStrategy = MapConfiguration.KeyStrategy.STRICT_STRING_ONLY)
    private Map<String, Address> addresses;
}
```

## Phase 7: Test Cases & Examples

### 7.1 Basic Map Test Cases

```java
// Test Case 1: Simple String-to-Primitive Maps
@MapperGenerate
public class GameStats {
    private Map<String, Integer> scores;        // {"math": 95, "english": 87}
    private Map<String, String> preferences;    // {"theme": "dark", "lang": "en"}
    private Map<String, Boolean> features;      // {"premium": true, "beta": false}
    private Map<String, Double> averages;       // {"gpa": 3.75, "attendance": 0.95}
}

// Test Case 2: Nested Object Maps
@MapperGenerate
public class AddressBook {
    private Map<String, Address> addresses;     // {"home": {}, "work": {}}
    private Map<String, List<Phone>> contacts;  // {"john": [{}, {}], "jane": [{}]}
    private Map<String, Set<String>> tags;      // {"john": ["friend", "colleague"]}
}

// Test Case 3: Complex Key Types
@MapperGenerate
public class OrderManagement {
    private Map<OrderId, OrderDetails> orders;      // Custom key conversion
    private Map<LocalDate, BigDecimal> dailySales;  // Temporal keys
    private Map<User, Set<Permission>> userPerms;   // Nested key/value
    private Map<Integer, String> statusCodes;       // Primitive key conversion
}

// Test Case 4: Circular References in Maps
@MapperGenerate
public class UserNetwork {
    private Map<String, User> connections;      // Potential circular refs
    @MapIgnoreCircular
    private Map<String, User> blockedUsers;     // Skip if circular
    private Map<String, UserGroup> groups;      // Nested circular potential
}
```

### 7.2 Edge Cases & Error Handling

```java
// Test Case 5: Null Handling
@MapperGenerate
public class NullMapTest {
    private Map<String, String> nullMap;       // null map
    private Map<String, String> emptyMap;      // empty map = {}
    private Map<String, String> nullValues;    // {"key": null}
    private Map<String, String> nullKeys;      // {null: "value"} - edge case
}

// Test Case 6: Generic Edge Cases
@MapperGenerate
public class GenericMapTest {
    @SuppressWarnings("rawtypes")
    private Map rawMap;                        // Raw map (no generics)
    private Map<?, ?> wildcardMap;             // Wildcard generics
    private Map<String, Object> objectMap;     // Object values
    private Map<Object, String> objectKeys;    // Object keys
}

// Test Case 7: Configuration Test Cases
@MapperGenerate
public class ConfigurationTest {
    @MapConfiguration(flatten = true, keyPrefix = "flat_")
    private Map<String, String> flattenedMap;
    
    @MapConfiguration(keyStrategy = MapConfiguration.KeyStrategy.JSON_SERIALIZE)
    private Map<ComplexKey, String> jsonKeyMap;
    
    @MapConfiguration(preserveNullValues = false)
    private Map<String, String> noNullsMap;
    
    @MapConfiguration(maxDepth = 2)
    private Map<String, Map<String, Map<String, String>>> deepMap;
}

// Test Case 8: Performance Test
@MapperGenerate
public class PerformanceTest {
    private Map<String, String> smallMap;      // 10 entries
    private Map<String, String> mediumMap;     // 1000 entries  
    private Map<String, String> largeMap;      // 100000 entries
    private Map<String, ComplexObject> nestedMap; // Nested performance
}
```

### 7.3 Integration Test Cases

```java
// Test Case 9: Mixed Collections and Maps
@MapperGenerate
public class MixedCollectionsTest {
    private List<Map<String, String>> listOfMaps;
    private Map<String, List<String>> mapOfLists;
    private Set<Map<String, Object>> setOfMaps;
    private Map<String, Set<Integer>> mapOfSets;
    private Map<String, String[]> mapOfArrays;
}

// Test Case 10: Records with Maps
@MapperGenerate
public record MapRecord(
    String name,
    Map<String, Integer> scores,
    Map<String, Address> addresses,
    @MapField("user_prefs") Map<String, String> preferences
) {}

// Test Case 11: External Objects with Maps
@ExternalMapper(targetClass = ExternalEntity.class)
public class ExternalMapConfig {
    String id;
    @ExternalField("entity_properties")
    Map<String, String> properties;
    Map<String, ExternalNestedObject> nestedObjects;
}
```

## Phase 8: Implementation Timeline

### Sprint 1 (Week 1): Foundation
- [ ] **Day 1-2**: Extend `FieldInfo` with Map support fields and updated constructor
- [ ] **Day 3-4**: Update `CollectionType` and `FieldType` enums
- [ ] **Day 5**: Implement Map type detection in `MapperProcessor.analyzeCollectionType()`

### Sprint 2 (Week 2): Core Map Functionality  
- [ ] **Day 1-2**: Create `MapCodeGenerator` class structure and basic methods
- [ ] **Day 3-4**: Implement `Map<String, Primitive>` to/from conversion
- [ ] **Day 5**: Add key type analysis and extraction logic

### Sprint 3 (Week 3): Nested Object Support
- [ ] **Day 1-2**: Implement `Map<String, CustomObject>` conversion
- [ ] **Day 3**: Add nested object dependency tracking for Maps
- [ ] **Day 4-5**: Integrate with existing circular reference protection

### Sprint 4 (Week 4): Advanced Features
- [ ] **Day 1-2**: Add support for non-String keys (toString conversion)
- [ ] **Day 3**: Implement temporal Map support (`Map<String, LocalDate>`)
- [ ] **Day 4-5**: Create `@MapConfiguration` annotation and processing

### Sprint 5 (Week 5): Integration & Polish
- [ ] **Day 1-2**: Integrate `MapCodeGenerator` with `CollectionCodeGenerator` 
- [ ] **Day 3**: Update `MapperCodeGenerator` to use Map support
- [ ] **Day 4**: Write comprehensive unit tests
- [ ] **Day 5**: Performance testing and optimization

### Sprint 6 (Week 6): Documentation & Testing
- [ ] **Day 1-2**: Create comprehensive test suite for all Map scenarios
- [ ] **Day 3**: Performance benchmarking against existing collection types
- [ ] **Day 4-5**: Documentation, examples, and README updates

## Expected Generated Code Examples

### 1. Simple Map
```java
// Input: Map<String, Integer> scores
// Generated toMap():
if (obj.getScores() != null) {
    map.put("scores", new HashMap<>(obj.getScores()));
}

// Generated fromMap():
Object scoresValue = map.get("scores");
if (scoresValue instanceof Map) {
    Map<?, ?> scoresMap = (Map<?, ?>) scoresValue;
    Map<String, Integer> scores = new HashMap<>();
    for (Map.Entry<?, ?> entry : scoresMap.entrySet()) {
        if (entry.getKey() instanceof String && entry.getValue() instanceof Integer) {
            scores.put((String) entry.getKey(), (Integer) entry.getValue());
        }
    }
    obj.setScores(scores);
}
```

### 2. Nested Object Map
```java
// Input: Map<String, Address> addresses
// Generated toMap():
if (obj.getAddresses() != null) {
    Map<String, Object> addressesMap = new HashMap<>();
    for (Map.Entry<String, Address> entry : obj.getAddresses().entrySet()) {
        if (entry.getValue() != null) {
            addressesMap.put(entry.getKey(), AddressFastMapper_.toMap(entry.getValue()));
        }
    }
    map.put("addresses", addressesMap);
}

// Generated fromMap():
Object addressesValue = map.get("addresses");
if (addressesValue instanceof Map) {
    Map<?, ?> addressesMap = (Map<?, ?>) addressesValue;
    Map<String, Address> addresses = new HashMap<>();
    for (Map.Entry<?, ?> entry : addressesMap.entrySet()) {
        if (entry.getKey() instanceof String && entry.getValue() instanceof Map) {
            String key = (String) entry.getKey();
            Address address = AddressFastMapper_.fromMap((Map<String, Object>) entry.getValue());
            addresses.put(key, address);
        }
    }
    obj.setAddresses(addresses);
}
```

### 3. Non-String Key Map
```java
// Input: Map<UserId, String> userNames
// Generated toMap():
if (obj.getUserNames() != null) {
    Map<String, Object> userNamesMap = new HashMap<>();
    for (Map.Entry<UserId, String> entry : obj.getUserNames().entrySet()) {
        String keyString = entry.getKey() != null ? entry.getKey().toString() : "null";
        userNamesMap.put(keyString, entry.getValue());
    }
    map.put("userNames", userNamesMap);
}
```

### 4. Circular Reference Map
```java
// Input: Map<String, User> connections (with circular reference protection)
// Generated toMapWithTracking():
if (obj.getConnections() != null) {
    Map<String, Object> connectionsMap = new HashMap<>();
    for (Map.Entry<String, User> entry : obj.getConnections().entrySet()) {
        if (entry.getValue() != null && !visited.contains(entry.getValue())) {
            connectionsMap.put(entry.getKey(), 
                              UserFastMapper_.toMapWithTracking(entry.getValue(), visited));
        }
    }
    map.put("connections", connectionsMap);
}
```

## Success Criteria

### Functional Requirements
- [ ] Support all common Map implementations (HashMap, LinkedHashMap, TreeMap, ConcurrentHashMap)
- [ ] Handle Map<String, Primitive> with direct embedding
- [ ] Handle Map<String, CustomObject> with nested object conversion
- [ ] Handle Map<CustomKey, V> with key serialization strategies
- [ ] Support temporal maps (Map<K, LocalDate>, etc.)
- [ ] Integrate with circular reference protection
- [ ] Support null key and value handling

### Performance Requirements
- [ ] Generated Map conversion code should be comparable to hand-written performance
- [ ] No significant performance degradation compared to existing List/Set collection handling
- [ ] Memory efficiency - minimal object allocation during conversion
- [ ] Large map handling (100K+ entries) without stack overflow

### Integration Requirements
- [ ] Seamless integration with existing `CollectionCodeGenerator`
- [ ] Compatible with all existing annotations (@MapField, @MapIgnore, etc.)
- [ ] Works with Records, regular classes, and external object mapping
- [ ] Maintains existing error handling and validation patterns

### Code Quality Requirements
- [ ] Generated code is readable and debuggable
- [ ] Comprehensive null safety checks
- [ ] Clear compilation error messages for invalid configurations
- [ ] Full test coverage for all Map scenarios

This comprehensive plan provides a structured approach to implementing Map<K,V> support while maintaining compatibility with the existing FastMapConverter architecture and design patterns.