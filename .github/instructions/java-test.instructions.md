---
applyTo: "**/src/test/java/**/*.java"
---

# GitHub Copilot Instructions: Java Tests (based on JUnit 6)

These instructions are dedicated to the Java test code in this repository, thus,
it applies to all files matching `**/src/test/java/**/*.java`. This is an
addition to the general Java instructions in
[java.instructions.md](./java.instructions.md) and should be applied in addition
to those.

## General Copilot Instruction

When validating Java test code, assume that you have expert knowledge in Java,
JUnit 6, AssertJ test assertions, and testing best practices.

## Assertions

- **Use AssertJ for assertions**: Prefer AssertJ's fluent assertion style for
  better readability and more informative error messages. For example, instead
  of using JUnit's `assertEquals(expected, actual)`, use AssertJ's
  `assertThat(actual).isEqualTo(expected)`.
- **Prefer test method names starting with "should"**: This convention helps to
  clearly express the expected behavior being tested. For example,
  `shouldReturnTrueWhenInputIsValid()`.
- **Prefer SoftAssertions for multiple assertions**: When a test method contains
  multiple assertions, use AssertJ's `SoftAssertions` to allow all assertions to
  be evaluated and report all failures at once, rather than stopping at the
  first failure. One exception to this rule is when the first assertion is a
  precondition check, in which case it is acceptable to fail fast if the
  precondition is not met.
- **Use `assertThatThrownBy` for exception testing**: When testing that a method
  throws an exception, use AssertJ's `assertThatThrownBy` for more expressive
  assertions:
  ```java
  assertThatThrownBy(() -> methodThatThrows())
    .isInstanceOf(ExpectedException.class)
    .hasMessage("expected message");
  ```

## Test Organization

- **Use `@Nested` classes for logical grouping**: Group related tests using
  JUnit's `@Nested` annotation to improve test organization and readability.
  Nested class names should describe the aspect being tested (e.g.,
  `EmptyBehavior`, `ValidationBehavior`).
- **Use `@DisplayName` for complex test descriptions**: When test method names
  cannot fully express the test intent, use `@DisplayName` to provide a more
  descriptive name.
- **Use parameterized tests for multiple inputs**: When testing the same logic
  with different inputs, use `@ParameterizedTest` with appropriate sources
  (`@EnumSource`, `@CsvSource`, `@ArgumentsSource`, etc.).
- **Create test fixtures with enums**: For parameterized tests with complex
  setup, consider using enums that implement `Supplier` or `ArgumentsProvider`
  to provide test data in a type-safe manner.

## Test Lifecycle

- **Use `@BeforeEach` for a common setup**: Setup that is common to all tests in
  a test class should be placed in a method annotated with `@BeforeEach`.
- **Use `@AfterEach` for a cleanup**: Cleanup that needs to happen after each
  test should be placed in a method annotated with `@AfterEach`.
- **Use `@BeforeAll` and `@AfterAll` sparingly**: Only use these for expensive
  setup/teardown that can be shared across all tests and doesn't need to be
  reset between tests.
