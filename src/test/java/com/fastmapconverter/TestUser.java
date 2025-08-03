package com.fastmapconverter;

import com.fastmapconverter.annotations.MapField;
import com.fastmapconverter.annotations.MapIgnore;
import com.fastmapconverter.annotations.MapperGenerate;

@MapperGenerate
public class TestUser {
    private String name;
    private int age;
    
    @MapField("user_email")
    private String email;
    
    @MapIgnore
    private String password;
    
    public TestUser() {}
    
    public TestUser(String name, int age, String email, String password) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}