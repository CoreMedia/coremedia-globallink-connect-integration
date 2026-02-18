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
![Current Release Version](https://img.shields.io/badge/Current-V2512.0.0--1-672779?style=for-the-badge&logo=semanticrelease)
[![Latest Release Version](https://img.shields.io/github/v/release/CoreMedia/coremedia-globallink-connect-integration?style=for-the-badge&filter=v*&sort=semver&logo=semanticrelease&label=Latest&color=363936)](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases) \
![CoreMedia Content Cloud Version](https://img.shields.io/badge/CMCC-V2512.0.0-198754?style=for-the-badge&logo=semanticrelease)
![GCC Used](https://img.shields.io/badge/GCC_REST_(current)_-v3.1.9-198754?style=for-the-badge&logo=semanticrelease)
[![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient?label=GCC%20REST%20(latest)&style=for-the-badge&logo=semanticrelease&color=363936)](https://central.sonatype.com/artifact/com.translations.globallink/gcc-restclient) \
![Java 21](https://img.shields.io/badge/Java-21-006cae?style=for-the-badge&logo=openjdk)
![Maven 3.9.11](https://img.shields.io/badge/Maven-3.9.11-dd3428?style=for-the-badge&logo=apachemaven) \
[![GitHub Pages](https://img.shields.io/badge/GitHub_Pages-265a53?style=for-the-badge&logo=githubpages)](https://coremedia.github.io/coremedia-globallink-connect-integration/)
````

Rendered as:

![Current Release Version](https://img.shields.io/badge/Current-V2512.0.0--1-672779?style=for-the-badge&logo=semanticrelease)
[![Latest Release Version](https://img.shields.io/github/v/release/CoreMedia/coremedia-globallink-connect-integration?style=for-the-badge&filter=v*&sort=semver&logo=semanticrelease&label=Latest&color=363936)](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases) \
![CoreMedia Content Cloud Version](https://img.shields.io/badge/CMCC-V2512.0.0-198754?style=for-the-badge&logo=semanticrelease)
![GCC Used](https://img.shields.io/badge/GCC_REST_(current)_-v3.1.9-198754?style=for-the-badge&logo=semanticrelease)
[![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient?label=GCC%20REST%20(latest)&style=for-the-badge&logo=semanticrelease&color=363936)](https://central.sonatype.com/artifact/com.translations.globallink/gcc-restclient) \
![Java 21](https://img.shields.io/badge/Java-21-006cae?style=for-the-badge&logo=openjdk)
![Maven 3.9.11](https://img.shields.io/badge/Maven-3.9.11-dd3428?style=for-the-badge&logo=apachemaven) \
[![GitHub Pages](https://img.shields.io/badge/GitHub_Pages-265a53?style=for-the-badge&logo=githubpages)](https://coremedia.github.io/coremedia-globallink-connect-integration/)

Typically adapted:

* Current release version
* CoreMedia Content Cloud version
* GCC REST Java client version (used)

And sometimes also:

* Java version
* Maven version

:::tip TIP: Shields.io
For details on parameters and how to create badges, visit
[Shields.io](https://shields.io/).
:::
