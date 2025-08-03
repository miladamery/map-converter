package com.mapconverter.generator;

import com.mapconverter.processor.FieldInfo;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for mapper generation.
 */
public class MapperUtility {
    
    /**
     * Adds static final String constants for map keys to avoid string duplication.
     */
    public static void addMapKeyConstants(TypeSpec.Builder mapperBuilder, List<FieldInfo> fields) {
        for (FieldInfo field : fields) {
            if (field.isIgnored()) {
                continue;
            }
            
            String constantName = generateKeyConstantName(field.getFieldName());
            String mapKey = field.getMapKey();
            
            mapperBuilder.addField(
                FieldSpec.builder(String.class, constantName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", mapKey)
                    .build()
            );
        }
    }

    /**
     * Adds thread-local visited set pooling for efficient circular reference handling.
     */
    public static void addVisitedSetPool(TypeSpec.Builder mapperBuilder) {
        TypeName setType = ParameterizedTypeName.get(
            ClassName.get(Set.class),
            ClassName.get(Object.class)
        );
        
        TypeName threadLocalType = ParameterizedTypeName.get(
            ClassName.get("java.lang", "ThreadLocal"),
            setType
        );
        
        mapperBuilder.addField(
            FieldSpec.builder(threadLocalType, "VISITED_POOL", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.withInitial(() -> new $T<>())", 
                    ClassName.get("java.lang", "ThreadLocal"), HashSet.class)
                .build()
        );
        
        // Add method to get a clean visited set from pool
        MethodSpec getVisitedSetMethod = MethodSpec.methodBuilder("getVisitedSet")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(setType)
            .addStatement("$T<Object> visited = VISITED_POOL.get()", Set.class)
            .addStatement("visited.clear()")
            .addStatement("return visited")
            .build();
            
        mapperBuilder.addMethod(getVisitedSetMethod);
    }

    /**
     * Generates a constant name for a field's map key.
     */
    private static String generateKeyConstantName(String fieldName) {
        String snakeCase = fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        return snakeCase + "_KEY";
    }
}