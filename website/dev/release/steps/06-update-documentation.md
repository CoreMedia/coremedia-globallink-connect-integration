---
description: Updating Documentation Deployed at GitHub Pages.
tags:
  - development
  - contributors
  - release
  - docusaurus
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Update Documentation

The documentation below `website/` comes in two parts. While `website/dev/`
contains documentation related to this repository (like this section here),
`website/docs/` contains the documentation for using the
CoreMedia GlobalLink Connect Integration. This section is about updating the
latter.

:::info INFO: Auto-Deployment of Documentation
The documentation is deployed to an extra branch `gh-pages`. It is generated
by [Docusaurus](https://docusaurus.io/) and located within the `website/`
folder of this repository.

Deployment is done automatically from `main` branch, as soon as changes have
been merged.
:::

## a) CoreMedia Documentation Links

For reference towards
[documentation.coremedia.com](https://documentation.coremedia.com/) the
documentation should use the MDX Component `<DocLink>`. If all documentation
adheres to this pattern, updating versioned links just requires updating
<RepositoryLink path="website/src/ts/context.ts"/>.

## b) Content Revision

On each CMCC version update, we need to check if the documentation for
administrators, editors and developers are still aligned with the product.

Unless we introduced new behaviors to the GCC integration, or the integration
into CoreMedia Blueprint has to be adapted, there is nothing to be done here
most of the time.

## c) Screenshot Revision

:::info INFO: Skip for Minor Release Approval
Unless it is about documenting new features or behaviors, aligning screenshots,
for example, to a new visual identity of CoreMedia, should just be considered
for approval of new major versions.
:::

The section _"Editors"_ contains screenshots, that may require an update.
Consider reading the corresponding [How-to](../../howto/screenshots.md) to guide
you through the process (like resolutions to choose, etc.).

## d) Third-Party Reports

Used third-party libraries must be reported and their licenses integrated into
the documentation (as at least some licenses require this). We only maintain
these reports for our Maven/Java based modules.

To create a new report:

* Ensure to have built the CMCC branch before (as most dependencies will be
  taken from corresponding parents or BOMs).

* Run:

  ```bash
  mvn -Pdocs-third-party generate-resources
  ```

  This will update the auto-generated file
  `website/docs/third-party/third-party.md` as well as adding downloaded
  licenses to `website/docs/third-party/files`.

:::note NOTE: Customizing Report
For generating the Markdown report `third-party.md` the FreeMarker template
`src/main/templates/third-party-md.ftl` is used.
:::

## e) Add Changelog Entry (Mj, Mn)

The release notes at
[GitHub/Releases](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases)
and in
[CHANGELOG.md](https://github.com/CoreMedia/coremedia-globallink-connect-integration/blob/main/CHANGELOG.md)
typically represent just an excerpt of all changes applied.

A more verbose changelog is maintained at
<RepositoryLink path="website/docs/changelog/"/>.
Add a corresponding file to represent the new version.

:::info INFO: Only Apply to Main Branch
For convenience the changelog should only be applied to the changes that make
it to the `main` branch. Thus, for major version approvals, you can directly
apply it on the later merged `maintenance/MMMM.x` branch, for minor version
approvals, you need to apply it on an extra pull request with base `main`.
:::

## f) Update CHANGELOG.md (Mj, Mn)

As an excerpt of the previous changes (and as main entry-point), update
<RepositoryLink path="CHANGELOG.md"/>.

These entries will later be used for the release note at
[GitHub/Releases](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases).

## g) Update README.md (Mj, mn)

:::info Optional for Minor Release Approval
On the `main` branch, we always only represent the most recent versions.
For consistency, you may consider adapting the `README.md` on the corresponding
`maintenance/*` branch. But it is totally optional.
:::

Update version badges at
<RepositoryLink path="README.md"/>.
