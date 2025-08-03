# Java Object-to-Map Converter Tool Development Prompt

## 🎯 Project Status: COMPLETED ✅

**Implementation Status**: All core features have been successfully implemented!

## Project Overview
Create a Java annotation processing tool that generates code for bidirectional conversion between Java objects and `Map<String, Object>`. The tool should work similarly to MapStruct and Lombok, using annotations and compile-time code generation.

**✅ COMPLETED**: Full working implementation with all core features delivered.

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
- ✅ **Type Handling**: Handle primitives, objects (collections/nested objects in roadmap)
- ✅ **Null Safety**: Proper null checking in generated code
- ✅ **Validation**: Type compatibility validation during generation

### 3. Advanced Features to Consider 🚧 ROADMAP
- 🔲 **Nested Object Support**: Handle complex object hierarchies
- 🔲 **Collection Handling**: Support for List, Set, Array conversions
- 🔲 **Date/Time Handling**: Special handling for temporal types
- 🔲 **Custom Converters**: Allow user-defined type converters
- 🔲 **Builder Pattern**: Generate builder-style mappers if needed

## Architecture Components

### 1. Core Annotations
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
```

### 2. Annotation Processor Structure ✅ COMPLETED
- ✅ **Processor Entry Point**: Main AbstractProcessor implementation
- ✅ **Model Analysis**: Analyze annotated classes and extract metadata
- ✅ **Code Generator**: Generate mapper classes with conversion methods
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

## Development Approach ✅ PHASES 1-2 COMPLETED

1. ✅ **Phase 1**: Basic annotation processor with simple field mapping - **COMPLETED**
2. ✅ **Phase 2**: Add custom field naming and ignore functionality - **COMPLETED**
3. 🔲 **Phase 3**: Implement nested object and collection support - **ROADMAP**
4. 🔲 **Phase 4**: Add advanced features (custom converters, validation) - **ROADMAP**
5. 🔲 **Phase 5**: Optimize performance and add comprehensive testing - **ROADMAP**

## Success Criteria ✅ CORE FEATURES ACHIEVED

- ✅ Zero runtime reflection - all conversion logic generated at compile time
- 🔲 Support for complex object hierarchies and collections (roadmap)
- ✅ Easy integration with existing build systems (Maven/Gradle)
- ✅ Clear error messages during compilation for invalid configurations
- ✅ Generated code should be readable and debuggable
- ✅ Performance comparable to hand-written conversion code

## Additional Considerations ✅ DELIVERED

- ✅ **Documentation**: Comprehensive JavaDoc and usage examples
- ✅ **Error Messages**: Clear compilation errors for misconfigured annotations
- 🔲 **Extensibility**: Plugin architecture for custom type handlers (roadmap)
- ✅ **Compatibility**: Support for different Java versions (11+)
- ✅ **Testing Framework**: Utilities for testing generated mappers

## 🎉 Implementation Summary

**COMPLETED FEATURES:**
- ✅ Complete annotation processing infrastructure
- ✅ Core annotations (@MapperGenerate, @MapField, @MapIgnore)
- ✅ JavaPoet-based code generation
- ✅ Maven build integration with Auto-Service
- ✅ Comprehensive testing framework
- ✅ Full documentation and examples
- ✅ Null-safe bidirectional conversion (toMap/fromMap)
- ✅ Custom field naming and field exclusion
- ✅ Primitive and object type support

**PROJECT STATUS:** Core implementation complete and ready for use! Advanced features (collections, nested objects) can be added as future enhancements.