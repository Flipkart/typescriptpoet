package com.flipkart.typescriptpoet;


import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import static com.flipkart.typescriptpoet.Util.*;

public final class FunctionSpec {
    private static final String CONSTRUCTOR = "constructor";

    public final String name;
    public final List<AnnotationSpec> annotations;
    public final CodeBlock code;
    final Set<Modifier> modifiers;
    final CodeBlock defaultValue;
    private final CodeBlock javadoc;
    private final List<TypeVariableName> typeVariables;
    private final TypeName returnType;
    private final List<ParameterSpec> parameters;
    private final boolean varargs;
    private final List<TypeName> exceptions;

    private FunctionSpec(Builder builder) {
        CodeBlock code = builder.code.build();
        checkArgument(code.isEmpty() || !builder.modifiers.contains(Modifier.ABSTRACT),
                "abstract method %s cannot have code", builder.name);
        checkArgument(!builder.varargs || lastParameterIsArray(builder.parameters),
                "last parameter of varargs method %s must be an array", builder.name);

        this.name = checkNotNull(builder.name, "name == null");
        this.javadoc = builder.javadoc.build();
        this.annotations = Util.immutableList(builder.annotations);
        this.modifiers = Util.immutableSet(builder.modifiers);
        this.typeVariables = Util.immutableList(builder.typeVariables);
        this.returnType = builder.returnType;
        this.parameters = Util.immutableList(builder.parameters);
        this.varargs = builder.varargs;
        this.exceptions = Util.immutableList(builder.exceptions);
        this.defaultValue = builder.defaultValue;
        this.code = code;
    }

    static Builder methodBuilder(String name) {
        return new Builder(name);
    }

    public static Builder constructorBuilder() {
        return new Builder(CONSTRUCTOR);
    }

    private boolean lastParameterIsArray(List<ParameterSpec> parameters) {
        return !parameters.isEmpty()
                && TypeName.arrayComponent(parameters.get(parameters.size() - 1).type) != null;
    }

    void emit(CodeWriter codeWriter, String enclosingName, Set<Modifier> implicitModifiers)
            throws IOException {
        codeWriter.emitJavadoc(javadoc);
        codeWriter.emitAnnotations(annotations, false);
        codeWriter.emitModifiers(modifiers, implicitModifiers);

        if (!typeVariables.isEmpty()) {
            codeWriter.emitTypeVariables(typeVariables);
            codeWriter.emit(" ");
        }

        if (isConstructor()) {
            codeWriter.emit("$L(", CONSTRUCTOR);
        } else {
            codeWriter.emit("$L(", name);
        }

        boolean firstParameter = true;
        for (Iterator<ParameterSpec> i = parameters.iterator(); i.hasNext(); ) {
            ParameterSpec parameter = i.next();
            if (!firstParameter) codeWriter.emit(",").emitWrappingSpace();
            parameter.emit(codeWriter, !i.hasNext() && varargs);
            firstParameter = false;
        }

        codeWriter.emit(")");

        if (returnType != null && !isConstructor()) {
            codeWriter.emit(": $T", returnType);
        }

        if (defaultValue != null && !defaultValue.isEmpty()) {
            codeWriter.emit(" default ");
            codeWriter.emit(defaultValue);
        }

        if (!exceptions.isEmpty()) {
            codeWriter.emitWrappingSpace().emit("throws");
            boolean firstException = true;
            for (TypeName exception : exceptions) {
                if (!firstException) codeWriter.emit(",");
                codeWriter.emitWrappingSpace().emit("$T", exception);
                firstException = false;
            }
        }

        if (hasModifier(Modifier.ABSTRACT)) {
            codeWriter.emit(";\n");
        } else {
            codeWriter.emit(" {\n");

            codeWriter.indent();
            codeWriter.emit(code);
            codeWriter.unindent();

            codeWriter.emit("}\n");
        }
    }

    boolean hasModifier(Modifier modifier) {
        return modifiers.contains(modifier);
    }

