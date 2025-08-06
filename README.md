# Map Converter

> **A high-performance Java annotation processor for automatic bidirectional object-to-Map conversion**

[![Java 17+](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/)
[![Maven Central](https://img.shields.io/badge/Maven-Central-green.svg)](https://search.maven.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

FastMapConverter eliminates boilerplate code by automatically generating high-performance, type-safe mapper classes that convert Java objects to `Map<String, Object>` and back. Perfect for API serialization, configuration management, and data transformation tasks.

## ✨ Core Features

### 🎯 Zero Runtime Reflection
- **Compile-time code generation** using annotation processing
- **Native performance** - generated code is as fast as hand-written conversion logic
- **Type-safe** conversions with compile-time validation

### 🔧 Flexible Field Mapping
- **Custom field naming** with `@MapField` annotation
- **Field exclusion** with `@MapIgnore` annotation
- **Comprehensive null safety** in generated code

### 📦 Advanced Collection Support
- **List, Set, Array** conversion with full type preservation
- **Collections of nested objects** - handle complex hierarchies
- **Null and empty collection** handling

### 🔗 Nested Object Mapping
- **Unlimited depth** nested object support
- **Circular reference protection** prevents infinite recursion
- **Automatic dependency resolution** for complex object graphs

### 🌐 External Object Mapping
- **Third-party class support** without source code modification
- **JPA entity mapping** for database objects
- **Library POJO support** for external dependencies

### 📅 Date/Time Handling
- **Comprehensive temporal type support** (LocalDateTime, Instant, etc.)
- **Flexible formatting strategies** (ISO, epoch, custom patterns)
- **Timezone conversion** capabilities

### 📋 Java Records Support
- **Full immutable record mapping** with zero configuration
- **Canonical constructor usage** for efficient object creation
- **Component accessor methods** (`name()` instead of `getName()`)
- **All annotations supported** on record components

## 🚀 Quick Start

### Basic Usage

```java
@MapperGenerate
public class User {
    private String name;
    private int age;
    private String email;
    
    // constructors, getters, setters...
}
```

// Generated mapper usage
User user = new User("John Doe", 30, "john@example.com");
Map<String, Object> userMap = UserMapper.toMap(user);
// Result: {"name": "John Doe", "age": 30, "email": "john@example.com"}

User restored = UserMapper.fromMap(userMap);
// Perfectly reconstructed User object
```

## 📋 Java Records Support

FastMapConverter fully supports **Java Records** with zero-configuration mapping. Records provide immutable data classes perfect for API responses, DTOs, and value objects.

### Basic Record Usage

```java
@MapperGenerate
public record PersonRecord(
    String name,
    int age,
    String email
) {}

// Generated usage
PersonRecord person = new PersonRecord("Jane Doe", 28, "jane@example.com");
Map<String, Object> personMap = PersonRecordMapper.toMap(person);
// Result: {"name": "Jane Doe", "age": 28, "email": "jane@example.com"}

PersonRecord restored = PersonRecordMapper.fromMap(personMap);
// Perfectly reconstructed using canonical constructor
```

### Advanced Record Features

Records support all the same annotations and features as regular classes:

```java
@MapperGenerate
public record UserRecord(
    String name,
    @MapField("user_email") String email,    // Custom field mapping
    @MapIgnore String internalId,            // Excluded from mapping
    List<String> roles,                      // Collection support
    AddressRecord address                    // Nested record support
) {}

@MapperGenerate
public record AddressRecord(
    String street,
    String city,
    String postalCode
) {}
```

### Record Collections and Nesting

```java
@MapperGenerate
public record TeamRecord(
    String name,
    List<PersonRecord> members,        // Collection of records
    Set<String> skills,                // Regular collections
    PersonRecord[] leads               // Array of records
) {}

// Full type preservation and nested mapping
TeamRecord team = new TeamRecord(
    "Engineering",
    List.of(person1, person2),
    Set.of("Java", "Kotlin"),
    new PersonRecord[]{lead1, lead2}
);

Map<String, Object> teamMap = TeamRecordMapper.toMap(team);
TeamRecord restoredTeam = TeamRecordMapper.fromMap(teamMap);
```

### Date/Time Records

Records work seamlessly with temporal type handling:

```java
@MapperGenerate
public record EventRecord(
    String title,
    @MapDateTime LocalDateTime createdAt,
    @MapDateTime(strategy = EPOCH_MILLIS) Instant updatedAt,
    @MapDateTime(pattern = "yyyy-MM-dd") LocalDate eventDate
) {}
```

### Record Benefits

| Feature | Records | Classes |
|---------|---------|---------|
| **Immutability** | ✅ Built-in | ❌ Manual |
| **Constructor** | ✅ Canonical | ❌ Multiple |
| **Accessors** | ✅ `name()` | ❌ `getName()` |
| **Boilerplate** | ✅ Minimal | ❌ Verbose |
| **Performance** | ✅ Optimized | ✅ Standard |
| **Null Safety** | ✅ Constructor validation | ❌ Setter validation |

**Why use Records with FastMapConverter?**
- **Immutable by design** - Perfect for DTOs and API responses
- **Constructor validation** - All fields validated at creation time
- **Reduced boilerplate** - No getters/setters needed
- **Type safety** - Compile-time guarantees for field access
- **Modern Java** - Embraces latest language features

## 🔧 Configuration Options

### Flexible Field Mapping
```java
@MapperGenerate
public class Product {
    private Long id;
    
    @MapField("product_name")    // Custom map key
    private String name;
    
    @MapIgnore                   // Skip field in conversion
    private String internalCode;
    
    private BigDecimal price;
}
```

### Collection Support
Automatic conversion of Java collections with full type preservation:

```java
@MapperGenerate
public class Order {
    private List<String> tags;           // List ↔ List
    private Set<String> categories;      // Set ↔ Set  
    private String[] colors;             // Array ↔ List
    
    // All collection types properly converted
}
```

**Supported Collections:**
- `List<T>` (ArrayList, LinkedList, etc.)
- `Set<T>` (HashSet, TreeSet, LinkedHashSet)
- `T[]` (any object array type)

## 🏢 Advanced Features

### 🔗 Nested Object Mapping
Handle complex object hierarchies automatically:

```java
@MapperGenerate
public class Customer {
    private String name;
    private Address homeAddress;         // Nested object
    private Address workAddress;         // Another nested object
    private List<Order> orders;          // Collection of nested objects
}

@MapperGenerate
public class Address {
    private String street;
    private String city;
    private String zipCode;
    private String country;
}

// Usage - all nested objects automatically handled
Customer customer = createComplexCustomer();
Map<String, Object> customerMap = CustomerMapper.toMap(customer);
Customer restored = CustomerMapper.fromMap(customerMap);
```

**Nested Features:**
- ✅ **Unlimited depth** - objects can be nested as deeply as needed
- ✅ **Collections of nested objects** - `List<Order>`, `Set<Product>`, etc.
- ✅ **Circular reference protection** - prevents infinite recursion
- ✅ **Automatic dependency resolution** - mappers generated in correct order

### 🔄 Circular Reference Handling
Robust protection against infinite recursion in complex object graphs:

```java
@MapperGenerate
public class User {
    private String name;
    private UserProfile profile;
}

@MapperGenerate 
public class UserProfile {
    private String bio;
    
    @MapIgnoreCircular  // Skip if circular reference detected
    private User user;  // Potential circular reference
}
```

**Circular Reference Strategies:**
- **Reference Tracking** - Uses visited set to prevent infinite loops
- **Max Depth Limiting** - Configurable depth limits
- **Selective Ignoring** - Skip specific fields during circular references

### 🌐 External Object Mapping
Map third-party classes without modifying their source code:

```java
// Third-party JPA entity (cannot modify)
@Entity
public class UserEntity {
    @Id private Long id;
    @Column(name = "username") private String username;
    @Column(name = "email") private String email;
}

// Configuration-based mapping
@ExternalMapper(targetClass = UserEntity.class)
public class UserEntityConfig {
    Long id;
    @ExternalField("user_name") String username;
    @ExternalField("user_email") String email;
}

// Generated mapper works identically
UserEntity user = userRepository.findById(1L);
Map<String, Object> userMap = UserEntityMapper.toMap(user);
```

**External Mapping Features:**
- ✅ **JPA Entity support** - map database entities without code changes
- ✅ **Library POJO support** - handle third-party objects seamlessly  
- ✅ **Custom field mapping** - flexible key naming for external objects
- ✅ **Nested external objects** - external objects can contain other external objects

### 📅 Date/Time Support
Comprehensive temporal type handling with flexible formatting:

```java
@MapperGenerate
public class Event {
    @MapDateTime                                    // ISO format
    private LocalDateTime createdAt;
    
    @MapDateTime(strategy = EPOCH_MILLIS)          // Timestamp format
    private Instant updatedAt;
    
    @MapDateTime(pattern = "yyyy-MM-dd")           // Custom format
    private LocalDate eventDate;
    
    @MapDateTime(
        strategy = CUSTOM_PATTERN,
        pattern = "yyyy-MM-dd HH:mm:ss",
        timezone = "UTC"
    )
    private ZonedDateTime timestamp;
}
```

**Supported Temporal Types:**
- `LocalDate`, `LocalDateTime`, `LocalTime`
- `ZonedDateTime`, `OffsetDateTime`, `Instant`
- `java.util.Date`, `java.sql.Date`, `java.sql.Timestamp`

**Date/Time Strategies:**
- `ISO_INSTANT` - Standard ISO 8601 format
- `EPOCH_MILLIS` - Unix timestamp (milliseconds)
- `EPOCH_SECONDS` - Unix timestamp (seconds)
- `CUSTOM_PATTERN` - User-defined format pattern
- `LOCALE_DEFAULT` - System default formatting

## 📈 Performance & Quality

### ⚡ Performance Characteristics
- **Zero reflection** - all conversion logic generated at compile time
- **Memory efficient** - minimal object allocation during conversion
- **Optimized iteration** - uses enhanced for-loops for collections
- **Type-safe casting** - no runtime type checking overhead

### 🛡️ Null Safety
Comprehensive null handling throughout the conversion process:

```java
// Generated code includes proper null checks
if (obj.getAddress() != null) {
    map.put("address", AddressMapper.toMap(obj.getAddress()));
}

// Null collections handled gracefully
if (obj.getTags() != null && !obj.getTags().isEmpty()) {
    List<Object> tagsList = new ArrayList<>();
    for (String item : obj.getTags()) {
        tagsList.add(item);
    }
    map.put("tags", tagsList);
}
```

### 🔍 Validation & Error Handling
- **Compile-time validation** - catch configuration errors early
- **Type compatibility checking** - ensure field types are convertible
- **Clear error messages** - detailed diagnostic information
- **Graceful degradation** - handle missing or invalid map keys

## 📚 Real-World Examples

### API Response Mapping
```java
@MapperGenerate
public class ApiResponse<T> {
    private String status;
    private T data;
    private String message;
    private LocalDateTime timestamp;
    
    @MapField("error_code")
    private String errorCode;
}

// Perfect for REST API serialization
ApiResponse<User> response = createApiResponse();
Map<String, Object> json = ApiResponseMapper.toMap(response);
```

### Configuration Management
```java
@MapperGenerate
public class DatabaseConfig {
    private String host;
    private int port;
    private String database;
    private ConnectionPool poolConfig;  // Nested configuration
    
    @MapIgnore
    private String password;  // Sensitive data excluded
}

// Load from properties/YAML into strongly-typed objects
Map<String, Object> configMap = loadFromYaml("database.yml");
DatabaseConfig config = DatabaseConfigMapper.fromMap(configMap);
```

### JPA Entity Serialization
```java
// Entity cannot be modified
@Entity
@Table(name = "products")
public class ProductEntity {
    @Id private Long id;
    @Column private String name;
    @Column private BigDecimal price;
    @ManyToOne private Category category;
}

// External mapping configuration
@ExternalMapper(targetClass = ProductEntity.class)
public class ProductEntityConfig {
    Long id;
    @ExternalField("product_name") String name;
    BigDecimal price;
    Category category;  // Nested external object
}

// Seamless entity conversion for APIs
ProductEntity entity = productRepository.findById(1L);
Map<String, Object> productJson = ProductEntityMapper.toMap(entity);
```

### Modern Java Records
```java
// Immutable DTOs with Records
@MapperGenerate
public record UserProfileRecord(
    String username,
    @MapField("display_name") String displayName,
    List<String> permissions,
    @MapDateTime LocalDateTime lastLogin,
    AddressRecord homeAddress
) {}

@MapperGenerate
public record AddressRecord(String street, String city, String zipCode) {}

// Zero boilerplate, maximum type safety
UserProfileRecord profile = new UserProfileRecord(
    "johndoe", "John Doe", List.of("read", "write"), 
    LocalDateTime.now(), new AddressRecord("123 Main St", "Springfield", "12345")
);

Map<String, Object> profileJson = UserProfileRecordMapper.toMap(profile);
// Perfect for modern API responses with immutable data
```

## 😧 Roadmap

### Current Status: **v1.1 - Enhanced** ✅
- ✅ Core annotation processing infrastructure
- ✅ Basic field mapping with custom naming
- ✅ Collection support (List, Set, Array)
- ✅ Nested object mapping with circular reference protection
- ✅ External object mapping for third-party classes
- ✅ Date/time handling with flexible formatting
- ✅ **Java Records Support** - Full immutable record mapping with canonical constructors

### Upcoming Features: **v1.2** 🔄
- 🟦 **Custom Converters** - User-defined type conversion logic
- 🟦 **Builder Pattern Support** - Generate builder-style mappers
- 🟦 **Map Collections** - Support for `Map<K,V>` field types
- 🟦 **Performance Optimizations** - Further speed improvements

### Future Vision: **v2.0** 🌟
- 🟦 **Plugin Architecture** - Extensible converter framework
- 🟦 **IDE Integration** - Enhanced development-time support
- 🟦 **Validation Integration** - Built-in Bean Validation support
- 🟦 **Schema Generation** - JSON Schema generation from mappers

## 🏆 Why FastMapConverter?

| Feature | FastMapConverter | Manual Code | MapStruct | Jackson |
|---------|------------------|-------------|-----------|----------|
| **Performance** | ⚡ Zero reflection | ⚡ Native speed | ⚡ Code generation | 🐌 Reflection |
| **Type Safety** | ✅ Compile-time | ✅ Compile-time | ✅ Compile-time | ❌ Runtime |
| **Nested Objects** | ✅ Automatic | ❌ Manual coding | ✅ Manual config | ✅ Automatic |
| **Collections** | ✅ Full support | ❌ Manual coding | ✅ Manual config | ✅ Automatic |
| **External Objects** | ✅ Configuration | ❌ Not possible | ❌ Limited | ✅ Annotations |
| **Java Records** | ✅ Full support | ❌ Manual coding | ❌ Not supported | ❌ Limited |
| **Setup Complexity** | 🟢 Minimal | 🔴 High maintenance | 🟡 Moderate | 🟢 Minimal |
| **Learning Curve** | 🟢 Simple annotations | 🔴 Complex coding | 🟡 DSL learning | 🟢 Familiar |

**FastMapConverter** combines the best of all worlds: the performance of generated code, the simplicity of annotations, and the flexibility to handle any object structure.

## 🤝 Contributing

We welcome contributions! See our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup
```bash
git clone https://github.com/your-org/fast-map-converter.git
cd fast-map-converter
mvn clean compile test
```

### Architecture Overview
- **Annotations** (`com.mapconverter.annotations`) - Core annotations for configuration
- **Processor** (`com.mapconverter.processor`) - Annotation processing logic
- **Generator** (`com.mapconverter.generator`) - Code generation using JavaPoet
- **Converter** (`com.mapconverter.converter`) - Utility converters for special types

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Made with ❤️ for the Java community**

[⭐ Star this repo](https://github.com/your-org/fast-map-converter) • [🐛 Report issues](https://github.com/your-org/fast-map-converter/issues) • [💡 Request features](https://github.com/your-org/fast-map-converter/discussions)

</div>