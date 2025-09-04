# GitHub Copilot Instructions: Java

These instructions are dedicated to the Java code in this repository, thus,
it applies to all files matching `**/*.java`.

## General Copilot Instruction

When validating Java code, assume that you have expert knowledge in Java,
ranging from knowing modern language features as well as knowing about possible
security risks.

## Language Specification Level

The actual Java specification level can be found in the repositories
[root POM](../../pom.xml). See the corresponding property value of
`maven.compiler.release`.

If in doubt, consider Java 17 as the current specification level.

### Copilot Instructions

* **Warn on unsupported language features.**

  All more modern language features should not be used. For example, if
  the current language level is 17, 
  [Pattern Matching for instanceof (JEP 394)](https://openjdk.org/jeps/394)
  must not be used. Adapt these checks to the detected Java version.

* **Hint on new language features.**

  Report hints in more modern language features to use. Like, for example,
  since Java 9 (as part of [JEP 213](https://openjdk.org/jeps/213)) complex
  multi-resource assignments can be simplified using final or even effectively
  final variables:

  ```java
  final Scanner scanner = new Scanner(new File("testRead.txt"));
  PrintWriter writer = new PrintWriter(new File("testWrite.txt"))
  try (scanner;writer) {
    // omitted
  }
  ```

  (Example from [Baeldung](https://www.baeldung.com/java-try-with-resources))

  There are exceptions to this rule, though:

  * Do not use `var`.

    We decided that for clarity
    [Local-Variable Type Inference (JEP 286)](https://openjdk.org/jeps/286)
    should not be used. For this repository, that also serves as example for
    others, consider using `var` in Java code is not allowed.
