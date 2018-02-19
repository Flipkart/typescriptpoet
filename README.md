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
Download
--------

Download [the latest .jar][dl] or depend via Maven:
```xml
<dependency>
  <groupId>com.flipkart.typescript</groupId>
  <artifactId>typescriptpoet</artifactId>
  <version>1.0.0</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.flipkart.typescript:typescriptpoet:1.0.0'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


Credits
-------

Built on top of [JavaPoet](https://github.com/square/javapoet)

License
-------

    Copyright (C) 2015 Square, Inc.
    Copyright 2018 Flipkart, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
