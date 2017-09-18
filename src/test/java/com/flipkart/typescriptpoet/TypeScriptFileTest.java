package com.flipkart.typescriptpoet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class TypeScriptFileTest {

    @Test
    public void simpleTypeScriptClassGeneration() throws Exception {
        String className = "ColorClass";
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className);

        FieldSpec redPropSpec = FieldSpec.builder(TypeName.STRING, "red").initializer("\"red\"").build();
        FieldSpec greenPropSpec = FieldSpec.builder(TypeName.STRING, "green").addModifiers(Modifier.PUBLIC).build();
        FieldSpec purplePropSpec = FieldSpec.builder(TypeName.INT, "purple").isOptional(true).build();

        List<FieldSpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(redPropSpec);
        propertySpecs.add(greenPropSpec);
        propertySpecs.add(purplePropSpec);

        for (FieldSpec spec : propertySpecs) {
            typeSpecBuilder.addField(spec);
        }

        ParameterSpec redParaSpec = ParameterSpec.builder(TypeName.STRING, "red").build();
        ParameterSpec greenParaSpec = ParameterSpec.builder(TypeName.STRING, "green").build();
        ParameterSpec blueParaSpec = ParameterSpec.builder(TypeName.STRING, "blue").build();
        ParameterSpec purpleParaSpec = ParameterSpec.builder(TypeName.INT, "purple").build();

        List<ParameterSpec> parameterSpecs = new ArrayList<>();
        parameterSpecs.add(redParaSpec);
        parameterSpecs.add(greenParaSpec);
        parameterSpecs.add(blueParaSpec);
        parameterSpecs.add(purpleParaSpec);

        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        MethodSpec.Builder initFunction = MethodSpec.methodBuilder("constructor").addJavadoc("//constructor");

        for (ParameterSpec parameterSpec : parameterSpecs) {
            codeBuilder.addStatement("this.$N = $N", parameterSpec, parameterSpec);
            initFunction.addParameter(parameterSpec);
        }

        initFunction.addCode(codeBuilder.build());
        typeSpecBuilder.addMethod(initFunction.build());

        MethodSpec.Builder classFunction = MethodSpec.methodBuilder("disp").addJavadoc("//function").returns(TypeName.VOID);
        codeBuilder = CodeBlock.builder();
        codeBuilder.addStatement("console.log(\"Engine is  :  \"+ this.engine)");

        classFunction.addCode(codeBuilder.build());
        typeSpecBuilder.addMethod(classFunction.build());


        classFunction = MethodSpec.methodBuilder("setDisp").addJavadoc("//function");
        codeBuilder = CodeBlock.builder();
        codeBuilder.addStatement("console.log(\"Engine is  :  \"+ this.engine)");

        classFunction.addCode(codeBuilder.build());
        typeSpecBuilder.addMethod(classFunction.build());

        TypeSpec typeSpec = typeSpecBuilder.build();
        File target = new File(TypeScriptFileTest.class.getName()).getParentFile();
        File generatedSources = new File(target, "generated-sources");

        TypeScriptFile.builder(typeSpec).build().writeTo(generatedSources);
    }

    @Test
    public void simpleTypeScriptInterfaceGeneration() throws Exception {
        String className = "ColorInterface";
        TypeSpec.Builder typeSpecBuilder = TypeSpec.interfaceBuilder(className);

        TypeSpec.Builder enumTypeSpecBuilder = TypeSpec.enumBuilder("color");

        FieldSpec greenPropSpec = FieldSpec.builder(TypeName.STRING, "green").addModifiers(Modifier.PUBLIC).build();
        typeSpecBuilder.addField(greenPropSpec);
        enumTypeSpecBuilder.addEnumConstant("Red").addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        enumTypeSpecBuilder.addEnumConstant("Green").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        typeSpecBuilder.addType(enumTypeSpecBuilder.build());

        TypeSpec typeSpec = typeSpecBuilder.build();

        File target = new File(TypeScriptFileTest.class.getName()).getParentFile();
        File generatedSources = new File(target, "generated-sources");

        TypeScriptFile.builder(typeSpec).build().writeTo(generatedSources);
    }

    @Test
    public void simpleTypeScriptInheritanceGeneration() throws Exception {
        File target = new File(TypeScriptFileTest.class.getName()).getParentFile();
        File generatedSources = new File(target, "generated-sources");

        String subClassName = "SubColorInheritance";
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(subClassName).superclass(ClassName.get("", "ColorInheritance"));
        TypeSpec typeSpec = typeSpecBuilder.build();
        TypeScriptFile.builder(typeSpec).build().writeTo(generatedSources);
    }
}
