# AGENTS.md

Guidance for AI coding agents (and human contributors skimming for the fast
path) working in this repository. This file complements, but does not
replace, [.github/copilot-instructions.md](.github/copilot-instructions.md)
and the per-file-type rules in [.github/instructions/](.github/instructions/),
which remain the authoritative source for coding-style rules. Read those
before making changes to matching files.

## What This Repository Is

An open-source CoreMedia Content Cloud (CMCC) extension that integrates
**GlobalLink Connect Cloud (GCC)**, a third-party REST API (by
Translations.com/TransPerfect), to let editors send content for translation,
track its progress, and import the finished result — all from within
CoreMedia Studio.

This repository is **public** and is meant as a **showcase / reference
implementation**: customers copy, fork, or vendor it into their own CoreMedia
projects and adapt it further. Keep this in mind for every change:

* Nothing you add (code, comments, docs, commit messages) should be
  something that must not be visible to customers or the public.
* Favor simple, well-documented solutions (KISS) over clever ones — the
  people maintaining forks of this code may not be CoreMedia workflow
  experts.
* Changes should generalize; avoid solutions that only work for
  CoreMedia-internal setups.

## Module Map (Where to Look)

* `apps/studio-client/` — TypeScript/React Studio UI extension (pnpm
  workspace).
* `apps/studio-server/` — Studio REST backend additions (Java).
* `apps/user-changes/` — Support module for content user-change handling
  (Java).
* `apps/workflow-server/` — The core translation logic (Java):
  * `gcc-workflow-server/` — Workflow definition
    (`translation-global-link.xml`, mirrored in `GCC-Workflow.bpmn`) and the
    Java workflow actions (`*GlobalLinkAction`) that drive it.
  * `gcc-workflow-server-facade/` — Pluggable facades that abstract the
    actual GCC REST/Java client (`default`, `disabled`, `mock` variants).
  * `gcc-workflow-server-util/` — Shared utilities, e.g. `RetryDelay`,
    `Settings`.
* `content/` — Content archive (e.g., Settings documents) shipped with the
  extension.
* `website/` — Docusaurus documentation site (published to GitHub Pages).
  * `website/docs/` — Customer-facing docs (editors, administrators,
    integrators); version-specific, maintained per `maintenance/MMMM.x`
    branch.
  * `website/dev/` — Contributor-facing docs (branching model, release
    process, howtos); maintained only on `main`.

## Where the Rules Live

| File type                                          | Rules file                                                              |
|----------------------------------------------------|-------------------------------------------------------------------------|
| `**/*.java`                                        | `.github/instructions/java.instructions.md`                             |
| `**/src/test/java/**/*.java`                       | `.github/instructions/java-test.instructions.md` (in addition to above) |
| `**/*.ts`, `**/*.tsx`                              | `.github/instructions/typescript.instructions.md`                       |
| `**/pom.xml`                                       | `.github/instructions/maven.instructions.md`                            |
| `**/package.json`, `**/pnpm-*.yaml`                | `.github/instructions/pnpm.instructions.md`                             |
| `**/*.md`                                          | `.github/instructions/markdown.instructions.md`                         |
| `**/*.java`, `**/*.md`, `**/*.mdx` (Javadoc/prose) | `.github/instructions/documentation.instructions.md`                    |
| `**/*.sh`                                          | `.github/instructions/bash.instructions.md`                             |

Highlights worth remembering without opening every file:

* Java language level is **21**, read from `maven.compiler.release` in the
  root `pom.xml`; do not use `var` (explicit team decision).
* Annotate top-level Java types with `@org.jspecify.annotations.NullMarked`
  and use `@Nullable` explicitly where needed.
* Tests use **JUnit 6** and **AssertJ**; prefer `assertThat(...)` over
  JUnit's own assertions, and `should...` test method names.
* Use `pnpm` (never plain `npm`) in `apps/studio-client/` and `website/`.

## Building and Testing

* Java modules: standard Maven build from the repository root
  (`mvn install`, or per-module `pom.xml`). Respect
  `.github/instructions/maven.instructions.md` when touching POMs.
* Studio client (`apps/studio-client/`) and the website
  (`website/`): pnpm workspaces; run `pnpm install` inside the respective
  workspace root before other pnpm commands.

## Configuration Model (Frequently Relevant)

Most runtime behavior (GCC connection, retry delays, submission behavior) is
governed by `Settings`, merged with increasing precedence from:

1. Spring properties (`gcc-workflow.properties`, `gcc.*` keys).
2. Global content settings (`/Settings/Options/Settings/Translation
   Services`).
3. Site-specific content settings
   (`<SITE_ROOT>/Options/Settings/Translation Services`).

Any new configuration option should follow this same layering, be documented
in `website/docs/administrators/configure-gcc-settings.mdx`, and — if it
is a duration/delay — reuse `RetryDelay` for parsing and bounds enforcement
rather than inventing a new format.

## Documentation Conventions

* `website/docs/` is customer-facing and version-specific; update it when
  behavior visible to customers/administrators/editors changes.
* `website/dev/` is contributor-facing and branch-agnostic (`main` only);
  update it for process/tooling changes relevant to contributors.
* `KNOWN_ISSUES.md` (repository root) tracks known bugs and open questions
  not yet resolved.
* See [CONTRIBUTING.md](CONTRIBUTING.md) for the contribution process
  (branching, PRs, commit expectations).

## Working with Temporary/Scratch Files

Investigative or planning artifacts (knowledge bases, task tracking, etc.)
belong under `tmp/`, which is excluded from version control via
`.gitignore`. Do not rely on `tmp/` content persisting across clones or being
shared with reviewers — promote anything that should survive into
`website/docs`, `website/dev`, `KNOWN_ISSUES.md`, or code comments/Javadoc as
appropriate.
