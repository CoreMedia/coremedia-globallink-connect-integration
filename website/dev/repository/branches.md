---
description: "Overview of Branches main, gh-pages, etc."
---

# Branches

Overview of Branches `main`, `maintenance/MMMM.x` and `gh-pages`.

## Branch Overview

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
  checkout maintenance/2412.x
  commit id: "2412.0 bugfix" tag: "v2412.0.0-2"
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

## Branch Details

### main

The primary development branch where ongoing work for future releases takes
place. All new feature development and pull requests should target this branch.
New major release branches (e.g., `maintenance/2406.x`, `maintenance/2412.x`)
are created from `main` when a new major version of the underlying platform
needs to be prepared.

### maintenance/MMMM.x

Major Version specific release branches created for each major version of the
underlying platform. It is created as soon as the approval for the next major
version starts.

Approvals for minor version within the same major version (e.g., `2406.1`,
`2406.2`) are created from the respective `maintenance/MMMM.x` branch. These
branches receive approval-specific changes, dependency updates, and API
adaptations.

Release tags are always applied to maintenance branches only.

Bug fix releases (in context of issues within this integration) typically
increase the last digit of the tag (e.g., `v2406.0.0-2`, `v2406.0.0-3`).

### gh-pages

An orphaned branch used to publish GitHub Pages website. For details and how it
interacts with the release/approval process, see the
[Documentation section](./documentation.md).

## Previous Branch Model

Until approval of 2406.1.0 and 2412.0.0 we stick to a Git-Flow process. Starting
with 2406.2.0 in August 2025, we changed to this new branching model. That is
why the actual branches may not match the graph above.
