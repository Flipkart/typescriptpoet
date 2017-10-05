package com.flipkart.typescriptpoet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class MapParameterizedTypeName extends ParameterizedTypeName {

    MapParameterizedTypeName(ParameterizedTypeName enclosingType, ClassName rawType, List<TypeName> typeArguments) {
        super(enclosingType, rawType, typeArguments);
    }

    public static ParameterizedTypeName get(ClassName rawType, TypeName... typeArguments) {
        return new MapParameterizedTypeName(null, rawType, Arrays.asList(typeArguments));
    }

    @Override
    CodeWriter emit(CodeWriter out) throws IOException {
        out.emit("{ [key: ");
        TypeName parameter = typeArguments.get(0);
        parameter.emitAnnotations(out);
        parameter.emit(out);
        out.emit("]: ");
        parameter = typeArguments.get(1);
        parameter.emitAnnotations(out);
        parameter.emit(out);
        out.emit(" }");

        return out;
    }
}