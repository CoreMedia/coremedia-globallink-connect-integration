# GitHub Copilot Instructions

This file contains instructions for GitHub Copilot. For better organization,
detailed instructions are split into separate files in the `instructions/`
directory.

> **Note: Tool Compatibility**
>
> Some tools only support this central file. If your tool doesn't automatically
> read the linked instruction files below, you may need to manually reference
> the specific instruction files in the `instructions/` directory.

## Instruction Files

The following instruction files contain detailed rules for different aspects
of the codebase:

### [Documentation Instructions](instructions/documentation.instructions.md)

**Applies to:** `**/*.md`, `**/*.mdx`, Javadoc in `**/*.java`

Key rules: AP Stylebook compliance, Javadoc formatting, technical writing
standards

### [Markdown Instructions](instructions/markdown.instructions.md)

**Applies to:** `**/*.md`

Key rules: Markdown syntax, structure, formatting, GitHub Flavored Markdown

### [Java Instructions](instructions/java.instructions.md)

**Applies to:** `**/*.java`

Key rules: Java 21 language level, coding standards, best practices, null-safety

### [Java Test Instructions](instructions/java-test.instructions.md)

**Applies to:** `**/src/test/java/**/*.java`

Key rules: JUnit 6 test framework, AssertJ assertions, test organization

### [TypeScript Instructions](instructions/typescript.instructions.md)

**Applies to:** `**/*.ts`, `**/*.tsx`

Key rules: TypeScript best practices, React components, type safety, ESLint
compliance

### [Maven Instructions](instructions/maven.instructions.md)

**Applies to:** `**/pom.xml`

Key rules: POM structure, dependency management, plugin configuration, Java 21
requirement

### [pnpm & Package.json Instructions](instructions/pnpm.instructions.md)

**Applies to:** `**/package.json`, `**/pnpm-*.yaml`

Key rules: pnpm workspaces, dependency management, package configuration, Node.js
version requirements

### [Bash Shell Script Instructions](instructions/bash.instructions.md)

**Applies to:** `**/*.sh`

Key rules: Bash best practices, strict mode, error handling, portability
