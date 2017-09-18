package com.flipkart.typescriptpoet;

public enum Modifier {
    PUBLIC,

    PRIVATE,

    STATIC,

    EXPORT,

    OVERRIDE,

    ABSTRACT,

    FINAL,

    DEFAULT;

    /**
     * Returns this modifier's name in lowercase.
     */
    public String toString() {
        return name().toLowerCase(java.util.Locale.US);
    }
}
