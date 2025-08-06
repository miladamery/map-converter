# Java Object-to-Map Converter Tool Development Prompt

## 🎯 Project Status: PRODUCTION-READY v0.0.1 ✅

**Implementation Status**: All major features have been successfully implemented and exceed the original roadmap scope!

## Project Overview
FastMapConverter is a high-performance Java annotation processor that generates code for bidirectional conversion between Java objects and `Map<String, Object>`. The tool works similarly to MapStruct and Lombok, using annotations and compile-time code generation.

**✅ COMPLETED**: Full production-ready implementation with comprehensive feature set including external object mapping, date/time handling, Java Records support, and advanced nested object capabilities.

## Core Requirements

### 1. Annotation-Based Configuration
```java
@MapperGenerate
public class User {
    private String name;
    private int age;
    private List<String> hobbies;
    
    // Optional: Custom field mapping
    @MapField("user_email")
    private String email;
    
    // Optional: Ignore field
    @MapIgnore
    private String password;
}
```

### 2. Generated Code Functionality
The tool should generate a mapper class with methods like:
```java
public class UserMapper {
    public static Map<String, Object> toMap(User user) { /* generated */ }
    public static User fromMap(Map<String, Object> map) { /* generated */ }
}
```

## Technical Implementation Requirements

### 1. Annotation Processor Setup ✅ COMPLETED
- ✅ Create custom annotations (@MapperGenerate, @MapField, @MapIgnore)
- ✅ Implement AbstractProcessor for compile-time code generation
- ✅ Handle annotation discovery and validation
- ✅ Generate source code files during compilation

### 2. Code Generation Features ✅ COMPLETED  
- ✅ **Field Mapping**: Automatic field-to-key mapping using field names
- ✅ **Custom Mapping**: Support @MapField annotation for custom key names
- ✅ **Type Handling**: Handle primitives, objects, collections, and nested objects
- ✅ **Null Safety**: Proper null checking in generated code
- ✅ **Validation**: Type compatibility validation during generation

### 3. Advanced Features ✅ COMPLETED
- ✅ **Nested Object Support**: Handle complex object hierarchies with circular reference protection
- ✅ **Collection Handling**: Support for List, Set, Array conversions including nested objects
- ✅ **Circular Reference Detection**: Topological sorting and reference tracking
- ✅ **Dependency Management**: Automatic mapper generation order resolution
- ✅ **External Object Mapping**: Support objects without source code access via @ExternalMapper
- ✅ **Date/Time Handling**: Comprehensive temporal type support with @MapDateTime
- ✅ **Java Records Support**: Full immutable record mapping with canonical constructors
- 🔲 **Custom Converters**: Allow user-defined type converters - **ROADMAP**
- 🔲 **Builder Pattern**: Generate builder-style mappers if needed - **ROADMAP**

## Architecture Components

