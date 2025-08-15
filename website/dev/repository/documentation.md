# Documentation

This project uses [Docusaurus](https://docusaurus.io/) for generating and maintaining versioned documentation. Docusaurus is a modern static website generator that's particularly well-suited for technical documentation with features like versioning, internationalization, and search.

## Documentation Workflow

The following diagram shows how documentation versions are managed and deployed:

```mermaid
gitGraph:
  commit id: "ongoing dev"
  commit id: "doc snapshot 2406.x"
  commit tag: "v2406.0.0-1"
  branch release/2406.0
  branch gh-pages
  checkout gh-pages
  commit id: "deploy 2406.x docs"
  checkout main
  commit id: "development"
  commit id: "doc snapshot 2412.x"
  commit tag: "v2412.0.0-1"
  branch release/2412.0
  checkout gh-pages
  commit id: "deploy 2412.x docs"
  checkout main
  commit id: "doc fixes"
  checkout gh-pages
  commit id: "update docs"
  checkout main
  commit id: "doc snapshot 2506.x"
  commit tag: "v2506.0.0-1"
  branch release/2506.0
  checkout gh-pages
  commit id: "deploy 2506.x docs"
  checkout main
  commit id: "continue dev"
```

## Documentation Versioning Strategy

### Major Version Documentation Only

Following Docusaurus best practices, we maintain versioned documentation only for major versions (e.g., `2406.x`, `2412.x`, `2506.x`). This approach:

- **Reduces build complexity**: Each documentation version increases build time and bundle size
- **Simplifies maintenance**: Fewer versions to keep updated and consistent
- **Focuses on significant changes**: Major versions typically introduce substantial changes that warrant separate documentation

### Documentation Update Process

- **All documentation updates happen on `main`**: Whether fixing typos, updating procedures for new approvals (like `2406.1.0`), or adding new features, all changes must be made on the main branch.

- **Release branches have static documentation**: Documentation sources on `release/` branches (e.g., `release/2406.0`, `release/2412.0`) are not maintained after creation. They serve only for building the specific version releases.

- **Versioned snapshots before major releases**: Before creating a new major release branch, a documentation version snapshot is created on main. This captures the documentation state for that major version.

- **GitHub Pages deployment from `main` only**: The `gh-pages` branch is updated exclusively from the main branch, which contains all version snapshots and serves as the single source of truth for documentation deployment.

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
