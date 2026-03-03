---
applyTo: "**/package.json,**/pnpm-*.yaml"
---

# GitHub Copilot Instructions: pnpm & package.json

These instructions are dedicated to pnpm and Node.js package configuration files
in this repository, applying to all files matching `**/package.json` and
`**/pnpm-*.yaml` (including `pnpm-workspace.yaml` and `pnpm-lock.yaml`).

## General Copilot Instruction

When working with `package.json` and pnpm configuration, assume that you have
expert knowledge in Node.js package management, pnpm workspaces, and npm/pnpm
best practices.

## Package.json Structure

* **Follow standard package.json order**:
  Maintain a consistent order of fields: `name`, `version`, `description`,
  `author`, `engines`, `private`, `keywords`, `scripts`, `dependencies`,
  `devDependencies`, `peerDependencies`.
* **Specify engine requirements**:
  Always specify Node.js and pnpm version requirements in the `engines` field:
  ```json
  "engines": {
    "node": ">=24",
    "pnpm": ">=10.24.0 <11"
  }
  ```
* **Use semantic versioning**:
  Follow semantic versioning (semver) for version numbers.

## pnpm Workspace Configuration

* **Use pnpm workspaces**:
  This project uses pnpm workspaces for managing multiple packages. Respect the
  workspace configuration in `pnpm-workspace.yaml`.
* **Avoid modifying pnpm-lock.yaml manually**:
  Never manually edit `pnpm-lock.yaml`. Let pnpm manage it automatically.
* **Use workspace protocol**:
  For internal workspace dependencies, use the workspace protocol:
  ```json
  "dependencies": {
    "@workspace/package": "workspace:*"
  }
  ```

## Dependency Management

* **Use catalog references where configured**:
  This project uses pnpm catalog feature. When dependencies use `catalog:`,
  respect this pattern:
  ```json
  "dependencies": {
    "@docusaurus/core": "catalog:"
  }
  ```
* **Separate dependencies by type**:
  - `dependencies`: Runtime dependencies
  - `devDependencies`: Development-only dependencies
  - `peerDependencies`: Dependencies that should be provided by the consumer
* **Keep dependencies up to date**:
  Regularly update dependencies using `pnpm update --interactive --latest`.
* **Avoid duplicate dependencies**:
  Use `pnpm dedupe` to remove duplicate dependencies when possible.

## Scripts

* **Use descriptive script names**:
  Script names should clearly describe what they do (e.g., `build`, `start`,
  `typecheck`).
* **Use preinstall hook when needed**:
  This project uses a `preinstall` script to enforce pnpm usage:
  ```json
  "scripts": {
    "preinstall": "node ./scripts/check-pnpm.mjs"
  }
  ```
* **Chain scripts appropriately**:
  Use `&&` to chain commands that must run sequentially, or separate scripts
  for better clarity.
* **Document complex scripts**:
  Add comments in README or inline documentation for non-obvious scripts.

## Version Constraints

* **Use appropriate version ranges**:
  - Exact version: `"1.0.0"` (avoid except for known compatibility issues)
  - Caret range: `"^1.0.0"` (allow minor and patch updates)
  - Tilde range: `"~1.0.0"` (allow patch updates only)
  - Greater than or equal: `">=1.0.0"` (use with upper bound: `">=1.0.0 <2"`)
* **Lock production dependencies**:
  For production code, prefer exact versions or narrow ranges to ensure
  reproducible builds.

## Package Metadata

* **Include complete author information**:
  ```json
  "author": {
    "name": "CoreMedia GmbH",
    "email": "info@coremedia.com",
    "url": "https://coremedia.com/"
  }
  ```
* **Add keywords for discoverability**:
  Include relevant keywords for package discoverability.
* **Set private flag for non-published packages**:
  If the package should not be published to npm, set `"private": true`.

## Best Practices

* **Validate package.json**:
  Ensure package.json is valid JSON. Use tools like `pnpm install --dry-run` to
  validate configuration.
* **Keep package.json clean**:
  Remove unused dependencies and scripts regularly.
* **Use pnpm commands**:
  Always use pnpm commands instead of npm for this project:
  - `pnpm install` instead of `npm install`
  - `pnpm add` instead of `npm install <package>`
  - `pnpm remove` instead of `npm uninstall`
* **Check for security vulnerabilities**:
  Regularly run `pnpm audit` to check for security vulnerabilities in
  dependencies.
