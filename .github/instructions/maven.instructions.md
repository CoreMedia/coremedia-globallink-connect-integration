---
applyTo: "**/pom.xml"
---

# GitHub Copilot Instructions: Maven

These instructions are dedicated to Maven POM files in this repository, applying
to all files matching `**/pom.xml`.

## General Copilot Instruction

When working with Maven POM files, assume that you have expert knowledge in
Maven build tooling, dependency management, and Maven best practices.

## POM Structure & Formatting

* **Follow standard POM order**:
  Maintain the standard Maven POM element order: `modelVersion`, `parent`,
  `groupId`, `artifactId`, `version`, `packaging`, `name`, `description`,
  `properties`, `dependencyManagement`, `dependencies`, `build`,
  `pluginManagement`, `plugins`, `modules`, `profiles`.
* **Use proper indentation**:
  Use two spaces for indentation in POM files, consistent with the project
  standard.
* **Use CDATA for descriptions with special characters**:
  When descriptions contain special characters or formatting, wrap them in
  `<![CDATA[...]]>`:
  ```xml
  <description><![CDATA[
    This module enables support for translating content via
    GlobalLink Connect Cloud REST API.
  ]]></description>
  ```

## Dependency Management

* **Use version properties**:
  Define dependency versions in the `<properties>` section and reference them
  using `${property.name}` syntax.
* **Avoid hardcoded versions in dependencies**:
  When possible, rely on dependency management from a parent POM or define
  versions in properties.
* **Use `<dependencyManagement>` in parent POMs**:
  Define dependency versions in parent POMs using `<dependencyManagement>` to
  ensure version consistency across modules.
* **Minimize transitive dependencies**:
  Be mindful of transitive dependencies and use `<exclusions>` when necessary
  to avoid conflicts.

## Java Version Configuration

* **Read Java version from `maven.compiler.release` property**:
  The Java language level is defined in the root POM's
  `maven.compiler.release` property. Currently set to `21`.
* **Respect project encoding**:
  Use UTF-8 encoding as specified in `project.build.sourceEncoding` and
  `project.reporting.outputEncoding` properties.

## Plugin Configuration

* **Use plugin management in parent POMs**:
  Define plugin versions and common configuration in `<pluginManagement>` in
  parent POMs.
* **Specify plugin versions explicitly**:
  Always specify plugin versions explicitly to ensure reproducible builds.
* **Configure maven-enforcer-plugin**:
  Use the maven-enforcer-plugin to enforce Maven and Java version requirements:
  - Maven version: Currently requires `3.9.11` or higher
  - Java version: Currently requires Java 21 or higher

## Module Structure

* **Use relative paths carefully**:
  When defining `<parent>` relationships, ensure `<relativePath>` is correct
  and points to a location within the project. Never use relative paths that
  point outside the project root (this is validated by the
  `validate-pom-parent-relative-path.sh` script).
* **Group related modules**:
  Use a logical module structure with parent POMs organizing related submodules.

## Best Practices

* **Keep POMs DRY (Don't Repeat Yourself)**:
  Extract common configuration to parent POMs or use properties to avoid
  duplication.
* **Document non-obvious configurations**:
  Use XML comments to explain non-standard configurations or workarounds.
* **Use Maven Central for dependencies**:
  Prefer dependencies from Maven Central over custom repositories when possible.
* **Validate POM changes**:
  After modifying POM files, run `mvn validate` to ensure the POM is
  well-formed and meets enforcer rules.
