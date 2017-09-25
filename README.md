TypeScriptPoet
========

`TypeScriptPoet` is a TypeScript API for generating `.ts` source files.

Source file generation can be useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.


### Example

Here's a (boring) `HelloWorld` class:

```java
export class HelloWorld {
  printMessage() {
    console.log("Hello World!");
  }
}
```

And this is the (exciting) code to generate it with JavaPoet:

```java
MethodSpec main = MethodSpec.methodBuilder("printMessage")
    .addStatement("Hello World!")
    .build();

TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addModifiers(Modifier.EXPORT)
    .addMethod(main)
    .build();

TypeScriptFile file = TypeScriptFile.builder("com.example.helloworld", helloWorld)
    .build();

file.writeTo(System.out);
```
