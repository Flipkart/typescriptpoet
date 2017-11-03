package com.flipkart.typescriptpoet;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.flipkart.typescriptpoet.Util.checkArgument;
import static com.flipkart.typescriptpoet.Util.checkNotNull;
import static javax.lang.model.element.NestingKind.MEMBER;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

public final class ClassName extends TypeName implements Comparable<ClassName> {
    static final ClassName OBJECT = ClassName.get(Object.class);
    final String canonicalName;
    private final List<String> names;

    private ClassName(List<String> names) {
        this(names, new ArrayList<AnnotationSpec>());
    }

    private ClassName(List<String> names, List<AnnotationSpec> annotations) {
        super(annotations);
        this.names = Util.immutableList(names);
        this.canonicalName = (names.get(0).isEmpty()
                ? Util.join(".", names.subList(1, names.size()))
                : Util.join(".", names));
    }

    public static ClassName get(Class<?> clazz) {
        checkNotNull(clazz, "clazz == null");
        checkArgument(!clazz.isPrimitive(), "primitive types cannot be represented as a ClassName");
        checkArgument(!void.class.equals(clazz), "'void' type cannot be represented as a ClassName");
        checkArgument(!clazz.isArray(), "array types cannot be represented as a ClassName");
        List<String> names = new ArrayList<>();
        while (true) {
            String anonymousSuffix = "";
            while (clazz.isAnonymousClass()) {
                int lastDollar = clazz.getName().lastIndexOf('$');
                anonymousSuffix = clazz.getName().substring(lastDollar) + anonymousSuffix;
                clazz = clazz.getEnclosingClass();
            }
            names.add(clazz.getSimpleName() + anonymousSuffix);
            Class<?> enclosing = clazz.getEnclosingClass();
            if (enclosing == null) break;
            clazz = enclosing;
        }
        // Avoid unreliable Class.getPackage(). https://github.com/square/javapoet/issues/295
        int lastDot = clazz.getName().lastIndexOf('.');
        if (lastDot != -1) names.add(clazz.getName().substring(0, lastDot));
        Collections.reverse(names);
        return new ClassName(names);
    }

    /**
     * Returns a class name created from the given parts. For example, calling this with package name
     * {@code "java.util"} and simple names {@code "Map"}, {@code "Entry"} yields {@link Map.Entry}.
     */
    public static ClassName get(String packageName, String simpleName, String... simpleNames) {
        List<String> result = new ArrayList<>();
        result.add(packageName);
        result.add(simpleName);
        Collections.addAll(result, simpleNames);
        return new ClassName(result);
    }

    /**
     * Returns the class name for {@code element}.
     */
    public static ClassName get(TypeElement element) {
        checkNotNull(element, "element == null");
        List<String> names = new ArrayList<>();
        for (Element e = element; isClassOrInterface(e); e = e.getEnclosingElement()) {
            checkArgument(element.getNestingKind() == TOP_LEVEL || element.getNestingKind() == MEMBER,
                    "unexpected type testing");
            names.add(e.getSimpleName().toString());
        }
        names.add(getPackage(element).getQualifiedName().toString());
        Collections.reverse(names);
        return new ClassName(names);
    }

    private static boolean isClassOrInterface(Element e) {
        return e.getKind().isClass() || e.getKind().isInterface();
    }

    private static PackageElement getPackage(Element type) {
        while (type.getKind() != ElementKind.PACKAGE) {
            type = type.getEnclosingElement();
        }
        return (PackageElement) type;
    }

    @Override
    public ClassName annotated(List<AnnotationSpec> annotations) {
        return new ClassName(names, concatAnnotations(annotations));
    }

    @Override
    public TypeName withoutAnnotations() {
        return new ClassName(names);
    }

    /**
     * Returns the package name, like {@code "java.util"} for {@code Map.Entry}.
     */
    String packageName() {
        return names.get(0);
    }

    String fullyQualifiedName() {
        return packageName().replace(".", "/") + "/" + simpleName();
    }

    /**
     * Returns the enclosing class, like {@link Map} for {@code Map.Entry}. Returns null if this class
     * is not nested in another class.
     */
    ClassName enclosingClassName() {
        if (names.size() == 2) return null;
        return new ClassName(names.subList(0, names.size() - 1));
    }

    /**
     * Returns the top class in this nesting group. Equivalent to chained calls to {@link
     * #enclosingClassName()} until the result's enclosing class is null.
     */
    ClassName topLevelClassName() {
        return new ClassName(names.subList(0, 2));
    }

    /**
     * Returns a new {@link ClassName} instance for the specified {@code name} as nested inside this
     * class.
     */
    ClassName nestedClass(String name) {
        checkNotNull(name, "name == null");
        List<String> result = new ArrayList<>(names.size() + 1);
        result.addAll(names);
        result.add(name);
        return new ClassName(result);
    }

    List<String> simpleNames() {
        return names.subList(1, names.size());
    }

    /**
     * Returns the simple name of this class, like {@code "Entry"} for {@link Map.Entry}.
     */
    String simpleName() {
        return names.get(names.size() - 1);
    }

    @Override
    public int compareTo(ClassName o) {
        return canonicalName.compareTo(o.canonicalName);
    }

    @Override
    CodeWriter emit(CodeWriter out) throws IOException {
        return out.emitAndIndent(out.lookupName(this));
    }
}
