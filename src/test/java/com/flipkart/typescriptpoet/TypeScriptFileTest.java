package test.java.com.flipkart.typescriptpoet;

import main.java.com.flipkart.typescriptpoet.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class TypeScriptFileTest {

    @Test
    public void swiftFileColorTest() throws Exception {
        String className = "Color";
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className);

        FieldSpec redPropSpec = FieldSpec.builder(TypeName.STRING, "red").build();
        FieldSpec greenPropSpec = FieldSpec.builder(TypeName.STRING, "green").build();
        FieldSpec bluePropSpec = FieldSpec.builder(TypeName.STRING, "blue").build();

        List<FieldSpec> propertySpecs = new ArrayList<>();
        propertySpecs.add(redPropSpec);
        propertySpecs.add(greenPropSpec);
        propertySpecs.add(bluePropSpec);

        for (FieldSpec spec : propertySpecs) {
            typeSpecBuilder.addField(spec);
        }

        TypeSpec typeSpec = typeSpecBuilder.build();
        File target = new File(TypeScriptFileTest.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
        File generatedSources = new File(target, "generated-sources");

        TypeScriptFile.builder(this.getClass().getPackage().getName(), typeSpec).build().writeTo(generatedSources);
    }
}
