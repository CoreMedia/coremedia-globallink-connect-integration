---
title: Update Third-Party
description: Update Third-Party Dependencies.
tags:
  - development
  - contributors
  - release
  - third-party
---

# Verify or Update Third-Party Dependency Versions

It is considered best practice, to align third-party dependencies with the
corresponding versions of the CMS. For Maven, as we integrate as an extension,
we typically inherit dependency versions from dependency management in parent
POMs or via BOM imports.

For `apps/studio-client` we should (or must) align dependency versions. This
especially applies to the Jangaroo tooling (artifacts such as
`@jangaroo/build`, `@jangaroo/core` and alike), that should use the same version
as within the CMCC workspace.
