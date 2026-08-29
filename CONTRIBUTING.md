![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

# Contributing

Thank you for your interest in contributing to the **CoreMedia GlobalLink
Connect Cloud Integration**! This document is the short, human- and
AI-readable entry point for contributions. For more, see
[AGENTS.md](AGENTS.md) (rules for AI coding agents; also a good quick
orientation for humans) and the detailed
[contributor documentation](https://coremedia.github.io/coremedia-globallink-connect-integration/dev/home).

## Before You Start

* This repository is public and is used by customers as a showcase to copy,
  fork, or vendor into their own projects.
* Read [.github/copilot-instructions.md](.github/copilot-instructions.md)
  and the linked [.github/instructions/](.github/instructions/) files for
  coding style rules per file type (Java, TypeScript, Maven, pnpm, Markdown,
  Bash, documentation).

## Reporting Issues

Please use the [issue tracker](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues)
and, if applicable, check [KNOWN_ISSUES.md](KNOWN_ISSUES.md) first to avoid
duplicates.

## Branching Model

* Target the `main` branch with pull requests for new features and fixes.
* Version-specific maintenance work happens on `maintenance/MMMM.x` branches.

See [Branches](https://coremedia.github.io/coremedia-globallink-connect-integration/dev/repository/branches)
for details.

## Making Changes

1. Fork the repository and create a feature branch from `main`.
2. Follow the module layout and coding rules described in
   [AGENTS.md](AGENTS.md).
3. Add or update tests alongside your change
   (see `.github/instructions/java-test.instructions.md` for Java tests).
4. Update relevant documentation:
   * [website/docs/](website/docs/) for customer-facing behavior changes.
   * [website/dev/](website/dev/) for contributor-facing/process changes.
   * [KNOWN_ISSUES.md](KNOWN_ISSUES.md) if you are documenting a known
     limitation rather than fixing it outright.
5. Open a pull request describing the change and its motivation.

## Building Locally

* Java modules: build with Maven from the repository root
  (`mvn install`), following
  [.github/instructions/maven.instructions.md](.github/instructions/maven.instructions.md).
* Studio client (`apps/studio-client/`) and the documentation site
  (`website/`) are pnpm workspaces; run `pnpm install` in the respective
  workspace root, following
  [.github/instructions/pnpm.instructions.md](.github/instructions/pnpm.instructions.md).

## Code of Conduct

Be respectful and constructive. We value feedback on use cases and further
developments, and we love to review and integrate pull requests.
