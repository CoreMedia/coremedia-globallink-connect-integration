# Documentation

This project uses [Docusaurus](https://docusaurus.io/) for generating and maintaining versioned documentation. Docusaurus is a modern static website generator that's particularly well-suited for technical documentation with features like versioning, internationalization, and search.

## Documentation Workflow

The following diagram shows how documentation versions are managed and deployed:

```mermaid
---
config:
  gitGraph:
    mainBranchOrder: 1
---
gitGraph:
  branch gh-pages order: 99
  commit id: "Create orphaned branch"
  checkout main
  commit id: "prior art"
  commit id: "start 2406.0.0"
  branch release/2406.0
  commit id: "docs 2406.0.0"
  commit id: "approve 2406.0.0" tag: "v2406.0.0-1"
  checkout main
  merge release/2406.0 id: "merge 2406.0"
  checkout gh-pages
  merge main id: "publish docs 2406.0"
  checkout main
  commit id: "start 2412.0.0"
  branch release/2412.0
  commit id: "docs 2412.0.0"
  commit id: "approve 2412.0.0" tag: "v2412.0.0-1"
  checkout main
  merge release/2412.0 id: "merge 2412.0"
  checkout gh-pages
  merge main id: "publish docs 2412.0"
  checkout release/2406.0
  commit id: "start 2406.1.0"
  branch release/2406.1
  commit id: "approve 2406.1.0" tag: "v2406.1.0-1"
  checkout main
  commit id: "docs 2406.1.0"
  checkout gh-pages
  merge main id: "publish docs 2406.1"
```

## Documentation Versioning Strategy

### Major Version Documentation Only

Following Docusaurus best practices, we maintain versioned documentation only
for major versions (e.g., `2406.x`, `2412.x`, `2506.x`). This approach:

- **Reduces build complexity**:
  Each documentation version increases build time and bundle size
- **Simplifies maintenance**:
  Fewer versions to keep updated and consistent
- **Focuses on significant changes**:
  Major versions typically introduce substantial changes that warrant separate
  documentation

Think of this approach as just another _embedded Version Control System_. Thus,
on `main` the _documentation branches_ will look like this:

```mermaid
---
title: Embedded Documentation VCS
config:
  theme: 'forest'
  gitGraph:
    mainBranchName: docs
    showCommitLabel: false
---
gitGraph:
  commit id: "prepare 2406.0.0 docs"
  commit id: "3rd-party 2406.0.0"
  commit id: "create version-2406.x"
  branch version-2406.x
  commit id: "version-2406.x"
  checkout docs
  commit id: "prepare 2412.0.0 docs"
  commit id: "3rd-party 2412.0.0"
  commit id: "create version-2412.x"
  branch version-2412.x
  commit id: "version-2412.x"
  checkout version-2406.x
  commit id: "2406.1.0 docs"
  checkout docs
  commit id: "NO 3rd-party 2406.1.0" type: REVERSE
  commit id: "prepare 2506.0.0 docs"
  commit id: "3rd-party 2506.0.0"
  commit id: "create version-2506.x"
  branch version-2506.x
  commit id: "version-2506.x"
  checkout docs
  commit id: "more commits"
```

### Documentation Update Process

- **All documentation updates happen on `main`**: Whether fixing typos, updating
  procedures for new approvals (like `2406.1.0`), or adding new features, all
  changes must be made on the main branch.

  This includes updating the overview of used third-party via

  ```bash
  mvn -Pdocs-third-party generate-resources
  ```

  :::note
  This approach has a glitch: For updates on maintenance branches we will not
  (necessarily) update the used third-party overview (because all updates will
  just happen on the _main_ documentation branch). If you still want or need to
  update these reports, you have to manually apply them to the versioned
  documentation branch.
  :::

- **Release branches have static documentation**: Documentation sources on
  `release/` branches (e.g., `release/2406.0`, `release/2412.0`) are not
  maintained after creation. They serve only for building the specific version
  releases.

- **Versioned snapshots before major releases**: Before creating a new major
  release branch, a documentation version snapshot is created on main. This
  captures the documentation state for that major version.

- **GitHub Pages deployment from `main` only**: The `gh-pages` branch is updated
  exclusively from the main branch, which contains all version snapshots and
  serves as the single source of truth for documentation deployment.

### Benefits for Developers

This workflow ensures that:

- Documentation improvements benefit all relevant versions
- No need to cherry-pick documentation fixes across multiple branches
- Clear separation between code releases and documentation maintenance
- Consistent documentation experience across all deployed versions

:::info
The future is yet to be designed. The more versions we ship, the more complex
the documentation will get. The suggestion is to archive not maintained versions
later, like not only removing the corresponding `release/*` branch, but also
removing the versioned documentation. For now, refer to the official Docusaurus
documentation, how to proceed with this scenario (and think about updating
this documentation).
:::
