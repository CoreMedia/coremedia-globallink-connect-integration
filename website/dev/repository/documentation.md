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
  commit id: "adapt 2406.1 docs" type: HIGHLIGHT
  commit id: "approve 2406.1" tag: "v2406.1.0-1"
  checkout main
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

## Previous Versions

For any documentation for a given approved/released version, the documentation
site should be deployed locally for browsing:

```bash
cd website
pnpm install
pnpm start
```

## docs/ vs. dev/

<RepositoryLink path="website/docs/" /> contains version specific documentation,
while <RepositoryLink path="website/dev/" /> is not related to any version.
Thus, typically `dev/` is only maintained on `main` while `docs` is maintained
also on the maintenance branches.

## Changelogs

We maintain two changelogs:

* <RepositoryLink path="website/docs/changelog/" /> contains a changelog with
  many details like upgrade information.

* [Release Description](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases)
  is meant to contain an overview of the applied changes.

While the changelog within the website is only maintained for sequential
releases (thus, `maintenance/2406.x` only contains changes relevant for
`2406.0`, `2406.1`, etc.), the _Releases_ changelog provides an overview of all
releases.

As a result for the website, the changelog deployed to GitHub pages (published
from `main`) always only contains changes along major version approvals.

:::note NOTE: CHANGELOG.md
In August 2025 we dropped a central `CHANGELOG.md` at repository root. Instead
the releases must contain a corresponding changelog and a more detailed
changelog with upgrade information is maintained
at <RepositoryLink path="website/docs/changelog/" />.
:::

## Third-Party Reports

For each release/approval we also generate the third-party reports. Just as
for the changelogs, as GitHub pages is only deployed from `main`, the published
reports only contain the report for the latest approved major version.

For maintenance versions, we still update the third-party report, but it is
only available for repository or local browsing.
