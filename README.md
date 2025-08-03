# Fast Map Converter

A Java annotation processing tool that generates code for bidirectional conversion between Java objects and `Map<String, Object>`. Similar to MapStruct and Lombok, it uses annotations and compile-time code generation for zero-runtime overhead.

## Features

- **Compile-time Code Generation**: Zero runtime reflection for maximum performance
- **Bidirectional Conversion**: Generate both `toMap()` and `fromMap()` methods
- **Custom Field Mapping**: Use `@MapField` to specify custom map keys
- **Field Exclusion**: Use `@MapIgnore` to exclude sensitive fields
- **Null Safety**: Proper null handling in generated code
- **Type Safety**: Maintains compile-time type safety

## Quick Start

### 1. Add Annotations to Your Class

```java
@MapperGenerate
public class User {
    private String name;
    private int age;
    
    @MapField("user_email")  // Custom map key
    private String email;
    
    @MapIgnore              // Exclude from mapping
    private String password;
    
    // Standard getters and setters...
}
```

### 2. Use the Generated Mapper

```java
User user = new User("John", 30, "john@example.com", "secret");

// Convert to Map
Map<String, Object> userMap = UserMapper.toMap(user);
// Result: {"name": "John", "age": 30, "user_email": "john@example.com"}

// Convert back to Object
User reconstructed = UserMapper.fromMap(userMap);
```

## Available Annotations

### @MapperGenerate
```java
@MapperGenerate(className = "CustomMapperName")  // Optional custom name
public class MyClass { ... }
```

### @MapField
```java
@MapField("custom_key_name")
private String myField;
```

### @MapIgnore
```java
@MapIgnore
private String sensitiveField;
```

## Maven Setup

Add to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>com.fastmapconverter</groupId>
        <artifactId>fast-map-converter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>11</source>
                <target>11</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## How It Works

1. **Annotation Processing**: During compilation, the annotation processor scans for `@MapperGenerate` annotations
2. **Code Analysis**: Analyzes the class fields, respecting `@MapField` and `@MapIgnore` annotations
3. **Code Generation**: Uses JavaPoet to generate mapper classes with static methods
4. **Integration**: Generated classes are automatically available in your classpath

## Generated Code Example

For the annotated `DemoUser` class, the processor generates:

```java
public final class DemoUserMapper {
    public static Map<String, Object> toMap(DemoUser obj) {
        if (obj == null) return null;
        Map<String, Object> map = new HashMap<>();
        
        if (obj.getName() != null) {
            map.put("name", obj.getName());
        }
        map.put("age", obj.getAge());
        if (obj.getEmail() != null) {
            map.put("user_email", obj.getEmail());  // Custom key from @MapField
        }
        // password field omitted due to @MapIgnore
        
        return map;
    }
    
    public static DemoUser fromMap(Map<String, Object> map) {
        if (map == null) return null;
        DemoUser obj = new DemoUser();
        
        if (map.containsKey("name")) {
            Object value = map.get("name");
            if (value != null) {
                obj.setName((String) value);
            }
        }
        // ... similar for other fields
        
        return obj;
    }
}
```

## Building the Project

```bash
mvn clean compile    # Compiles and runs annotation processor
mvn test            # Runs tests including annotation processor tests
mvn package         # Creates JAR file
```

## Project Structure

```
src/
├── main/java/com/fastmapconverter/
│   ├── annotations/          # Core annotations
│   ├── processor/           # Annotation processor
│   └── generator/          # Code generation logic
├── test/java/              # Test classes
└── example/               # Usage examples
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Current Limitations

- Requires standard getter/setter methods
- Basic type support (primitives, strings, objects)
- No nested object support yet (planned for future versions)

## Roadmap

- [ ] Nested object support
- [ ] Collection handling (List, Set, Array)
- [ ] Custom type converters
- [ ] Date/Time handling
- [ ] Builder pattern support
- [ ] Gradle plugin

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## License

This project is licensed under the MIT License.