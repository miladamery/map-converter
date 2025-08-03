package com.mapconverter.processor;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for tracking external mapping configurations during annotation processing.
 * Manages the state of external mapper generation and prevents duplicate processing.
 */
public class ExternalMappingRegistry {
    private final Map<String, ExternalMappingConfig> externalConfigs = new HashMap<>();
    private final Map<String, Boolean> generatedExternalMappers = new HashMap<>();

    /**
     * Registers an external mapping configuration.
     * 
     * @param targetClassName the qualified name of the target class
     * @param config the external mapping configuration
     */
    public void registerExternalConfig(String targetClassName, ExternalMappingConfig config) {
        externalConfigs.put(targetClassName, config);
    }

    /**
     * Gets an external mapping configuration for a target class.
     * 
     * @param targetClassName the qualified name of the target class
     * @return the external mapping configuration, or null if not found
     */
    public ExternalMappingConfig getExternalConfig(String targetClassName) {
        return externalConfigs.get(targetClassName);
    }

    /**
     * Checks if an external mapping configuration exists for a target class.
     * 
     * @param targetClassName the qualified name of the target class
     * @return true if configuration exists
     */
    public boolean hasExternalConfig(String targetClassName) {
        return externalConfigs.containsKey(targetClassName);
    }

    /**
     * Gets all registered external mapping configurations.
     * 
     * @return map of target class names to configurations
     */
    public Map<String, ExternalMappingConfig> getAllExternalConfigs() {
        return new HashMap<>(externalConfigs);
    }

    /**
     * Marks an external mapper as generated.
     * 
     * @param targetClassName the qualified name of the target class
     */
    public void markExternalMapperGenerated(String targetClassName) {
        generatedExternalMappers.put(targetClassName, true);
    }

    /**
     * Checks if an external mapper has been generated for a target class.
     * 
     * @param targetClassName the qualified name of the target class
     * @return true if mapper has been generated
     */
    public boolean isExternalMapperGenerated(String targetClassName) {
        return generatedExternalMappers.getOrDefault(targetClassName, false);
    }

    /**
     * Gets all target class names that have external configurations.
     * 
     * @return set of target class names
     */
    public Set<String> getAllExternalTargetClasses() {
        return externalConfigs.keySet();
    }

    /**
     * Checks if a type mirror represents an external object with configuration.
     * 
     * @param type the type to check
     * @return true if type has external configuration
     */
    public boolean isExternalType(TypeMirror type) {
        String typeName = getQualifiedTypeName(type);
        return hasExternalConfig(typeName);
    }

    /**
     * Gets the qualified name of a type.
     * 
     * @param type the type mirror
     * @return qualified type name
     */
    private String getQualifiedTypeName(TypeMirror type) {
        return type.toString();
    }

    /**
     * Clears all registrations (useful for testing).
     */
    public void clear() {
        externalConfigs.clear();
        generatedExternalMappers.clear();
    }
}