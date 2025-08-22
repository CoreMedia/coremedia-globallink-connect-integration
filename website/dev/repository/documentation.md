# Documentation

This project uses [Docusaurus](https://docusaurus.io/) for generating and
deploying documentation as static HTML files to GitHub pages.

:::note
While Docusaurus also provides the option to maintain multiple versions, we
dropped the idea in August 2025 to make use of this feature due to the
complexity of maintaining such a versioned documentation.
:::

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

The documentation is always only deployed from the `main` branch (performed
automatically by GitHub actions).

For releases based on minor version increments (like `v2406.1.0-1`) the
documentation itself is considered **_frozen_**. Changes for minor version
increments are never merged back to `main` again. **The only exception here:**
Changelog entries (`website/dev/changelog`) are always required to be applied
to `main`. It is up to you, if you just apply them to `main` or cherry-pick
them from the corresponding release branch.

## Documentation Versioning

As stated above, we do not use documentation versioning as provided as Opt-In
by Docusaurus. Instead for documentation of previous releases the recommended
approach is to switch to the corresponding version tag and locally deploy
the documentation for browsing:

```bash
cd website
pnpm install
pnpm start
```
