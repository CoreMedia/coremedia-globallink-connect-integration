---
applyTo: "**/*.ts,**/*.tsx"
---

# GitHub Copilot Instructions: TypeScript

These instructions are dedicated to TypeScript code in this repository, applying
to all files matching `**/*.ts` and `**/*.tsx`.

## General Copilot Instruction

When validating TypeScript code, assume that you have expert knowledge in
TypeScript, React (for `.tsx` files), and modern JavaScript/TypeScript best
practices.

## Code Quality & Type Safety

* **Rely on ESLint for code quality**:
  This project uses ESLint to enforce code quality rules. Follow ESLint
  recommendations and warnings. If ESLint is configured, it will catch most
  issues automatically.
* **Prefer explicit types over implicit**:
  While TypeScript can infer many types, prefer explicit type annotations for
  function parameters, return types, and exported interfaces/types.
* **Export types separately**:
  When exporting both a component and its props type, export them separately:
  ```typescript
  export default MyComponent;
  export type { MyComponentProps };
  ```
* **Use `const` by default**:
  Prefer `const` over `let` unless reassignment is necessary. Never use `var`.
* **Use template literals**:
  For string concatenation, prefer template literals over string concatenation
  with `+`.
* **Prefer arrow functions**:
  Use arrow functions for function expressions, especially for callbacks and
  short utility functions.

## React-Specific Guidelines (`.tsx` files)

* **Use functional components**:
  Prefer functional components with hooks over class components.
* **Define props interfaces**:
  Always define explicit interfaces for component props with JSDoc comments:
  ```typescript
  /**
   * Props for the MyComponent component
   */
  interface MyComponentProps {
    /** The display text */
    text: string;
    /** Optional callback when clicked */
    onClick?: () => void;
  }
  ```
* **Document components with JSDoc**:
  Provide clear JSDoc comments for exported components, describing their purpose
  and usage:
  ```typescript
  /**
   * MyComponent displays a formatted text element.
   *
   * Example usage in MDX files:
   * ```mdx
   * <MyComponent text="Hello World" />
   * ```
   *
   * @param props - the component configuration
   * @returns a formatted text element
   */
  const MyComponent = (props: MyComponentProps) => {
    // ...
  };
  ```

## Documentation

* **Use JSDoc for exported functions and types**:
  All exported functions, interfaces, types, and constants should have JSDoc
  comments describing their purpose and usage.
* **Include `@param` and `@returns` tags**:
  For functions, document parameters and return values:
  ```typescript
  /**
   * Escapes special characters in a string.
   *
   * @param value - the string to escape
   * @returns the escaped string
   */
  const escapeString = (value: string): string => {
    // ...
  };
  ```

## Module Organization

* **Prefer named exports for utilities**:
  For utility functions and constants, use named exports to make imports more
  explicit.
* **Use default exports for components**:
  For React components, use default exports as the primary export, with type
  exports as secondary.
* **Group related constants**:
  Group related constants together at the top of the file or in a separate
  constants object.

## TypeScript Configuration

* **Respect `tsconfig.json` settings**:
  Follow the TypeScript compiler options defined in the project's
  `tsconfig.json`. This project extends `@docusaurus/tsconfig` configuration.
* **Use path aliases when configured**:
  If baseUrl or path mappings are configured in `tsconfig.json`, use them for
  cleaner imports.
