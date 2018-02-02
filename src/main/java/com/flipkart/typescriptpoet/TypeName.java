/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flipkart.typescriptpoet;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor7;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

public class TypeName {
    public static final TypeName VOID = new TypeName("void");
    public static final TypeName INT = new TypeName("number");
    public static final TypeName STRING = new TypeName("string");
    public static final TypeName OBJECT = new TypeName("object");
    public static final TypeName BOOLEAN = new TypeName("boolean");
    public static final TypeName BYTE = new TypeName("number");
    public static final TypeName SHORT = new TypeName("number");
    public static final TypeName LONG = new TypeName("number");
    public static final TypeName CHAR = new TypeName("char");
    public static final TypeName FLOAT = new TypeName("number");
    public static final TypeName DOUBLE = new TypeName("number");
    public static final TypeName ANY = new TypeName("any");

    public final List<AnnotationSpec> annotations;
    /**
     * The name of this type if it is a keyword, or null.
     */
    private final String keyword;
    /**
     * Lazily-initialized toString of this type name.
     */
    private String cachedString;

    private TypeName(String keyword) {
        this(keyword, new ArrayList<AnnotationSpec>());
    }

    private TypeName(String keyword, List<AnnotationSpec> annotations) {
        this.keyword = keyword;
        this.annotations = Util.immutableList(annotations);
    }

    // Package-private constructor to prevent third-party subclasses.
    TypeName(List<AnnotationSpec> annotations) {
        this(null, annotations);
    }

    /**
     * Returns a type name equivalent to {@code mirror}.
     */
    public static TypeName get(TypeMirror mirror) {
        return get(mirror, new LinkedHashMap<TypeParameterElement, TypeVariableName>());
    }

    static TypeName get(final TypeMirror mirror,
                        final Map<TypeParameterElement, TypeVariableName> typeVariables) {
        return mirror.accept(new SimpleTypeVisitor7<TypeName, Void>() {
            @Override
            public TypeName visitPrimitive(PrimitiveType t, Void p) {
                switch (t.getKind()) {
                    case BOOLEAN:
                        return TypeName.BOOLEAN;
                    case BYTE:
                        return TypeName.BYTE;
                    case SHORT:
                        return TypeName.SHORT;
                    case INT:
                        return TypeName.INT;
                    case LONG:
                        return TypeName.LONG;
                    case CHAR:
                        return TypeName.CHAR;
                    case FLOAT:
                        return TypeName.FLOAT;
                    case DOUBLE:
                        return TypeName.DOUBLE;
                    default:
                        throw new AssertionError();
                }
            }

            @Override
            public TypeName visitDeclared(DeclaredType t, Void p) {
                ClassName rawType = ClassName.get((TypeElement) t.asElement());
                TypeMirror enclosingType = t.getEnclosingType();
                TypeName enclosing =
                        (enclosingType.getKind() != TypeKind.NONE)
                                && !t.asElement().getModifiers().contains(Modifier.STATIC)
                                ? enclosingType.accept(this, null)
                                : null;
                if (t.getTypeArguments().isEmpty() && !(enclosing instanceof ParameterizedTypeName)) {
                    return rawType;
                }

                List<TypeName> typeArgumentNames = new ArrayList<>();
                for (TypeMirror mirror : t.getTypeArguments()) {
                    typeArgumentNames.add(get(mirror, typeVariables));
                }
                return enclosing instanceof ParameterizedTypeName
                        ? ((ParameterizedTypeName) enclosing).nestedClass(
                        rawType.simpleName(), typeArgumentNames)
                        : new ParameterizedTypeName(null, rawType, typeArgumentNames);
            }

            @Override
            public TypeName visitError(ErrorType t, Void p) {
                return visitDeclared(t, p);
            }

            @Override
            public ArrayTypeName visitArray(ArrayType t, Void p) {
                return ArrayTypeName.get(t, typeVariables);
            }

            @Override
            public TypeName visitTypeVariable(javax.lang.model.type.TypeVariable t, Void p) {
                return TypeVariableName.get(t, typeVariables);
            }

            @Override
            public TypeName visitWildcard(javax.lang.model.type.WildcardType t, Void p) {
                //return WildcardTypeName.get(t, typeVariables);
                // TODO: 11/09/17 handle
                return null;
            }

            @Override
            public TypeName visitNoType(NoType t, Void p) {
                if (t.getKind() == TypeKind.VOID) return TypeName.VOID;
                return super.visitUnknown(t, p);
            }

            @Override
            protected TypeName defaultAction(TypeMirror e, Void p) {
                throw new IllegalArgumentException("Unexpected type mirror: " + e);
            }
        }, null);
    }

