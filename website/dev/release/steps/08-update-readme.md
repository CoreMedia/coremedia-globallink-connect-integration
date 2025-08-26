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
![CoreMedia Content Cloud Version](https://img.shields.io/static/v1?message=2412.0&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779)
![GCC Used](https://img.shields.io/static/v1?message=v3.1.3&label=GCC%20REST%20API%20%28Used%29&style=for-the-badge&color=green)
[![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient.svg?label=GCC%20REST%20API%20%28Recent%29&style=for-the-badge)](https://central.sonatype.com/search?q=com.translations.globallink%3Agcc-restclient)
````

Rendered as:

![CoreMedia Content Cloud Version](https://img.shields.io/static/v1?message=2412.0&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779)
![GCC Used](https://img.shields.io/static/v1?message=v3.1.3&label=GCC%20REST%20API%20%28Used%29&style=for-the-badge&color=green)
[![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient.svg?label=GCC%20REST%20API%20%28Recent%29&style=for-the-badge)](https://central.sonatype.com/search?q=com.translations.globallink%3Agcc-restclient)

Only the first two `message` values need to be validated, where the first is
the latest approved CMCC version and the second is the latest used GCC
REST API.

:::tip TIP: Shields.io
For details on parameters and how to create badges, visit
[Shields.io](https://shields.io/).
:::
