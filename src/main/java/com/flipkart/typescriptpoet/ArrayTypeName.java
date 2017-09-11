package com.flipkart.typescriptpoet;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;

import static com.flipkart.typescriptpoet.Util.checkNotNull;

public final class ArrayTypeName extends TypeName {
    public final TypeName componentType;

    private ArrayTypeName(TypeName componentType) {
        this(componentType, new ArrayList<AnnotationSpec>());
    }

    private ArrayTypeName(TypeName componentType, List<AnnotationSpec> annotations) {
        super(annotations);
        this.componentType = checkNotNull(componentType, "rawType == null");
    }

    @Override
    public ArrayTypeName annotated(List<AnnotationSpec> annotations) {
        return new ArrayTypeName(componentType, concatAnnotations(annotations));
    }

    @Override
    public TypeName withoutAnnotations() {
        return new ArrayTypeName(componentType);
    }

    @Override
    CodeWriter emit(CodeWriter out) throws IOException {
        return out.emit("$T[]", componentType);
    }

    /**
     * Returns an array type whose elements are all instances of {@code componentType}.
     */
    public static ArrayTypeName of(TypeName componentType) {
        return new ArrayTypeName(componentType);
    }

    /**
     * Returns an array type whose elements are all instances of {@code componentType}.
     */
    public static ArrayTypeName of(Type componentType) {
        return of(TypeName.get(componentType));
    }

    /**
     * Returns an array type equivalent to {@code mirror}.
     */
    public static ArrayTypeName get(ArrayType mirror) {
        return get(mirror, new LinkedHashMap<TypeParameterElement, TypeVariableName>());
    }

    static ArrayTypeName get(
            ArrayType mirror, Map<TypeParameterElement, TypeVariableName> typeVariables) {
        return new ArrayTypeName(get(mirror.getComponentType(), typeVariables));
    }

    /**
     * Returns an array type equivalent to {@code type}.
     */
    public static ArrayTypeName get(GenericArrayType type) {
        return get(type, new LinkedHashMap<Type, TypeVariableName>());
    }

    static ArrayTypeName get(GenericArrayType type, Map<Type, TypeVariableName> map) {
        return ArrayTypeName.of(get(type.getGenericComponentType(), map));
    }
}