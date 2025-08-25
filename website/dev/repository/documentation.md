---
description: "Maintaining GitHub Pages."
---

# Documentation

This project uses [Docusaurus](https://docusaurus.io/) for generating and
deploying documentation as static HTML files to GitHub pages.

## Documentation Workflow

The following diagram shows how documentation versions are managed and deployed:

```mermaid
---
config:
  theme: 'default'
  themeVariables:
      'git0': '#e4d4c8'
      'git1': '#a47786'
      'git2': '#179b17'
      'git3': '#dd342b'
      'git4': '#006cae'
      'git5': '#9db6cc'
      'gitBranchLabel0': '#000000'
      'gitBranchLabel1': '#ffffff'
      'gitBranchLabel2': '#ffffff'
      'gitBranchLabel3': '#ffffff'
      'gitBranchLabel4': '#ffffff'
      'gitBranchLabel5': '#000000'
  gitGraph:
    mainBranchOrder: 97
---
gitGraph:
  branch gh-pages order: 98
  commit id: "orphaned"
  checkout main
  commit id: " "
  commit id: "maintenance/2406.x"
  branch maintenance/2406.x
  commit id: "approve 2406.0" tag: "v2406.0.0-1"
  checkout main
  merge maintenance/2406.x id: "merge 2406.x"
  checkout gh-pages
  merge main id: "publish 2406.0 docs"
  checkout main
  commit id: "  "
  commit id: "maintenance/2412.x"
  branch maintenance/2412.x
  commit id: "approve 2412.0" tag: "v2412.0.0-1"
  checkout main
  merge maintenance/2412.x id: "merge 2412.x"
  checkout gh-pages
  merge main id: "publish 2412.0 docs"
  checkout main
  checkout maintenance/2406.x
  commit id: "approve 2406.1" tag: "v2406.1.0-1"
  checkout main
  commit id: "docs: adapt 2406.1"
  checkout gh-pages
  merge main id: "publish 2406.1 adaptations" type: HIGHLIGHT
  checkout main
  commit id: "   "
  commit id: "maintenance/2506.x"
  branch maintenance/2506.x
  commit id: "2506.0 approval" tag: "v2506.0.0-1"
  checkout main
  merge maintenance/2506.x id: "one-time merge 2506.x"
  checkout gh-pages
  merge main id: "publish 2506.0 docs"
  checkout main
  commit id: "..."
```

The documentation is always only deployed from the `main` branch (performed
automatically by GitHub actions).

For releases based on minor version increments (like `v2406.1.0-1`) the
documentation itself is typically considered **_frozen_**. In other words:
expect no changes to `website/` folder on maintenance branches after the first
approval of the major version.

Do not forget, though, to update the changelog on the `main` branch also for
minor release approvals (as can be seen for highlighted publication named
_publish 2406.1 adaptations_).

:::info INFO: Option to Adapt Maintenance Branches
You are free to decide to still adapt the documentation also on maintenance
branches for more relevant changes. For a specific version, customers of this
workspace may decide to browse it locally via `pnpm start`.
:::

## Documentation Versioning

We do not use documentation versioning as provided as opt-in by Docusaurus.
Instead for documentation of previous releases the recommended approach is to
switch to the corresponding version tag and locally deploy the documentation for
browsing:

```bash
cd website
pnpm install
pnpm start
```
