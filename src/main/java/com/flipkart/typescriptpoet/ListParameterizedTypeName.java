package com.flipkart.typescriptpoet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class ListParameterizedTypeName extends ParameterizedTypeName {

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

        for (TypeName parameter : typeArguments) {
            if (!firstParameterInType) out.emitAndIndent("<");
            parameter.emitAnnotations(out);
            parameter.emit(out);
            if (!firstParameterInType) out.emitAndIndent(">");
            firstParameterInType = false;
        }

        if (!firstParameterInType && lastParameterInType) {
            out.emitAndIndent("[]");
        }

        return out;
    }
}