    /**
     * Returns a type name equivalent to {@code type}.
     */
    public static TypeName get(Type type) {
        return get(type, new LinkedHashMap<Type, TypeVariableName>());
    }

    static TypeName get(Type type, Map<Type, TypeVariableName> map) {
        if (type instanceof Class<?>) {
            Class<?> classType = (Class<?>) type;
            if (type == void.class) return VOID;
            if (type == boolean.class) return BOOLEAN;
            if (type == byte.class) return BYTE;
            if (type == short.class) return SHORT;
            if (type == int.class) return INT;
            if (type == long.class) return LONG;
            if (type == char.class) return CHAR;
            if (type == float.class) return FLOAT;
            if (type == double.class) return DOUBLE;
            if (classType.isArray()) return ArrayTypeName.of(get(classType.getComponentType(), map));
            return ClassName.get(classType);

        } else if (type instanceof ParameterizedType) {
            return ParameterizedTypeName.get((ParameterizedType) type, map);
            //todo handle wildcard
        } else if (type instanceof TypeVariable<?>) {
            return TypeVariableName.get((TypeVariable<?>) type, map);

        } else if (type instanceof GenericArrayType) {
            return ArrayTypeName.get((GenericArrayType) type, map);

        } else {
            throw new IllegalArgumentException("unexpected type: " + type);
        }
    }

    /**
     * Converts an array of types to a list of type names.
     */
    static List<TypeName> list(Type[] types) {
        return list(types, new LinkedHashMap<Type, TypeVariableName>());
    }

    static List<TypeName> list(Type[] types, Map<Type, TypeVariableName> map) {
        List<TypeName> result = new ArrayList<>(types.length);
        for (Type type : types) {
            result.add(get(type, map));
        }
        return result;
    }

    /**
     * Returns the array component of {@code type}, or null if {@code type} is not an array.
     */
    static TypeName arrayComponent(TypeName type) {
        return type instanceof ArrayTypeName
                ? ((ArrayTypeName) type).componentType
                : null;
    }

    public final TypeName annotated(AnnotationSpec... annotations) {
        return annotated(Arrays.asList(annotations));
    }

    public TypeName annotated(List<AnnotationSpec> annotations) {
        Util.checkNotNull(annotations, "annotations == null");
        return new TypeName(keyword, concatAnnotations(annotations));
    }

    public TypeName withoutAnnotations() {
        return new TypeName(keyword);
    }

    protected final List<AnnotationSpec> concatAnnotations(List<AnnotationSpec> annotations) {
        List<AnnotationSpec> allAnnotations = new ArrayList<>(this.annotations);
        allAnnotations.addAll(annotations);
        return allAnnotations;
    }

    public boolean isAnnotated() {
        return !annotations.isEmpty();
    }

    /**
     * Returns true if this is a primitive type like {@code int}. Returns false for all other types
     * types including boxed primitives and {@code void}.
     */
    public boolean isPrimitive() {
        return keyword != null && this != VOID;
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() && toString().equals(o.toString());
    }

    @Override
    public final int hashCode() {
        return toString().hashCode();
    }

    @Override
    public final String toString() {
        String result = cachedString;
        if (result == null) {
            try {
                StringBuilder resultBuilder = new StringBuilder();
                CodeWriter codeWriter = new CodeWriter(resultBuilder);
                emitAnnotations(codeWriter);
                emit(codeWriter);
                result = resultBuilder.toString();
                cachedString = result;
            } catch (IOException e) {
                throw new AssertionError();
            }
        }
        return result;
    }

    CodeWriter emit(CodeWriter out) throws IOException {
        if (keyword == null) throw new AssertionError();
        return out.emitAndIndent(keyword);
    }

    CodeWriter emitAnnotations(CodeWriter out) throws IOException {
        for (AnnotationSpec annotation : annotations) {
            annotation.emit(out, true);
            out.emit(" ");
        }
        return out;
    }
}
