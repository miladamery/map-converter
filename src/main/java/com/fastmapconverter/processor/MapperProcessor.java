package com.fastmapconverter.processor;

import com.fastmapconverter.annotations.MapField;
import com.fastmapconverter.annotations.MapIgnore;
import com.fastmapconverter.annotations.MapperGenerate;
import com.fastmapconverter.generator.MapperCodeGenerator;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Annotation processor for generating mapper classes from @MapperGenerate annotations.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.fastmapconverter.annotations.MapperGenerate")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class MapperProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            
            for (Element element : annotatedElements) {
                if (element.getKind() != ElementKind.CLASS) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@MapperGenerate can only be applied to classes",
                        element
                    );
                    continue;
                }
                
                TypeElement classElement = (TypeElement) element;
                try {
                    processClass(classElement);
                } catch (IOException e) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate mapper for " + classElement.getSimpleName() + ": " + e.getMessage(),
                        classElement
                    );
                }
            }
        }
        
        return true;
    }

    private void processClass(TypeElement classElement) throws IOException {
        MapperGenerate annotation = classElement.getAnnotation(MapperGenerate.class);
        String mapperClassName = annotation.className().isEmpty() 
            ? classElement.getSimpleName() + "Mapper"
            : annotation.className();

        List<FieldInfo> fields = analyzeFields(classElement);
        
        MapperCodeGenerator generator = new MapperCodeGenerator(
            processingEnv.getFiler(),
            processingEnv.getElementUtils(),
            processingEnv.getTypeUtils()
        );
        
        generator.generateMapper(classElement, mapperClassName, fields);
    }

    private List<FieldInfo> analyzeFields(TypeElement classElement) {
        List<FieldInfo> fields = new ArrayList<>();
        
        for (Element enclosedElement : classElement.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.FIELD) {
                continue;
            }
            
            VariableElement fieldElement = (VariableElement) enclosedElement;
            
            // Skip static and final fields
            if (fieldElement.getModifiers().contains(Modifier.STATIC) ||
                fieldElement.getModifiers().contains(Modifier.FINAL)) {
                continue;
            }
            
            String fieldName = fieldElement.getSimpleName().toString();
            boolean ignored = fieldElement.getAnnotation(MapIgnore.class) != null;
            
            String mapKey = fieldName;
            MapField mapFieldAnnotation = fieldElement.getAnnotation(MapField.class);
            if (mapFieldAnnotation != null) {
                mapKey = mapFieldAnnotation.value();
            }
            
            FieldInfo fieldInfo = new FieldInfo(
                fieldName,
                mapKey,
                fieldElement.asType(),
                ignored,
                fieldElement
            );
            
            fields.add(fieldInfo);
        }
        
        return fields;
    }
}