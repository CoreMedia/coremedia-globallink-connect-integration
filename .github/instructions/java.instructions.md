---
applyTo: "**/*.java"
---

# GitHub Copilot Instructions: Java

These instructions are dedicated to the Java code in this repository, thus,
it applies to all files matching `**/*.java`.

## General Copilot Instruction

When validating Java code, assume that you have expert knowledge in Java,
ranging from knowing modern language features as well as knowing about possible
security risks.

## Language Specification Level

**Detect the Java specification level** from the repository's
[root POM](../../pom.xml) by checking the `maven.compiler.release` property
value.

**Apply version-specific rules** based on the detected Java version:

- Only suggest language features available in the detected version
- Warn about features from newer Java versions
- If the Java version cannot be determined, **assume Java 17** as the baseline

**Auto-adapt validation rules** to match the project's actual Java version
rather than hardcoded assumptions.

**Exceptions to Java language features:** We decided that some language
features should not be used, even if they are available in the detected Java
version. For example, we decided to not use `var` (see below). Here is a list
of such exceptions:

- Do not use `var`.

  We decided that for clarity
  [Local-Variable Type Inference (JEP 286)](https://openjdk.org/jeps/286)
  should not be used. For this repository, that also serves as example for
  others, consider using `var` in Java code is not allowed.

### **Validation Rules**

* **Warn on unsupported language features**:
  Flag usage of features from newer Java versions. For example, if the current 
  language level is 17, Pattern Matching for switch (JEP 441, available in Java 21+) 
  must not be used:

  ```java
  // NOT supported in Java 17, available in Java 21+
  return switch (obj) {
      case String s -> "String: " + s;
      case Integer i -> "Integer: " + i;
      default -> "Unknown type";
  };
  ```

* **Recommend available language features**:
  Recommend modern features available in the detected Java version. For Java 17,
  for example, recommend text blocks, switch expressions, records, and sealed
  classes when appropriate.

### **Code Quality & Style**

* **Prefer immutable objects**:
  Use `final` fields, immutable collections (`List.of()`, `Map.of()`), and
  record classes when appropriate.
* **Use Optional correctly**:
  Never use `Optional.get()` without checking, prefer `orElse()`,
  `orElseThrow()`, or `ifPresent()`.
* **Avoid raw types**:
  Always use parameterized types for generics (e.g., `List<String>` not `List`).
* **Annotate with `@org.jspecify.annotations.NullMarked`**:
  Apply this annotation to all top-level classes, interfaces, enums, and
  annotations (including test classes) to indicate that all types are
  non-nullable by default, unless explicitly marked as `@Nullable`. This
  annotation should appear after other class-level annotations and immediately
  before the class/interface/enum declaration.
* **Use `@org.jspecify.annotations.Nullable` for nullable types**:
  When a field, parameter, or return type can be `null`, explicitly annotate it
  with `@Nullable`. This works in conjunction with `@NullMarked` to provide
  precise null-safety contracts.
* **Avoid Error Prone Bug Patterns**:
  Avoid all bug patterns listed at <https://errorprone.info/bugpatterns>,
  except the ones that are experimental, yet.
* **Prefer enums for constants and utilities**:
  When defining multiple related constants, use an enum instead of a class with
  `public static final` fields. For utility classes with only static methods,
  prefer using an enum with a semicolon-only body: `enum UtilityName { ; }`.
  This prevents instantiation more clearly than a private constructor.
* **Use static imports judiciously**:
  Static imports are encouraged for:
  - Assertion methods (e.g., `org.assertj.core.api.Assertions.assertThat`)
  - Test lifecycle methods (e.g., `org.junit.jupiter.api.Assertions.*`)
  - Factory methods (e.g., `java.util.Collections.*`, `java.util.List.of`)
  - Logger creation: `java.lang.invoke.MethodHandles.lookup` and
    `org.slf4j.LoggerFactory.getLogger`
* **Logger pattern**:
  For logging, use SLF4J with the following pattern:
  ```java
  private static final Logger LOG = getLogger(lookup().lookupClass());
  ```
  This requires static imports:
  ```java
  import static java.lang.invoke.MethodHandles.lookup;
  import static org.slf4j.LoggerFactory.getLogger;
  ```

### **Modern Java Features (Java 21)**

* **Use text blocks**:
  For multi-line strings, prefer text blocks over string concatenation.
* **Use switch expressions**:
  Replace traditional switch statements with switch expressions when returning
  values.
* **Use records**:
  For data-only classes, prefer `record` over traditional classes with
  getters/setters.

### **Security & Best Practices**

* **Validate input parameters**:
  Always check method parameters for `null` and invalid values.
* **Use try-with-resources**:
  For `AutoCloseable` resources, always use try-with-resources syntax.
* **Avoid string concatenation in loops**:
  Use `StringBuilder` or `StringJoiner` for repeated string operations.
* **Prefer composition over inheritance**:
  Favor composition and interfaces over class inheritance.

### **Documentation & Naming**

* **Use meaningful names**:
  Variables, methods, and classes should clearly express their purpose.
* **Write complete Javadoc**:
  Include `@param`, `@return`, `@throws` for public methods.
* **Follow naming conventions**:
  Use camelCase for methods/variables, PascalCase for classes, UPPER_CASE for
  constants.
