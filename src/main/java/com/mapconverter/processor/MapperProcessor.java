package com.mapconverter.processor;

import com.mapconverter.annotations.*;
import com.mapconverter.enumeration.DateTimeStrategy;
import com.mapconverter.generator.MapperCodeGenerator;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Annotation processor for generating mapper classes from @MapperGenerate annotations.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "com.mapconverter.annotations.MapperGenerate",
    "com.mapconverter.annotations.ExternalMapper"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class MapperProcessor extends AbstractProcessor {
    
    private MapperRegistry mapperRegistry;
    private ExternalMappingRegistry externalMappingRegistry;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (mapperRegistry == null) {
            mapperRegistry = new MapperRegistry();
        }
        if (externalMappingRegistry == null) {
            externalMappingRegistry = new ExternalMappingRegistry();
        }
        
        try {
            // First pass: collect external mapping configurations
            collectExternalConfigurations(roundEnv);
            
            // Second pass: collect regular @MapperGenerate classes
            collectRegularMapperClasses(roundEnv);
            
            // Third pass: process all collected classes with dependency resolution
            processAllMappers();
            
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to generate mappers: " + e.getMessage()
            );
        }
        
        return true;
    }

    private void processClass(TypeElement typeElement) throws IOException {
        MapperGenerate annotation = typeElement.getAnnotation(MapperGenerate.class);
        String mapperClassName = annotation.className().isEmpty() 
            ? typeElement.getSimpleName() + "FastMapper_"
            : annotation.className();

        List<FieldInfo> fields;
        boolean isRecord = typeElement.getKind() == ElementKind.RECORD;
        
        if (isRecord) {
            fields = analyzeRecordComponents(typeElement);
        } else {
            fields = analyzeFields(typeElement);
        }
        
        MapperCodeGenerator generator = new MapperCodeGenerator(
            processingEnv.getFiler(),
            processingEnv.getElementUtils(),
            processingEnv.getTypeUtils()
        );
        
        generator.generateMapper(typeElement, mapperClassName, fields, isRecord);
    }

    private List<FieldInfo> analyzeFields(TypeElement classElement) {
        List<FieldInfo> fields = new ArrayList<>();
        
        for (Element enclosedElement : classElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.FIELD) {
                continue;
            }
            
            VariableElement fieldElement = (VariableElement) enclosedElement;
            
            // Skip static and final fields
            if (fieldElement.getModifiers().contains(Modifier.STATIC) ||
                fieldElement.getModifiers().contains(Modifier.FINAL)) {
                continue;
            }
            
            String fieldName = fieldElement.getSimpleName().toString();
            boolean ignored = fieldElement.getAnnotation(MapIgnore.class) != null;
            
            String mapKey = fieldName;
            MapField mapFieldAnnotation = fieldElement.getAnnotation(MapField.class);
            if (mapFieldAnnotation != null) {
                mapKey = mapFieldAnnotation.value();
            }
            
            // Analyze collection type and field type
            TypeMirror fieldType = fieldElement.asType();
            FieldInfo.CollectionType collectionType = analyzeCollectionType(fieldType);
            TypeMirror elementType = extractElementType(fieldType, collectionType);
            
            // Analyze nested object type
            FieldInfo.FieldType fieldTypeEnum = analyzeFieldType(fieldType);
            TypeMirror nestedObjectType = null;
            TypeMirror nestedElementType = null;
            boolean isNestedObjectGenerated = false;
            
            if (fieldTypeEnum == FieldInfo.FieldType.NESTED_OBJECT) {
                nestedObjectType = fieldType;
                isNestedObjectGenerated = isCustomObject(fieldType);
            } else if (fieldTypeEnum == FieldInfo.FieldType.NESTED_COLLECTION && elementType != null) {
                nestedElementType = elementType;
                isNestedObjectGenerated = isCustomObject(elementType);
            }
            
            // Extract @MapDateTime configuration
            DateTimeStrategy dateTimeStrategy = null;
            String dateTimePattern = "";
            String dateTimeTimezone = "";
            boolean preserveNanos = false;
            boolean lenientParsing = true;
            
            MapDateTime mapDateTimeAnnotation = fieldElement.getAnnotation(MapDateTime.class);
            if (mapDateTimeAnnotation != null) {
                dateTimeStrategy = mapDateTimeAnnotation.strategy();
                dateTimePattern = mapDateTimeAnnotation.pattern();
                dateTimeTimezone = mapDateTimeAnnotation.timezone();
                preserveNanos = mapDateTimeAnnotation.preserveNanos();
                lenientParsing = mapDateTimeAnnotation.lenientParsing();
            } else if (fieldTypeEnum == FieldInfo.FieldType.TEMPORAL || fieldTypeEnum == FieldInfo.FieldType.TEMPORAL_COLLECTION) {
                // Set default strategy for temporal fields without annotation
                dateTimeStrategy = DateTimeStrategy.AUTO;
            }
            
            FieldInfo fieldInfo = new FieldInfo(
                fieldName,
                mapKey,
                fieldType,
                ignored,
                fieldElement,
                false, // isRecordComponent - false for regular fields
                collectionType,
                elementType,
                fieldTypeEnum,
                nestedObjectType,
                nestedElementType,
                isNestedObjectGenerated,
                dateTimeStrategy,
                dateTimePattern,
                dateTimeTimezone,
                preserveNanos,
                lenientParsing
            );
            
            fields.add(fieldInfo);
        }
        
        return fields;
    }

    /**
     * Analyzes record components and creates FieldInfo objects for each component.
     */
    private List<FieldInfo> analyzeRecordComponents(TypeElement recordElement) {
        List<FieldInfo> components = new ArrayList<>();
        
        for (Element enclosedElement : recordElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.RECORD_COMPONENT) {
                continue;
            }
            
            RecordComponentElement componentElement = (RecordComponentElement) enclosedElement;
            
            String componentName = componentElement.getSimpleName().toString();
            boolean ignored = componentElement.getAnnotation(MapIgnore.class) != null;
            
            String mapKey = componentName;
            MapField mapFieldAnnotation = componentElement.getAnnotation(MapField.class);
            if (mapFieldAnnotation != null) {
                mapKey = mapFieldAnnotation.value();
            }
            
            // Analyze collection type and field type
            TypeMirror componentType = componentElement.asType();
            FieldInfo.CollectionType collectionType = analyzeCollectionType(componentType);
            TypeMirror elementType = extractElementType(componentType, collectionType);
            
            // Analyze nested object type
            FieldInfo.FieldType fieldTypeEnum = analyzeFieldType(componentType);
            TypeMirror nestedObjectType = null;
            TypeMirror nestedElementType = null;
            boolean isNestedObjectGenerated = false;
            
            if (fieldTypeEnum == FieldInfo.FieldType.NESTED_OBJECT) {
                nestedObjectType = componentType;
                isNestedObjectGenerated = isCustomObject(componentType);
            } else if (fieldTypeEnum == FieldInfo.FieldType.NESTED_COLLECTION && elementType != null) {
                nestedElementType = elementType;
                isNestedObjectGenerated = isCustomObject(elementType);
            }
            
            // Extract @MapDateTime configuration
            DateTimeStrategy dateTimeStrategy = null;
            String dateTimePattern = "";
            String dateTimeTimezone = "";
            boolean preserveNanos = false;
            boolean lenientParsing = true;
            
            MapDateTime mapDateTimeAnnotation = componentElement.getAnnotation(MapDateTime.class);
            if (mapDateTimeAnnotation != null) {
                dateTimeStrategy = mapDateTimeAnnotation.strategy();
                dateTimePattern = mapDateTimeAnnotation.pattern();
                dateTimeTimezone = mapDateTimeAnnotation.timezone();
                preserveNanos = mapDateTimeAnnotation.preserveNanos();
                lenientParsing = mapDateTimeAnnotation.lenientParsing();
            } else if (fieldTypeEnum == FieldInfo.FieldType.TEMPORAL || fieldTypeEnum == FieldInfo.FieldType.TEMPORAL_COLLECTION) {
                // Set default strategy for temporal fields without annotation
                dateTimeStrategy = DateTimeStrategy.AUTO;
            }
            
            FieldInfo componentInfo = new FieldInfo(
                componentName,
                mapKey,
                componentType,
                ignored,
                componentElement,
                true, // isRecordComponent - true for record components
                collectionType,
                elementType,
                fieldTypeEnum,
                nestedObjectType,
                nestedElementType,
                isNestedObjectGenerated,
                dateTimeStrategy,
                dateTimePattern,
                dateTimeTimezone,
                preserveNanos,
                lenientParsing
            );
            
            components.add(componentInfo);
        }
        
        return components;
    }

    private FieldInfo.CollectionType analyzeCollectionType(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            return FieldInfo.CollectionType.ARRAY;
        }
        
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            String qualifiedName = typeElement.getQualifiedName().toString();
            
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

    private TypeMirror extractElementType(TypeMirror type, FieldInfo.CollectionType collectionType) {
        switch (collectionType) {
            case ARRAY:
                ArrayType arrayType = (ArrayType) type;
                return arrayType.getComponentType();
                
            case LIST:
            case SET:
                DeclaredType declaredType = (DeclaredType) type;
                if (!declaredType.getTypeArguments().isEmpty()) {
                    return declaredType.getTypeArguments().get(0);
                }
                // Return Object type if no generic parameter
                return processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
                
            default:
                return null;
        }
    }

    /**
     * Processes all nested objects in dependency order using topological sorting.
     */
    private void processNestedObjects() throws IOException {
        Map<String, TypeElement> pendingTypes = mapperRegistry.getPendingTypes();
        
        // Build dependency graph
        Map<String, Set<String>> dependencies = buildDependencyGraph(pendingTypes);
        
        // Perform topological sort
        List<String> processingOrder = topologicalSort(dependencies, pendingTypes.keySet());
        
        // Process in dependency order
        for (String className : processingOrder) {
            if (!mapperRegistry.isMapperGenerated(className)) {
                TypeElement typeElement = pendingTypes.get(className);
                if (typeElement != null) {
                    processClass(typeElement);
                    mapperRegistry.registerMapper(className);
                }
            }
        }
    }

    /**
     * Builds a dependency graph for all pending types.
     */
    private Map<String, Set<String>> buildDependencyGraph(Map<String, TypeElement> pendingTypes) {
        Map<String, Set<String>> dependencies = new HashMap<>();
        
        for (Map.Entry<String, TypeElement> entry : pendingTypes.entrySet()) {
            String className = entry.getKey();
            TypeElement typeElement = entry.getValue();
            
            Set<String> classDependencies = findNestedObjectDependencies(typeElement, pendingTypes.keySet());
            dependencies.put(className, classDependencies);
        }
        
        return dependencies;
    }

    /**
     * Finds all nested object dependencies for a given class.
     */
    private Set<String> findNestedObjectDependencies(TypeElement classElement, Set<String> allPendingTypes) {
        Set<String> dependencies = new HashSet<>();
        
        for (Element enclosedElement : classElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.FIELD) {
                continue;
            }
            
            VariableElement fieldElement = (VariableElement) enclosedElement;
            
            // Skip static and final fields
            if (fieldElement.getModifiers().contains(Modifier.STATIC) ||
                fieldElement.getModifiers().contains(Modifier.FINAL)) {
                continue;
            }
            
            // Skip ignored fields
            if (fieldElement.getAnnotation(MapIgnore.class) != null) {
                continue;
            }
            
            TypeMirror fieldType = fieldElement.asType();
            
            // Check for nested object dependencies
            if (isCustomObject(fieldType)) {
                String dependentClass = getQualifiedTypeName(fieldType);
                if (allPendingTypes.contains(dependentClass)) {
                    dependencies.add(dependentClass);
                }
            }
            
            // Check for collection element dependencies
            if (isCollectionType(fieldType)) {
                TypeMirror elementType = extractElementTypeForAnalysis(fieldType);
                if (elementType != null && isCustomObject(elementType)) {
                    String dependentClass = getQualifiedTypeName(elementType);
                    if (allPendingTypes.contains(dependentClass)) {
                        dependencies.add(dependentClass);
                    }
                }
            }
        }
        
        return dependencies;
    }

    /**
     * Performs topological sorting using Kahn's algorithm.
     */
    private List<String> topologicalSort(Map<String, Set<String>> dependencies, Set<String> allNodes) {
        // Calculate in-degrees
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : allNodes) {
            inDegree.put(node, 0);
        }
        
        for (Set<String> deps : dependencies.values()) {
            for (String dep : deps) {
                inDegree.put(dep, inDegree.getOrDefault(dep, 0) + 1);
            }
        }
        
        // Initialize queue with nodes having no dependencies
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<String> result = new ArrayList<>();
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            
            // Process dependents
            Set<String> currentDeps = dependencies.getOrDefault(current, new HashSet<>());
            for (String dependent : currentDeps) {
                inDegree.put(dependent, inDegree.get(dependent) - 1);
                if (inDegree.get(dependent) == 0) {
                    queue.offer(dependent);
                }
            }
        }
        
        // Check for circular dependencies
        if (result.size() != allNodes.size()) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "Circular dependencies detected in mapper generation. Some mappers may not be generated correctly."
            );
            
            // Add remaining nodes to result (fallback for circular dependencies)
            for (String node : allNodes) {
                if (!result.contains(node)) {
                    result.add(node);
                }
            }
        }
        
        return result;
    }

    /**
     * Collects all @ExternalMapper configurations in the first pass.
     */
    private void collectExternalConfigurations(RoundEnvironment roundEnv) {
        Set<? extends Element> externalMapperElements = roundEnv.getElementsAnnotatedWith(ExternalMapper.class);
        
        for (Element element : externalMapperElements) {
            if (element.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@ExternalMapper can only be applied to classes",
                    element
                );
                continue;
            }
            
            TypeElement configClass = (TypeElement) element;
            ExternalMapper annotation = configClass.getAnnotation(ExternalMapper.class);
            
            try {
                // Extract target class information
                TypeMirror targetClass = getTargetClassFromAnnotation(annotation, configClass);
                
                // Validate target class accessibility
                if (!isTargetClassAccessible(targetClass)) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Target class not accessible: " + targetClass,
                        configClass
                    );
                    continue;
                }
                
                // Build configuration
                ExternalMappingConfig config = buildExternalMappingConfig(configClass, targetClass, annotation);
                String targetClassName = getQualifiedTypeName(targetClass);
                externalMappingRegistry.registerExternalConfig(targetClassName, config);
                
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to process external mapper configuration: " + e.getMessage(),
                    configClass
                );
            }
        }
    }

    /**
     * Collects all regular @MapperGenerate classes and records.
     */
    private void collectRegularMapperClasses(RoundEnvironment roundEnv) {
        Set<? extends Element> mapperGenerateElements = roundEnv.getElementsAnnotatedWith(MapperGenerate.class);
        
        for (Element element : mapperGenerateElements) {
            if (element.getKind() != ElementKind.CLASS && element.getKind() != ElementKind.RECORD) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@MapperGenerate can only be applied to classes and records",
                    element
                );
                continue;
            }
            
            TypeElement typeElement = (TypeElement) element;
            String typeName = typeElement.getQualifiedName().toString();
            
            if (!mapperRegistry.isMapperGenerated(typeName)) {
                mapperRegistry.addToProcessingQueue(typeName, typeElement);
            }
        }
    }

    /**
     * Processes both regular and external mappers with dependency resolution.
     */
    private void processAllMappers() throws IOException {
        // First process regular mappers (existing functionality)
        processNestedObjects();
        
        // Then process external mappers
        processExternalMappers();
    }

    /**
     * Processes all external mappers with validation and code generation.
     */
    private void processExternalMappers() throws IOException {
        Map<String, ExternalMappingConfig> externalConfigs = externalMappingRegistry.getAllExternalConfigs();
        
        for (Map.Entry<String, ExternalMappingConfig> entry : externalConfigs.entrySet()) {
            String targetClassName = entry.getKey();
            ExternalMappingConfig config = entry.getValue();
            
            if (!externalMappingRegistry.isExternalMapperGenerated(targetClassName)) {
                // Validate configuration
                if (validateExternalMappingConfig(config)) {
                    // Generate external mapper
                    generateExternalMapper(config);
                    externalMappingRegistry.markExternalMapperGenerated(targetClassName);
                }
            }
        }
    }

    /**
     * Extracts the target class type from @ExternalMapper annotation.
     */
    private TypeMirror getTargetClassFromAnnotation(ExternalMapper annotation, TypeElement configClass) {
        try {
            // This will throw MirroredTypeException, which is expected
            annotation.targetClass();
            return null; // This line should never be reached
        } catch (javax.lang.model.type.MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    /**
     * Checks if the target class is accessible for processing.
     */
    private boolean isTargetClassAccessible(TypeMirror targetClass) {
        if (targetClass.getKind() != javax.lang.model.type.TypeKind.DECLARED) {
            return false;
        }
        
        javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) targetClass;
        javax.lang.model.element.TypeElement typeElement = (javax.lang.model.element.TypeElement) declaredType.asElement();
        
        // Check if the class exists and is accessible
        return typeElement != null;
    }

    /**
     * Builds external mapping configuration from annotation and config class.
     */
    private ExternalMappingConfig buildExternalMappingConfig(TypeElement configClass, TypeMirror targetClass, ExternalMapper annotation) {
        String mapperName = annotation.mapperName().isEmpty() 
            ? getSimpleTypeName(targetClass) + "FastMapper_"
            : annotation.mapperName();
            
        String packageName = annotation.packageName().isEmpty()
            ? processingEnv.getElementUtils().getPackageOf(configClass).getQualifiedName().toString()
            : annotation.packageName();
            
        List<ExternalMappingConfig.ExternalFieldConfig> fieldConfigs = extractExternalFieldMappings(configClass, targetClass);
        
        return new ExternalMappingConfig(
            configClass,
            targetClass,
            mapperName,
            packageName,
            annotation.generateNestedMappers(),
            fieldConfigs
        );
    }

    /**
     * Extracts field mapping configurations from the config class.
     */
    private List<ExternalMappingConfig.ExternalFieldConfig> extractExternalFieldMappings(TypeElement configClass, TypeMirror targetClass) {
        List<ExternalMappingConfig.ExternalFieldConfig> fieldConfigs = new ArrayList<>();
        javax.lang.model.element.TypeElement targetElement = (javax.lang.model.element.TypeElement) ((javax.lang.model.type.DeclaredType) targetClass).asElement();
        
        for (Element enclosedElement : configClass.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement configField = (VariableElement) enclosedElement;
                
                // Skip static and final fields
                if (configField.getModifiers().contains(Modifier.STATIC) ||
                    configField.getModifiers().contains(Modifier.FINAL)) {
                    continue;
                }
                
                String configFieldName = configField.getSimpleName().toString();
                String targetFieldName = configFieldName; // Default to same name
                
                // Check if target field exists
                VariableElement targetField = findFieldInTargetClass(targetFieldName, targetElement);
                if (targetField == null) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Field '" + targetFieldName + "' not found in target class: " + targetClass,
                        configField
                    );
                    continue;
                }
                
                // Process @ExternalField annotation
                ExternalField externalFieldAnnotation = configField.getAnnotation(ExternalField.class);
                String mapKey = configFieldName; // Default to field name
                boolean ignored = false;
                TypeMirror converterType = null;
                
                if (externalFieldAnnotation != null) {
                    if (!externalFieldAnnotation.value().isEmpty()) {
                        mapKey = externalFieldAnnotation.value();
                    }
                    ignored = externalFieldAnnotation.ignore();
                    
                    try {
                        externalFieldAnnotation.converter();
                    } catch (javax.lang.model.type.MirroredTypeException e) {
                        TypeMirror converterTypeMirror = e.getTypeMirror();
                        if (!converterTypeMirror.toString().equals("java.lang.Void")) {
                            converterType = converterTypeMirror;
                        }
                    }
                }
                
                // Analyze field type
                TypeMirror fieldType = targetField.asType();
                FieldInfo.CollectionType collectionType = analyzeCollectionType(fieldType);
                TypeMirror elementType = extractElementType(fieldType, collectionType);
                FieldInfo.FieldType fieldTypeEnum = analyzeFieldType(fieldType);
                
                // Check if field is external object or collection of external objects
                boolean isExternalObject = isExternalObjectType(fieldType);
                boolean isExternalCollection = collectionType != FieldInfo.CollectionType.NONE && 
                                             elementType != null && isExternalObjectType(elementType);
                
                ExternalMappingConfig.ExternalFieldConfig fieldConfig = new ExternalMappingConfig.ExternalFieldConfig(
                    configFieldName,
                    targetFieldName,
                    mapKey,
                    ignored,
                    fieldType,
                    converterType,
                    fieldTypeEnum,
                    collectionType,
                    elementType,
                    isExternalObject,
                    isExternalCollection
                );
                
                fieldConfigs.add(fieldConfig);
            }
        }
        
        return fieldConfigs;
    }

    /**
     * Finds a field in the target class by name.
     */
    private VariableElement findFieldInTargetClass(String fieldName, javax.lang.model.element.TypeElement targetClass) {
        for (Element enclosedElement : targetClass.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;
                if (field.getSimpleName().toString().equals(fieldName)) {
                    return field;
                }
            }
        }
        return null;
    }

    /**
     * Validates an external mapping configuration.
     */
    private boolean validateExternalMappingConfig(ExternalMappingConfig config) {
        boolean valid = true;
        
        // Validate target class
        javax.lang.model.element.TypeElement targetElement = (javax.lang.model.element.TypeElement) ((javax.lang.model.type.DeclaredType) config.getTargetClass()).asElement();
        
        // Check for required constructors
        if (!hasDefaultConstructor(targetElement) && !hasAllArgsConstructor(targetElement)) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Target class must have default constructor or all-args constructor: " + config.getTargetClass(),
                config.getConfigClass()
            );
            valid = false;
        }
        
        // Validate field mappings
        for (ExternalMappingConfig.ExternalFieldConfig fieldConfig : config.getFields()) {
            if (!validateExternalFieldConfig(fieldConfig, config)) {
                valid = false;
            }
        }
        
        return valid;
    }

    /**
     * Validates an individual external field configuration.
     */
    private boolean validateExternalFieldConfig(ExternalMappingConfig.ExternalFieldConfig fieldConfig, ExternalMappingConfig config) {
        if (fieldConfig.isIgnored()) {
            return true; // Ignored fields don't need validation
        }
        
        // Find target field
        javax.lang.model.element.TypeElement targetElement = (javax.lang.model.element.TypeElement) ((javax.lang.model.type.DeclaredType) config.getTargetClass()).asElement();
        VariableElement targetField = findFieldInTargetClass(fieldConfig.getTargetFieldName(), targetElement);
        
        if (targetField == null) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Field not found in target class: " + fieldConfig.getTargetFieldName(),
                config.getConfigClass()
            );
            return false;
        }
        
        // Check field accessibility (has getter/setter)
        if (!hasFieldAccessors(targetField, config.getTargetClass())) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "Field may not be accessible (no getter/setter): " + fieldConfig.getTargetFieldName(),
                config.getConfigClass()
            );
        }
        
        return true;
    }

    /**
     * Checks if a class has a default constructor.
     */
    private boolean hasDefaultConstructor(javax.lang.model.element.TypeElement typeElement) {
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                javax.lang.model.element.ExecutableElement constructor = (javax.lang.model.element.ExecutableElement) enclosedElement;
                if (constructor.getParameters().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a class has an all-args constructor.
     */
    private boolean hasAllArgsConstructor(javax.lang.model.element.TypeElement typeElement) {
        List<VariableElement> fields = new ArrayList<>();
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;
                if (!field.getModifiers().contains(Modifier.STATIC) &&
                    !field.getModifiers().contains(Modifier.FINAL)) {
                    fields.add(field);
                }
            }
        }
        
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                javax.lang.model.element.ExecutableElement constructor = (javax.lang.model.element.ExecutableElement) enclosedElement;
                if (constructor.getParameters().size() == fields.size()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a field has getter and setter methods.
     */
    private boolean hasFieldAccessors(VariableElement field, TypeMirror targetClass) {
        javax.lang.model.element.TypeElement targetElement = (javax.lang.model.element.TypeElement) ((javax.lang.model.type.DeclaredType) targetClass).asElement();
        String fieldName = field.getSimpleName().toString();
        String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        
        boolean hasGetter = false;
        boolean hasSetter = false;
        
        for (Element enclosedElement : targetElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                javax.lang.model.element.ExecutableElement method = (javax.lang.model.element.ExecutableElement) enclosedElement;
                String methodName = method.getSimpleName().toString();
                
                // Check for getter
                if ((methodName.equals("get" + capitalizedFieldName) || methodName.equals("is" + capitalizedFieldName)) &&
                    method.getParameters().isEmpty()) {
                    hasGetter = true;
                }
                
                // Check for setter
                if (methodName.equals("set" + capitalizedFieldName) &&
                    method.getParameters().size() == 1) {
                    hasSetter = true;
                }
            }
        }
        
        return hasGetter && hasSetter;
    }

    /**
     * Generates an external mapper class.
     */
    private void generateExternalMapper(ExternalMappingConfig config) throws IOException {
        MapperCodeGenerator generator = new MapperCodeGenerator(
            processingEnv.getFiler(),
            processingEnv.getElementUtils(),
            processingEnv.getTypeUtils()
        );
        
        generator.generateExternalMapper(config);
    }

    /**
     * Checks if a type is an external object (has external mapping configuration).
     */
    private boolean isExternalObjectType(TypeMirror type) {
        String typeName = getQualifiedTypeName(type);
        return externalMappingRegistry.hasExternalConfig(typeName);
    }

    /**
     * Gets the simple name of a type.
     */
    private String getSimpleTypeName(TypeMirror type) {
        if (type.getKind() == javax.lang.model.type.TypeKind.DECLARED) {
            javax.lang.model.type.DeclaredType declaredType = (javax.lang.model.type.DeclaredType) type;
            javax.lang.model.element.TypeElement typeElement = (javax.lang.model.element.TypeElement) declaredType.asElement();
            return typeElement.getSimpleName().toString();
        }
        return type.toString();
    }

    /**
     * Gets the qualified name of a type.
     */
    private String getQualifiedTypeName(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeElement typeElement = (TypeElement) declaredType.asElement();
            return typeElement.getQualifiedName().toString();
        }
        return type.toString();
    }

    /**
     * Analyzes the field type to determine if it's primitive, collection, nested object, temporal, etc.
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

    /**
     * Checks if the given type is a custom object that should have a mapper generated.
     */
    private boolean isCustomObject(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        
        // Skip standard Java packages
        if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
            return false;
        }
        
        // Check if has @MapperGenerate annotation
        return typeElement.getAnnotation(MapperGenerate.class) != null;
    }

    /**
     * Checks if the type is a standard Java type that should be treated as primitive.
     */
    private boolean isStandardJavaType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();
        
        return qualifiedName.equals("java.lang.String") ||
               qualifiedName.equals("java.lang.Integer") ||
               qualifiedName.equals("java.lang.Long") ||
               qualifiedName.equals("java.lang.Double") ||
               qualifiedName.equals("java.lang.Float") ||
               qualifiedName.equals("java.lang.Boolean") ||
               qualifiedName.equals("java.lang.Character") ||
               qualifiedName.equals("java.lang.Byte") ||
               qualifiedName.equals("java.lang.Short") ||
               qualifiedName.equals("java.math.BigDecimal") ||
               qualifiedName.equals("java.math.BigInteger");
    }

    /**
     * Checks if the type is a collection type.
     */
    private boolean isCollectionType(TypeMirror type) {
        return analyzeCollectionType(type) != FieldInfo.CollectionType.NONE;
    }

    /**
     * Extracts element type for field analysis (similar to existing method but for analysis).
     */
    private TypeMirror extractElementTypeForAnalysis(TypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) type;
            return arrayType.getComponentType();
        }
        
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (!declaredType.getTypeArguments().isEmpty()) {
                return declaredType.getTypeArguments().get(0);
            }
        }
        
        return null;
    }

    /**
     * Enhanced isPrimitive check.
     */
    private boolean isPrimitive(TypeMirror type) {
        return type.getKind().isPrimitive();
    }
    
    /**
     * Checks if the type is an enum type.
     */
    private boolean isEnumType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        
        return typeElement.getKind() == ElementKind.ENUM;
    }
    
    /**
     * Checks if the type is a temporal type (date/time related).
     */
    private boolean isTemporalType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        
        DeclaredType declaredType = (DeclaredType) type;
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        String qualifiedName = typeElement.getQualifiedName().toString();
        
        // java.time.* types
        return qualifiedName.equals("java.time.LocalDate") ||
               qualifiedName.equals("java.time.LocalDateTime") ||
               qualifiedName.equals("java.time.LocalTime") ||
               qualifiedName.equals("java.time.Instant") ||
               qualifiedName.equals("java.time.ZonedDateTime") ||
               qualifiedName.equals("java.time.OffsetDateTime") ||
               qualifiedName.equals("java.time.OffsetTime") ||
               qualifiedName.equals("java.time.Year") ||
               qualifiedName.equals("java.time.YearMonth") ||
               qualifiedName.equals("java.time.MonthDay") ||
               qualifiedName.equals("java.time.Duration") ||
               qualifiedName.equals("java.time.Period") ||
               // Legacy java.util.Date family
               qualifiedName.equals("java.util.Date") ||
               qualifiedName.equals("java.sql.Date") ||
               qualifiedName.equals("java.sql.Time") ||
               qualifiedName.equals("java.sql.Timestamp") ||
               qualifiedName.equals("java.util.Calendar") ||
               qualifiedName.equals("java.util.GregorianCalendar");
    }
}