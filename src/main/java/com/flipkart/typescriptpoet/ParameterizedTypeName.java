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

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static com.flipkart.typescriptpoet.Util.checkArgument;
import static com.flipkart.typescriptpoet.Util.checkNotNull;

public class ParameterizedTypeName extends TypeName {
    final ClassName rawType;
    final List<TypeName> typeArguments;
    private final ParameterizedTypeName enclosingType;
    private MapParameterizedTypeName mapParameterizedTypeName;
    private ListParameterizedTypeName listParameterizedTypeName;

    ParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType,
                          List<TypeName> typeArguments) {
        this(enclosingType, rawType, typeArguments, new ArrayList<AnnotationSpec>());
    }

    private ParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType,
                                  List<TypeName> typeArguments, List<AnnotationSpec> annotations) {
        super(annotations);
        this.rawType = checkNotNull(rawType, "rawType == null");
        this.enclosingType = enclosingType;
        this.typeArguments = Util.immutableList(typeArguments);
        checkArgument(!this.typeArguments.isEmpty() || enclosingType != null,
                "no type arguments: %s", rawType);
    }

    /**
     * Returns a parameterized type, applying {@code typeArguments} to {@code rawType}.
     */
    public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
        return new ParameterizedTypeName(null, rawType, Arrays.asList(typeArguments));
    }

    /**
     * Returns a parameterized type, applying {@code typeArguments} to {@code rawType}.
     */
    public static ParameterizedTypeName get(Class<?> rawType, Type... typeArguments) {
        return new ParameterizedTypeName(null, ClassName.get(rawType), list(typeArguments));
    }

    /**
     * Returns a parameterized type equivalent to {@code type}.
     */
    public static ParameterizedTypeName get(ParameterizedType type) {
        return get(type, new LinkedHashMap<Type, TypeVariableName>());
    }

    /**
     * Returns a parameterized type equivalent to {@code type}.
     */
    static ParameterizedTypeName get(ParameterizedType type, Map<Type, TypeVariableName> map) {
        ClassName rawType = ClassName.get((Class<?>) type.getRawType());
        ParameterizedType ownerType = (type.getOwnerType() instanceof ParameterizedType)
                && !Modifier.isStatic(((Class<?>) type.getRawType()).getModifiers())
                ? (ParameterizedType) type.getOwnerType() : null;
        List<TypeName> typeArguments = TypeName.list(type.getActualTypeArguments(), map);
        return (ownerType != null)
                ? get(ownerType, map).nestedClass(rawType.simpleName(), typeArguments)
                : new ParameterizedTypeName(null, rawType, typeArguments);
    }

    private MapParameterizedTypeName getMapParameterizedTypeName() {
        if (mapParameterizedTypeName == null) {
            mapParameterizedTypeName = new MapParameterizedTypeName(enclosingType, rawType, typeArguments);
        }
        return mapParameterizedTypeName;
    }

    private ListParameterizedTypeName getListParameterizedTypeName() {
        if (listParameterizedTypeName == null) {
            listParameterizedTypeName = new ListParameterizedTypeName(enclosingType, rawType, typeArguments);
        }
        return listParameterizedTypeName;
    }

    @Override
    public ParameterizedTypeName annotated(List<AnnotationSpec> annotations) {
        return new ParameterizedTypeName(
                enclosingType, rawType, typeArguments, concatAnnotations(annotations));
    }

    @Override
    public TypeName withoutAnnotations() {
        return new ParameterizedTypeName(
                enclosingType, rawType, typeArguments, new ArrayList<AnnotationSpec>());
    }

    @Override
    CodeWriter emit(CodeWriter out) throws IOException {
        if (Util.isMap(rawType)) {
            return getMapParameterizedTypeName().emit(out);
        }

        return getListParameterizedTypeName().emit(out);
    }

    /**
     * Returns a new {@link ParameterizedTypeName} instance for the specified {@code name} as nested
     * inside this class.
     */
    public ParameterizedTypeName nestedClass(String name) {
        checkNotNull(name, "name == null");
        return new ParameterizedTypeName(this, rawType.nestedClass(name), new ArrayList<TypeName>(),
                new ArrayList<AnnotationSpec>());
    }

    /**
     * Returns a new {@link ParameterizedTypeName} instance for the specified {@code name} as nested
     * inside this class, with the specified {@code typeArguments}.
     */
    public ParameterizedTypeName nestedClass(String name, List<TypeName> typeArguments) {
        checkNotNull(name, "name == null");
        return new ParameterizedTypeName(this, rawType.nestedClass(name), typeArguments,
                new ArrayList<AnnotationSpec>());
    }

    private static final class ListParameterizedTypeName extends ParameterizedTypeName {

        ListParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType, List<TypeName> typeArguments) {
            super(enclosingType, rawType, typeArguments);
        }

        public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
            return new ListParameterizedTypeName(null, rawType, Arrays.asList(typeArguments));
        }

        @Override
        CodeWriter emit(CodeWriter out) throws IOException {
            boolean firstParameterInType;
            boolean lastParameterInType;

            if (!Util.isList(rawType)) {
                firstParameterInType = false;
                lastParameterInType = false;
                rawType.emitAnnotations(out);
                rawType.emit(out);
            } else {
                firstParameterInType = true;
                lastParameterInType = true;
            }

            int totalArgumentsSize = typeArguments.size();
            for (int i = 0; i < totalArgumentsSize; i++) {
                TypeName parameter = typeArguments.get(i);
                if (i > 0 && i <= totalArgumentsSize - 1) {
                    out.emitAndIndent(", ");
                }
                if (!firstParameterInType && i == 0) out.emitAndIndent("<");
                parameter.emitAnnotations(out);
                parameter.emit(out);
                if (!firstParameterInType && i == totalArgumentsSize - 1) out.emitAndIndent(">");
                firstParameterInType = false;
            }

            if (!firstParameterInType && lastParameterInType) {
                out.emitAndIndent("[]");
            }

            return out;
        }
    }

    public static final class MapParameterizedTypeName extends ParameterizedTypeName {

        MapParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType, List<TypeName> typeArguments) {
            super(enclosingType, rawType, typeArguments);
        }

        public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
            return new MapParameterizedTypeName(null, rawType, Arrays.asList(typeArguments));
        }

        @Override
        CodeWriter emit(CodeWriter out) throws IOException {
            out.emit("Record<string, ");
            TypeName parameter = typeArguments.get(1);
            parameter.emitAnnotations(out);
            parameter.emit(out);
            out.emit(">");

            return out;
        }
    }
}
