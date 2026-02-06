---
description: Updating the Changelog.
tags:
  - development
  - contributors
  - release
  - docusaurus
  - changelog
---

# Update Changelog

## No-Operation: CHANGELOG.md

Starting in August 2025, we dropped updating
<RepositoryLink path="CHANGELOG.md"/>. This is because this is not feasible
for parallel maintenance releases.

Instead, `CHANGELOG.md` forwards to the only relevant information on changes.

## Website Changelog

We maintain a changelog which is meant as the first-level citizen changelog
within the website at
<RepositoryLink path="website/docs/changelog/"/>.

Just add a new entry like `v12.2412.0.0-1.md` that describes the changes
as well as, if required, the upgrade steps.

The changes description at
[GitHub/Releases](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases)
is most of the time just an excerpt of this information.

For minor releases, changelog entries are still recommended, although they
will never be published. Instead, they exist for locally browsing the changelog
within the repository or if the website is deployed locally via `pnpm start`
from a given release tag.

:::note NOTE: Reverse Sorting
For the changelog we have defined sorting as `descending` in the corresponding
`_category_.json`. This is backed by a `sidebarItemsGenerator` in
`docusaurus.config.ts`. Note, that it only considers alphabetic sorting and is
not aware of the numbers within the filename.
:::
