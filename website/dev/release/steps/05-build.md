---
title: Build
description: Compile Time Checks
tags:
  - development
  - contributors
  - release
  - build
---

# Build with Strict Checks

By default, strict checks are disabled at compile time, as this would also
enforce customers to straighten their code when they update to a new GCC
version. However, for the release process, it is required to build the project
with strict checks, which especially (but not only) include nullability checks.
This is to ensure that the code quality is as high as possible and that no
warnings are present in the codebase.

Ensure running the build with the `gcc-strict-checks` profile activated, either
locally or within the CI.
