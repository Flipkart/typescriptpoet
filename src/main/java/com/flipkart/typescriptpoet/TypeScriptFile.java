package com.flipkart.typescriptpoet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.flipkart.typescriptpoet.Util.checkArgument;
import static com.flipkart.typescriptpoet.Util.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A Java file containing a single top level class.
 */
public final class TypeScriptFile {
    private static final Appendable NULL_APPENDABLE = new Appendable() {
        @Override
        public Appendable append(CharSequence charSequence) {
            return this;
        }

        @Override
        public Appendable append(CharSequence charSequence, int start, int end) {
            return this;
        }

        @Override
        public Appendable append(char c) {
            return this;
        }
    };

    private static final String TYPESCRIPT_EXTENSION = ".ts";
    public final CodeBlock fileComment;
    public final String packageName;
    public final TypeSpec typeSpec;
    public final boolean skipJavaLangImports;
    private final Set<String> staticImports;
    private final String indent;
    private final Path currentPath;

    private TypeScriptFile(Builder builder) {
        this.fileComment = builder.fileComment.build();
        this.packageName = builder.packageName;
        this.typeSpec = builder.typeSpec;
        this.skipJavaLangImports = builder.skipJavaLangImports;
        this.staticImports = Util.immutableSet(builder.staticImports);
        this.indent = builder.indent;
        this.currentPath = Util.absolutePath(packageName, typeSpec.name);
    }

    public static Builder builder(String packageName, TypeSpec typeSpec) {
        checkNotNull(typeSpec, "typeSpec == null");
        checkNotNull(packageName, "packageName == null");
        return new Builder(packageName, typeSpec);
    }

    public void writeTo(Appendable out) throws IOException {
        // First pass: emit the entire class, just to collect the types we'll need to import.
        CodeWriter importsCollector = new CodeWriter(NULL_APPENDABLE, indent, staticImports);
        emit(importsCollector);
        Map<String, ClassName> suggestedImports = importsCollector.suggestedImports();

        // Second pass: write the code, taking advantage of the imports.
        CodeWriter codeWriter = new CodeWriter(out, indent, suggestedImports, staticImports);
        emit(codeWriter);
    }

    /**
     * Writes this to {@code directory} as UTF-8 using the standard directory structure.
     */
    public void writeTo(Path directory, String fileExtension) throws IOException {
        checkArgument(Files.notExists(directory) || Files.isDirectory(directory),
                "path %s exists but is not a directory.", directory);

        Path outputDirectory = directory;

        if (!packageName.isEmpty()) {
            for (String packageComponent : packageName.split("\\.")) {
                outputDirectory = outputDirectory.resolve(packageComponent);
            }
            Files.createDirectories(outputDirectory);
        }

        Path outputPath = outputDirectory.resolve(typeSpec.name + (fileExtension != null ? fileExtension : ".ts"));
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outputPath), UTF_8)) {
            writeTo(writer);
        }
    }

    /**
     * Writes this to {@code directory} as UTF-8 using the standard directory structure.
     */
    public void writeTo(File directory, String fileExtension) throws IOException {
        writeTo(directory.toPath(), fileExtension);
    }

    /**
     * Writes this to {@code directory} as UTF-8 using the standard directory structure.
     */
    public void writeTo(File directory) throws IOException {
        writeTo(directory.toPath(), TYPESCRIPT_EXTENSION);
    }

    private void emit(CodeWriter codeWriter) throws IOException {
        codeWriter.pushPackage(packageName);

        if (!fileComment.isEmpty()) {
            codeWriter.emitComment(fileComment);
        }

        if (!staticImports.isEmpty()) {
            for (String signature : staticImports) {
                codeWriter.emit("import static $L;\n", signature);
            }
            codeWriter.emit("\n");
        }

        int importedTypesCount = 0;
        for (ClassName className : new TreeSet<>(codeWriter.importedTypes().values())) {
            Path importPath = Paths.get(className.fullyQualifiedName());
            String relativePath = Util.getRelativePath(currentPath, importPath);
            codeWriter.emit("import { $L ;\n", className.simpleName() + " } from '" + relativePath + "'");
            importedTypesCount++;
        }

        if (importedTypesCount > 0) {
            codeWriter.emit("\n");
        }

        typeSpec.emit(codeWriter, null, Collections.<Modifier>emptySet());
        codeWriter.popPackage();
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
        try {
            StringBuilder result = new StringBuilder();
            writeTo(result);
            return result.toString();
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    public Builder toBuilder() {
        Builder builder = new Builder(packageName, typeSpec);
        builder.fileComment.add(fileComment);
        builder.skipJavaLangImports = skipJavaLangImports;
        builder.indent = indent;
        return builder;
    }

    public static final class Builder {
        private final TypeSpec typeSpec;
        private final String packageName;
        private final CodeBlock.Builder fileComment = CodeBlock.builder();
        private final Set<String> staticImports = new TreeSet<>();
        private boolean skipJavaLangImports;
        private String indent = "  ";

        private Builder(String packageName, TypeSpec typeSpec) {
            this.packageName = packageName;
            this.typeSpec = typeSpec;
        }

        public Builder addFileComment(String format, Object... args) {
            this.fileComment.add(format, args);
            return this;
        }

        public Builder addStaticImport(Enum<?> constant) {
            return addStaticImport(ClassName.get(constant.getDeclaringClass()), constant.name());
        }

        public Builder addStaticImport(Class<?> clazz, String... names) {
            return addStaticImport(ClassName.get(clazz), names);
        }

        public Builder addStaticImport(ClassName className, String... names) {
            checkArgument(className != null, "className == null");
            checkArgument(names != null, "names == null");
            checkArgument(names.length > 0, "names array is empty");
            for (String name : names) {
                checkArgument(name != null, "null entry in names array: %s", Arrays.toString(names));
                staticImports.add(className.canonicalName + "." + name);
            }
            return this;
        }

        /**
         * Call this to omit imports for classes in {@code java.lang}, such as {@code java.lang.String}.
         * <p>
         * <p>By default, JavaPoet explicitly imports types in {@code java.lang} to defend against
         * naming conflicts. Suppose an (ill-advised) class is named {@code com.example.String}. When
         * {@code java.lang} imports are skipped, generated code in {@code com.example} that references
         * {@code java.lang.String} will get {@code com.example.String} instead.
         */
        public Builder skipJavaLangImports(boolean skipJavaLangImports) {
            this.skipJavaLangImports = skipJavaLangImports;
            return this;
        }

        public Builder indent(String indent) {
            this.indent = indent;
            return this;
        }

        public TypeScriptFile build() {
            return new TypeScriptFile(this);
        }
    }
}