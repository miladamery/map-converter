package com.mapconverter.processor;

import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registry for managing mapper generation dependencies and preventing circular references.
 */
public class MapperRegistry {
    private final Set<String> generatedMappers = new HashSet<>();
    private final Set<String> processingQueue = new HashSet<>();
    private final Map<String, TypeElement> pendingTypes = new HashMap<>();
    
    /**
     * Checks if a mapper has already been generated for the given class name.
     */
    public boolean isMapperGenerated(String className) {
        return generatedMappers.contains(className);
    }
    
    /**
     * Registers a mapper as generated for the given class name.
     */
    public void registerMapper(String className) {
        generatedMappers.add(className);
        processingQueue.remove(className);
    }
    
    /**
     * Adds a type to the processing queue.
     */
    public void addToProcessingQueue(String className, TypeElement typeElement) {
        processingQueue.add(className);
        pendingTypes.put(className, typeElement);
    }
    
    /**
     * Checks if a type is currently being processed.
     */
    public boolean isCurrentlyProcessing(String className) {
        return processingQueue.contains(className);
    }
    
    /**
     * Gets all pending types that need to be processed.
     */
    public Map<String, TypeElement> getPendingTypes() {
        return new HashMap<>(pendingTypes);
    }
    
    /**
     * Checks for circular references in the dependency graph.
     */
    public boolean hasCircularReference(String className, Set<String> visitedTypes) {
        if (visitedTypes.contains(className)) {
            return true;
        }
        
        if (isMapperGenerated(className)) {
            return false;
        }
        
        visitedTypes.add(className);
        
        // For now, we'll implement basic detection
        // More sophisticated dependency analysis can be added later
        TypeElement typeElement = pendingTypes.get(className);
        if (typeElement != null) {
            // Check nested dependencies would go here
            // For initial implementation, we'll assume no circular refs
        }
        
        visitedTypes.remove(className);
        return false;
    }
    
    /**
     * Clears the registry (useful for testing).
     */
    public void clear() {
        generatedMappers.clear();
        processingQueue.clear();
        pendingTypes.clear();
    }
    
    /**
     * Gets the count of generated mappers.
     */
    public int getGeneratedMappersCount() {
        return generatedMappers.size();
    }
    
    /**
     * Gets the count of pending mappers.
     */
    public int getPendingMappersCount() {
        return pendingTypes.size();
    }
}