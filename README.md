![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

<!--
  On Update review and adapt the following badges:
     * (L12) Current Release Version: The version of the GCC integration release, e.g. `v2512.0.0-1`.
     * (L14) CoreMedia Content Cloud Version: The version of CMCC that is supported by the current release, e.g. `V2512.0.0`.
     * (L15) GCC Used: The version of the GCC REST client used in the current release, e.g. `v3.1.3`.
     * (L17) Java Version: The Java version required to run the GCC REST client, e.g. `Java 21`.
     * (L18) Maven Version: The Maven version required to build the project, e.g. `Maven 3.9.11`.
-->

![Current Release Version](https://img.shields.io/badge/Current-V2512.0.0--1-672779?style=for-the-badge&logo=semanticrelease)
[![Latest Release Version](https://img.shields.io/github/v/release/CoreMedia/coremedia-globallink-connect-integration?style=for-the-badge&filter=v*&sort=semver&logo=semanticrelease&label=Latest&color=363936)](https://github.com/CoreMedia/coremedia-globallink-connect-integration/releases) \
![CoreMedia Content Cloud Version](https://img.shields.io/badge/CMCC-V2512.0.0-198754?style=for-the-badge&logo=semanticrelease)
![GCC Used](https://img.shields.io/badge/GCC_REST_(current)_-v3.1.3-198754?style=for-the-badge&logo=semanticrelease)
[![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient?label=GCC%20REST%20(latest)&style=for-the-badge&logo=semanticrelease&color=363936)](https://central.sonatype.com/artifact/com.translations.globallink/gcc-restclient) \
![Java 21](https://img.shields.io/badge/Java-21-006cae?style=for-the-badge&logo=openjdk)
![Maven 3.9.11](https://img.shields.io/badge/Maven-3.9.11-dd3428?style=for-the-badge&logo=apachemaven) \
[![GitHub Pages](https://img.shields.io/badge/GitHub_Pages-265a53?style=for-the-badge&logo=githubpages)](https://coremedia.github.io/coremedia-globallink-connect-integration/)

# Translation via GlobalLink Connect Cloud

This open-source workspace enables CoreMedia CMS to communicate with GlobalLink
Connect Cloud (GCC) REST API in order to send content to be translated, query
the translation status and to update content with the received translation
result eventually.

## Feature Overview

This open-source extension adds the following functionality to the CoreMedia Studio:

* Send content to GlobalLink for translation into one or multiple languages
  with individual due dates in one or multiple workflows.

* Retrieve content from GlobalLink once the translation is finished.

* Automatically detect cancellations of submissions at GlobalLink and cancel the
  translation workflow in CoreMedia Studio.
* Configure the connection to GlobalLink per site hierarchy.

* Show additional information like the translation status from GlobalLink in
  CoreMedia Studio.

* Download XLIFF files and import log files in CoreMedia Studio if an error
  occurs during import.

* Editors in CoreMedia Studio are notified about completion, cancellation, and
  import and communication errors of a translation workflow with GlobalLink.

> **GitHub Pages for more**
>
> A detailed documentation available at
> [GitHub Pages](https://coremedia.github.io/coremedia-globallink-connect-integration/).

# Version Tags

For adaptions to CoreMedia CMS major versions you will find corresponding tags
named according to the CMS major version. It is recommended to take these tags
as a starting point from within your project, choosing the major version
matching your project version.

# See Also

## GCC Java REST Client Facades

* **[README: gcc-restclient-facade](apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade/README.md)**

  Facade encapsulating all calls to GCC REST, the Java API as well as the REST
  backend.

* **[README: gcc-restclient-facade-default](apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-default/README.md)**

  This is the default and fallback facade used when nothing is defined in
  settings — or if there is no other facade applicable.

* **[README: gcc-restclient-facade-disabled](apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-disabled/README.md)**

  This facade mainly serves as example how to implement custom connection
  types. The implementation just throws exceptions on every interaction with the facade.

* **[README: gcc-restclient-facade-mock](apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-mock/README.md)**

  The mock facade will simulate a translation service in that way, that it
  replaces the target nodes (pre-filled with values from source nodes) in
  XLIFF with some other characters.

# CoreMedia Labs

Welcome to [CoreMedia Labs](https://www.coremedia.com/labs/)! This repository
is part of a platform for developers who want to have a look under the hood or
get some hands-on understanding of the vast and compelling capabilities of
CoreMedia. Whatever your experience level with CoreMedia is, we've got something
for you.

Each project in our Labs platform is an extra feature to be used with CoreMedia,
including extensions, tools and 3rd party integrations. We provide some test
data and explanatory videos for non-customers and for insiders there is
open-source code and instructions on integrating the feature into your
CoreMedia workspace. 

The code we provide is meant to be example code, illustrating a set of features
that could be used to enhance your CoreMedia experience. We'd love to hear your
feedback on use-cases and further developments! If you're having problems with
our code, please refer to our issues section. If you already have a solution to
an issue, we love to review and integrate your pull requests.
