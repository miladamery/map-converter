package com.mapconverter.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * Configuration model for external object mapping.
 * Contains all information needed to generate a mapper for an external class.
 */
public class ExternalMappingConfig {
    private final TypeElement configClass;
    private final TypeMirror targetClass;
    private final String mapperName;
    private final String packageName;
    private final boolean generateNestedMappers;
    private final List<ExternalFieldConfig> fields;

    public ExternalMappingConfig(TypeElement configClass, TypeMirror targetClass, 
                                String mapperName, String packageName, 
                                boolean generateNestedMappers, List<ExternalFieldConfig> fields) {
        this.configClass = configClass;
        this.targetClass = targetClass;
        this.mapperName = mapperName;
        this.packageName = packageName;
        this.generateNestedMappers = generateNestedMappers;
        this.fields = fields;
    }

    public TypeElement getConfigClass() {
        return configClass;
    }

    public TypeMirror getTargetClass() {
        return targetClass;
    }

    public String getMapperName() {
        return mapperName;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isGenerateNestedMappers() {
        return generateNestedMappers;
    }

    public List<ExternalFieldConfig> getFields() {
        return fields;
    }

    /**
     * Configuration for individual field mappings in external objects.
     */
    public static class ExternalFieldConfig {
        private final String configFieldName;   // Field name in config class
        private final String targetFieldName;   // Field name in target class
        private final String mapKey;            // Map key (from @ExternalField or field name)
        private final boolean ignored;          // Whether to ignore this field
        private final TypeMirror fieldType;     // Field type for validation
        private final TypeMirror converterType; // Custom converter type (if any)
        private final FieldInfo.FieldType fieldTypeEnum; // Enhanced field type classification
        private final FieldInfo.CollectionType collectionType; // Collection type if applicable
        private final TypeMirror elementType;   // Element type for collections
        private final boolean isExternalObject; // Whether field is an external object
        private final boolean isExternalCollection; // Whether field is collection of external objects

        public ExternalFieldConfig(String configFieldName, String targetFieldName, 
                                 String mapKey, boolean ignored, TypeMirror fieldType,
                                 TypeMirror converterType, FieldInfo.FieldType fieldTypeEnum,
                                 FieldInfo.CollectionType collectionType, TypeMirror elementType,
                                 boolean isExternalObject, boolean isExternalCollection) {
            this.configFieldName = configFieldName;
            this.targetFieldName = targetFieldName;
            this.mapKey = mapKey;
            this.ignored = ignored;
            this.fieldType = fieldType;
            this.converterType = converterType;
            this.fieldTypeEnum = fieldTypeEnum;
            this.collectionType = collectionType;
            this.elementType = elementType;
            this.isExternalObject = isExternalObject;
            this.isExternalCollection = isExternalCollection;
        }

        public String getConfigFieldName() {
            return configFieldName;
        }

        public String getTargetFieldName() {
            return targetFieldName;
        }

        public String getMapKey() {
            return mapKey;
        }

        public boolean isIgnored() {
            return ignored;
        }

        public TypeMirror getFieldType() {
            return fieldType;
        }

        public TypeMirror getConverterType() {
            return converterType;
        }

        public boolean hasCustomConverter() {
            return converterType != null;
        }

        public FieldInfo.FieldType getFieldTypeEnum() {
            return fieldTypeEnum;
        }

        public FieldInfo.CollectionType getCollectionType() {
            return collectionType;
        }

        public TypeMirror getElementType() {
            return elementType;
        }

        public boolean isExternalObject() {
            return isExternalObject;
        }

        public boolean isExternalCollection() {
            return isExternalCollection;
        }

        public boolean isCollection() {
            return collectionType != FieldInfo.CollectionType.NONE;
        }

        public boolean isPrimitive() {
            return fieldTypeEnum == FieldInfo.FieldType.PRIMITIVE;
        }
    }
}