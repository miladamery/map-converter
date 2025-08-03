package com.fastmapconverter;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

/**
 * Tests for the MapperProcessor annotation processor.
 */
public class MapperProcessorTest {


    /*@Test
    public void testBasicMapperGeneration() {
        JavaFileObject testClass = JavaFileObjects.forSourceString("com.example.Person", """
            package com.example;
            
            import com.fastmapconverter.annotations.MapperGenerate;
            
            @MapperGenerate
            public class Person {
                private String name;
                private int age;
                
                public Person() {}
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public int getAge() { return age; }
                public void setAge(int age) { this.age = age; }
            }
            """);

        Compilation compilation = javac()
            .withProcessors(new com.fastmapconverter.processor.MapperProcessor())
            .compile(testClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("com.example.PersonMapper");
    }

    @Test
    public void testMapFieldAnnotation() {
        JavaFileObject testClass = JavaFileObjects.forSourceString("com.example.User", """
            package com.example;
            
            import com.fastmapconverter.annotations.MapperGenerate;
            import com.fastmapconverter.annotations.MapField;
            
            @MapperGenerate
            public class User {
                private String name;
                
                @MapField("user_email")
                private String email;
                
                public User() {}
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public String getEmail() { return email; }
                public void setEmail(String email) { this.email = email; }
            }
            """);

        Compilation compilation = javac()
            .withProcessors(new com.fastmapconverter.processor.MapperProcessor())
            .compile(testClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("com.example.UserMapper");
        
        // Check that the generated code contains the custom map key
        assertThat(compilation)
            .generatedSourceFile("com.example.UserMapper")
            .contentsAsUtf8String()
            .contains("user_email");
    }

    @Test
    public void testMapIgnoreAnnotation() {
        JavaFileObject testClass = JavaFileObjects.forSourceString("com.example.Account", """
            package com.example;
            
            import com.fastmapconverter.annotations.MapperGenerate;
            import com.fastmapconverter.annotations.MapIgnore;
            
            @MapperGenerate
            public class Account {
                private String username;
                
                @MapIgnore
                private String password;
                
                public Account() {}
                
                public String getUsername() { return username; }
                public void setUsername(String username) { this.username = username; }
                public String getPassword() { return password; }
                public void setPassword(String password) { this.password = password; }
            }
            """);

        Compilation compilation = javac()
            .withProcessors(new com.fastmapconverter.processor.MapperProcessor())
            .compile(testClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("com.example.AccountMapper");
        
        // Check that the generated code does NOT contain password mapping
        String generatedCode = compilation
            .generatedSourceFile("com.example.AccountMapper")
            .contentsAsUtf8String();
        
        assert !generatedCode.contains("getPassword()");
        assert !generatedCode.contains("setPassword");
    }

    @Test
    public void testCustomMapperClassName() {
        JavaFileObject testClass = JavaFileObjects.forSourceString("com.example.Product", """
            package com.example;
            
            import com.fastmapconverter.annotations.MapperGenerate;
            
            @MapperGenerate(className = "CustomProductMapper")
            public class Product {
                private String name;
                private double price;
                
                public Product() {}
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public double getPrice() { return price; }
                public void setPrice(double price) { this.price = price; }
            }
            """);

        Compilation compilation = javac()
            .withProcessors(new com.fastmapconverter.processor.MapperProcessor())
            .compile(testClass);

        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("com.example.CustomProductMapper");
    }*/
}