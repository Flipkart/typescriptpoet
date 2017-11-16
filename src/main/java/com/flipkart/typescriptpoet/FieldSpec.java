package com.flipkart.typescriptpoet;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.flipkart.typescriptpoet.Util.*;

/**
 * A generated field declaration.
 */
public final class FieldSpec {
    public final TypeName type;
    public final String name;
    public final List<AnnotationSpec> annotations;
    private final CodeBlock typescriptDoc;
    private final Set<Modifier> modifiers;
    private final CodeBlock initializer;
    private final boolean isMutable, isOptional;

    private FieldSpec(Builder builder) {
        this.type = checkNotNull(builder.type, "type == null");
        this.name = checkNotNull(builder.name, "name == null");
        this.typescriptDoc = builder.typescriptDoc.build();
        this.annotations = Util.immutableList(builder.annotations);
        this.modifiers = Util.immutableSet(builder.modifiers);
        this.initializer = (builder.initializer == null)
                ? CodeBlock.builder().build()
                : builder.initializer;
        this.isMutable = builder.isMutable;
        this.isOptional = builder.isOptional;
    }

    public static Builder builder(TypeName type, String name, Modifier... modifiers) {
        checkNotNull(type, "type == null");
        checkArgument(SourceVersion.isName(name), "not a valid name: %s", name);
        return new Builder(type, name)
                .addModifiers(modifiers);
    }

    public static Builder builder(Type type, String name, Modifier... modifiers) {
        return builder(TypeName.get(type), name, modifiers);
    }

    boolean hasModifier(com.flipkart.typescriptpoet.Modifier modifier) {
        return modifiers.contains(modifier);
    }

    void emit(CodeWriter codeWriter, Set<Modifier> implicitModifiers) throws IOException {
        codeWriter.emitJavadoc(typescriptDoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emitModifiers(modifiers, implicitModifiers);
        String codeArg = !isMutable ? "const $L" : "$L";
        codeArg = isOptional ? codeArg + "?" : codeArg;
        codeWriter.emit(codeArg + ": $T", name, type);
        if (!initializer.isEmpty()) {
            codeWriter.emit(" = ");
            codeWriter.emit(initializer);
        }
        codeWriter.emit(";\n");
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        try {
            CodeWriter codeWriter = new CodeWriter(out);
            emit(codeWriter, Collections.<Modifier>emptySet());
            return out.toString();
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public Builder toBuilder() {
        Builder builder = new Builder(type, name);
        builder.typescriptDoc.add(typescriptDoc);
        builder.annotations.addAll(annotations);
        builder.modifiers.addAll(modifiers);
        builder.initializer = initializer.isEmpty() ? null : initializer;
        return builder;
    }

    public static final class Builder {
        private final TypeName type;
        private final String name;
        private final CodeBlock.Builder typescriptDoc = CodeBlock.builder();
        private final List<AnnotationSpec> annotations = new ArrayList<>();
        private final List<Modifier> modifiers = new ArrayList<>();
        private boolean isMutable = true;
        private boolean isOptional = false;
        private CodeBlock initializer = null;

        private Builder(TypeName type, String name) {
            this.type = type;
            this.name = name;
        }

        public Builder addDoc(String format, Object... args) {
            typescriptDoc.add(format, args);
            return this;
        }

        public Builder isMutable(boolean isMutable) {
            this.isMutable = isMutable;
            return this;
        }

        public Builder optional() {
            this.isOptional = true;
            return this;
        }

        public Builder addDoc(CodeBlock block) {
            typescriptDoc.add(block);
            return this;
        }

        public Builder addAnnotations(Iterable<AnnotationSpec> annotationSpecs) {
            checkArgument(annotationSpecs != null, "annotationSpecs == null");
            for (AnnotationSpec annotationSpec : annotationSpecs) {
                this.annotations.add(annotationSpec);
            }
            return this;
        }

        public Builder addAnnotation(AnnotationSpec annotationSpec) {
            this.annotations.add(annotationSpec);
            return this;
        }

        public Builder addAnnotation(ClassName annotation) {
            this.annotations.add(AnnotationSpec.builder(annotation).build());
            return this;
        }

        public Builder addAnnotation(Class<?> annotation) {
            return addAnnotation(ClassName.get(annotation));
        }

        public Builder addModifiers(Modifier... modifiers) {
            Collections.addAll(this.modifiers, modifiers);
            return this;
        }

        public Builder initializer(String format, Object... args) {
            return initializer(CodeBlock.of(format, args));
        }

        public Builder initializer(CodeBlock codeBlock) {
            checkState(this.initializer == null, "initializer was already set");
            this.initializer = checkNotNull(codeBlock, "codeBlock == null");
            return this;
        }

        public FieldSpec build() {
            return new FieldSpec(this);
        }
    }
}
