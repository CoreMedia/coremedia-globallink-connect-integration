---
description: Updating the README.
tags:
  - development
  - contributors
  - release
  - docusaurus
  - readme
---

# Update README

**This step may be skipped for minor version approvals,** as it only about
updating the version badges in <RepositoryLink path="README.md"/>. This again
is more relevant to provide valid information for the `main` branch when
visitors navigate to the repository at GitHub.

Here is an example of the version badges:

````markdown
![Latest Release Version](https://img.shields.io/github/v/release/CoreMedia/coremedia-globallink-connect-integration?style=for-the-badge&filter=v*&sort=semver&logo=semanticrelease&label=Latest&color=672779)
![GCC Used](https://img.shields.io/badge/GCC_REST_API_(used)_-v3.1.3-198754?style=for-the-badge&logo=semanticrelease)
![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient?label=GCC%20REST%20API%20(recent)&style=for-the-badge&logo=semanticrelease&color=0d6efd)
````

Rendered as:

![Latest Release Version](https://img.shields.io/github/v/release/CoreMedia/coremedia-globallink-connect-integration?style=for-the-badge&filter=v*&sort=semver&logo=semanticrelease&label=Latest&color=672779)
![GCC Used](https://img.shields.io/badge/GCC_REST_API_(used)_-v3.1.3-198754?style=for-the-badge&logo=semanticrelease)
![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient?label=GCC%20REST%20API%20(recent)&style=for-the-badge&logo=semanticrelease&color=0d6efd)

Only the badge for _GCC Used_ is static and requires to be adapted. All other
badges are dynamic and should not require to be updated.

:::tip TIP: Shields.io
For details on parameters and how to create badges, visit
[Shields.io](https://shields.io/).
:::