    boolean isConstructor() {
        return name.equals(CONSTRUCTOR);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;
        return toString().equals(o.toString());
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
            emit(codeWriter, "Constructor", Collections.<Modifier>emptySet());
            return out.toString();
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public Builder toBuilder() {
        Builder builder = new Builder(name);
        builder.javadoc.add(javadoc);
        builder.annotations.addAll(annotations);
        builder.modifiers.addAll(modifiers);
        builder.typeVariables.addAll(typeVariables);
        builder.returnType = returnType;
        builder.parameters.addAll(parameters);
        builder.exceptions.addAll(exceptions);
        builder.code.add(code);
        builder.varargs = varargs;
        builder.defaultValue = defaultValue;
        return builder;
    }

    public static final class Builder {
        private final String name;

        private final CodeBlock.Builder javadoc = CodeBlock.builder();
        private final List<AnnotationSpec> annotations = new ArrayList<>();
        private final List<Modifier> modifiers = new ArrayList<>();
        private final List<ParameterSpec> parameters = new ArrayList<>();
        private final Set<TypeName> exceptions = new LinkedHashSet<>();
        private final CodeBlock.Builder code = CodeBlock.builder();
        private List<TypeVariableName> typeVariables = new ArrayList<>();
        private TypeName returnType;
        private boolean varargs;
        private CodeBlock defaultValue;

        private Builder(String name) {
            checkNotNull(name, "name == null");
            checkArgument(name.equals(CONSTRUCTOR) || SourceVersion.isName(name),
                    "not a valid name: %s", name);
            this.name = name;
        }

        public Builder addJavadoc(String format, Object... args) {
            javadoc.add(format, args);
            return this;
        }

        public Builder addJavadoc(CodeBlock block) {
            javadoc.add(block);
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
            checkNotNull(modifiers, "modifiers == null");
            Collections.addAll(this.modifiers, modifiers);
            return this;
        }

        public Builder addModifiers(Iterable<Modifier> modifiers) {
            checkNotNull(modifiers, "modifiers == null");
            for (Modifier modifier : modifiers) {
                this.modifiers.add(modifier);
            }
            return this;
        }

        public Builder addTypeVariables(Iterable<TypeVariableName> typeVariables) {
            checkArgument(typeVariables != null, "typeVariables == null");
            for (TypeVariableName typeVariable : typeVariables) {
                this.typeVariables.add(typeVariable);
            }
            return this;
        }

        public Builder addTypeVariable(TypeVariableName typeVariable) {
            typeVariables.add(typeVariable);
            return this;
        }

        public Builder returns(TypeName returnType) {
            checkState(!name.equals(CONSTRUCTOR), "constructor cannot have return type.");
            this.returnType = returnType;
            return this;
        }

        public Builder returns(Type returnType) {
            return returns(TypeName.get(returnType));
        }

        public Builder addParameters(Iterable<ParameterSpec> parameterSpecs) {
            checkArgument(parameterSpecs != null, "parameterSpecs == null");
            for (ParameterSpec parameterSpec : parameterSpecs) {
                this.parameters.add(parameterSpec);
            }
            return this;
        }

        public Builder addParameter(ParameterSpec parameterSpec) {
            this.parameters.add(parameterSpec);
            return this;
        }

        public Builder addParameter(TypeName type, String name, Modifier... modifiers) {
            return addParameter(ParameterSpec.builder(type, name, modifiers).build());
        }

        public Builder addParameter(Type type, String name, Modifier... modifiers) {
            return addParameter(TypeName.get(type), name, modifiers);
        }

        public Builder varargs() {
            return varargs(true);
        }

        public Builder varargs(boolean varargs) {
            this.varargs = varargs;
            return this;
        }

        public Builder addExceptions(Iterable<? extends TypeName> exceptions) {
            checkArgument(exceptions != null, "exceptions == null");
            for (TypeName exception : exceptions) {
                this.exceptions.add(exception);
            }
            return this;
        }

        public Builder addException(TypeName exception) {
            this.exceptions.add(exception);
            return this;
        }

        public Builder addException(Type exception) {
            return addException(TypeName.get(exception));
        }

        public Builder addCode(String format, Object... args) {
            code.add(format, args);
            return this;
        }

        public Builder addNamedCode(String format, Map<String, ?> args) {
            code.addNamed(format, args);
            return this;
        }

        public Builder addCode(CodeBlock codeBlock) {
            code.add(codeBlock);
            return this;
        }

        public Builder addComment(String format, Object... args) {
            code.add("// " + format + "\n", args);
            return this;
        }

        public Builder defaultValue(String format, Object... args) {
            return defaultValue(CodeBlock.of(format, args));
        }

        public Builder defaultValue(CodeBlock codeBlock) {
            checkState(this.defaultValue == null, "defaultValue was already set");
            this.defaultValue = checkNotNull(codeBlock, "codeBlock == null");
            return this;
        }

        /**
         * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
         *                    Shouldn't contain braces or newline characters.
         */
        public Builder beginControlFlow(String controlFlow, Object... args) {
            code.beginControlFlow(controlFlow, args);
            return this;
        }

        /**
         * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
         *                    Shouldn't contain braces or newline characters.
         */
        public Builder nextControlFlow(String controlFlow, Object... args) {
            code.nextControlFlow(controlFlow, args);
            return this;
        }

        public Builder endControlFlow() {
            code.endControlFlow();
            return this;
        }

        /**
         * @param controlFlow the optional control flow construct and its code, such as
         *                    "while(foo == 20)". Only used for "do/while" control flows.
         */
        public Builder endControlFlow(String controlFlow, Object... args) {
            code.endControlFlow(controlFlow, args);
            return this;
        }

        public Builder addStatement(String format, Object... args) {
            code.addStatement(format, args);
            return this;
        }

        public Builder addStatement(CodeBlock codeBlock) {
            code.addStatement(codeBlock);
            return this;
        }

        public FunctionSpec build() {
            return new FunctionSpec(this);
        }
    }
}