### 1. Core Annotations ✅ ENHANCED
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MapperGenerate {
    String className() default ""; // Optional custom mapper class name
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapField {
    String value(); // Custom map key name
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapIgnore {
}

// Advanced Configuration Annotations
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapNested {
    int depth() default -1; // Max nesting depth
    CircularRefStrategy strategy() default REFERENCE_TRACKING;
    boolean generateNestedMappers() default true;
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapIgnoreCircular {
    // Skip field if circular reference detected
}

// External Object Mapping
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ExternalMapper {
    Class<?> targetClass();
    String mapperName() default "";
    String packageName() default "";
    boolean generateNestedMappers() default true;
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ExternalField {
    String value() default "";
    boolean ignore() default false;
    Class<?> converter() default Void.class;
}

// Date/Time Handling
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapDateTime {
    String pattern() default "";
    String timezone() default "";
    DateTimeStrategy strategy() default ISO_INSTANT;
    boolean preserveNanos() default false;
    boolean lenientParsing() default true;
}
```

### 2. Annotation Processor Structure ✅ ENHANCED
- ✅ **Processor Entry Point**: Main AbstractProcessor implementation
- ✅ **Model Analysis**: Analyze annotated classes and extract metadata with nested object detection
- ✅ **Dependency Resolution**: Topological sorting for proper mapper generation order
- ✅ **MapperRegistry**: Track generation state and prevent duplicate processing
- ✅ **Code Generator**: Generate mapper classes with conversion methods and circular reference protection
- ✅ **File Writer**: Write generated source files to appropriate locations

### 3. Build Integration ✅ COMPLETED
- ✅ Maven/Gradle plugin configuration
- ✅ Proper annotation processor registration
- ✅ Integration with IDE for development-time generation

## Expected Usage Example

### Input Class
```java
@MapperGenerate
public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    @MapField("category_name")
    private String category;
    @MapIgnore
    private String internalCode;
    private List<String> tags;
}
```

### Generated Mapper Usage
```java
Product product = new Product(1L, "Laptop", new BigDecimal("999.99"), "Electronics", "INTERNAL", Arrays.asList("tech", "computer"));

// Convert to Map
Map<String, Object> productMap = ProductMapper.toMap(product);
// Result: {"id": 1, "name": "Laptop", "price": 999.99, "category_name": "Electronics", "tags": ["tech", "computer"]}

// Convert back to Object
Product reconstructed = ProductMapper.fromMap(productMap);
```

## Technical Challenges to Address

1. **Reflection vs Code Generation**: Ensure compile-time generation for performance
2. **Type Safety**: Maintain type safety in generated conversion code
3. **Error Handling**: Graceful handling of missing/invalid map keys
4. **Performance**: Generate efficient conversion code
5. **IDE Integration**: Ensure generated code is recognized by IDEs
6. **Testing**: Framework for testing generated mappers

## Development Approach ✅ PHASES 1-3 COMPLETED

1. ✅ **Phase 1**: Basic annotation processor with simple field mapping - **COMPLETED**
2. ✅ **Phase 2**: Add custom field naming and ignore functionality - **COMPLETED**
3. ✅ **Phase 3**: Implement nested object and collection support - **COMPLETED**
4. ✅ **Phase 4**: Add external object mapping and date/time handling - **COMPLETED**
5. ✅ **Phase 5**: Java Records support and advanced testing - **COMPLETED**
6. 🔲 **Phase 6**: Custom converters and builder pattern - **ROADMAP**
7. 🔲 **Phase 7**: Performance optimization and extensibility - **ROADMAP**

## Success Criteria ✅ ALL MAJOR FEATURES ACHIEVED

- ✅ Zero runtime reflection - all conversion logic generated at compile time
- ✅ Support for complex object hierarchies and collections with circular reference protection
- ✅ Easy integration with existing build systems (Maven/Gradle)
- ✅ Clear error messages during compilation for invalid configurations
- ✅ Generated code should be readable and debuggable
- ✅ Performance comparable to hand-written conversion code
- ✅ Dependency resolution with topological sorting
- ✅ Advanced configuration options for nested object handling

## Additional Considerations ✅ ENHANCED

- ✅ **Documentation**: Comprehensive JavaDoc and usage examples
- ✅ **Error Messages**: Clear compilation errors for misconfigured annotations
- ✅ **Circular Reference Handling**: Multiple strategies (reference tracking, max depth, lazy reference)
- ✅ **Advanced Annotations**: @MapNested, @MapIgnoreCircular, @ExternalMapper, @MapDateTime for fine-grained control
- ✅ **Java Records**: Full support for immutable records with component accessors
- 🔲 **Extensibility**: Plugin architecture for custom type handlers - **ROADMAP**
- ✅ **Compatibility**: Support for Java 17+
- ✅ **Testing Framework**: Utilities for testing generated mappers

## 🎉 Implementation Summary

**COMPLETED FEATURES:**
- ✅ Complete annotation processing infrastructure with dependency resolution
- ✅ Core annotations (@MapperGenerate, @MapField, @MapIgnore)
- ✅ Advanced annotations (@MapNested, @MapIgnoreCircular)
- ✅ JavaPoet-based code generation with circular reference protection
- ✅ Maven build integration with Auto-Service
- ✅ Comprehensive testing framework for nested objects
- ✅ Full documentation and examples
- ✅ Null-safe bidirectional conversion (toMap/fromMap)
- ✅ Custom field naming and field exclusion
- ✅ Primitive, object, collection, and nested object type support
- ✅ Collections of nested objects (List<CustomObject>, Set<CustomObject>, CustomObject[])
- ✅ Circular reference detection and handling with multiple strategies
- ✅ Topological sorting for dependency resolution
- ✅ MapperRegistry for generation state management
- ✅ Enhanced field type classification (FieldType enum)
- ✅ Package-private method visibility for inter-mapper communication
- ✅ **External Object Mapping**: Support mapping objects from third-party libraries without source code access
    - Configuration-based mapping for external classes (e.g., library POJOs, DTOs, JPA entities)
    - @ExternalMapper annotation for defining mappings without modifying source code
    - @ExternalField for custom field mapping and conversion logic
    - Support for objects where @MapperGenerate cannot be added
- ✅ **Date/Time Support**: Comprehensive temporal type handling with flexible formatting
    - @MapDateTime annotation with multiple strategies (ISO, EPOCH, CUSTOM_PATTERN)
    - Support for java.time.* and legacy java.util.Date family
    - Timezone conversion capabilities with preservation of nanosecond precision
- ✅ **Java Records Support**: Full immutable record mapping with zero configuration
    - Canonical constructor usage for efficient object creation
    - Component accessor methods (name() instead of getName())
    - All standard annotations supported on record components 
**NESTED OBJECT FEATURES:**
- ✅ **Simple Nested Objects**: A → B mapping with automatic mapper generation
- ✅ **Deep Nesting**: A → B → C → D with unlimited depth support
- ✅ **Collections of Nested Objects**: List<Order>, Set<Address>, Order[]
- ✅ **Circular Reference Protection**: User ↔ Profile, Parent ↔ Child relationships
- ✅ **Reference Tracking**: Visited set prevents infinite recursion
- ✅ **Dependency Management**: Mappers generated in correct order
- ✅ **Configuration Options**: @MapNested for fine-grained control

**ADDITIONAL IMPLEMENTED FEATURES:**
- ✅ **Date/Time Handling**: Comprehensive temporal type support with @MapDateTime
    - Multiple conversion strategies (ISO_INSTANT, EPOCH_MILLIS, EPOCH_SECONDS, CUSTOM_PATTERN, AUTO)
    - Timezone conversion and nanosecond precision control
    - Support for all java.time.* and legacy Date types
- ✅ **External Object Mapping**: Third-party class support without source modification
    - @ExternalMapper and @ExternalField annotations
    - JPA entity mapping, library POJO support
    - Configuration-based field mapping with validation
- ✅ **Java Records Support**: Modern immutable data class mapping
    - Zero-configuration record component mapping
    - Canonical constructor usage with type safety
    - Component accessor method generation

**REMAINING ROADMAP FEATURES (Future Releases):**
- 🔲 **Custom Converters**: User-defined type converters for specialized mapping logic
- 🔲 **Builder Pattern**: Generate builder-style mappers for fluent API design
- 🔲 **Map Collections**: Support for Map<K,V> field types
- 🔲 **Extensibility**: Plugin architecture for custom type handlers
- 🔲 **Performance Profiling**: Advanced benchmarking and optimization tools

**PROJECT STATUS:** Production-ready v0.0.1 with feature completeness exceeding original scope! The framework supports complex enterprise use cases including external object mapping, comprehensive date/time handling, Java Records, and advanced nested object hierarchies with circular reference protection.