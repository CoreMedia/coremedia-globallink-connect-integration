![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

<!--
  On Update:
     * Change "message" for CMCC version to recent version.
     * Change "message" for GCC (Used) version to the recently used version.
-->

![CoreMedia Content Cloud Version](https://img.shields.io/static/v1?message=2201&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779)
![GCC Used](https://img.shields.io/static/v1?message=v2.4.0&label=GCC%20REST%20API%20%28Used%29&style=for-the-badge&color=green)
[![Maven Central: GCC Recent](https://img.shields.io/maven-central/v/com.translations.globallink/gcc-restclient.svg?label=GCC%20REST%20API%20%28Recent%29&style=for-the-badge)](https://search.maven.org/search?q=g:%22com.translations.globallink%22%20AND%20a:%22gcc-restclient%22)

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

**Detailed documentation available at
[GitHub Pages](https://coremedia.github.io/coremedia-globallink-connect-integration/),
or browse directly in [docs/ folder](./docs/README.md).**

# ⑃ Branches &amp; Tags

* **[master](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/master):**

    When development has finished on `develop` branch, changes will be merged to
    `master` branch.

* **[develop](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/develop):**

    Will contain preparations for next supported major.

* **ci/develop:**

    An artificial branch required for CoreMedia internal CI systems.

* **Version Tags:**

    For adaptions to CoreMedia CMS major versions you will find corresponding
    tags named according to the CMS major version. It is recommended to
    take these tags as starting point from within your project,
    choosing the major version matching your project version.

# See Also

* **[Changelog](CHANGELOG.md)**

    for recent changes

* **[Documentation](https://coremedia.github.io/coremedia-globallink-connect-integration/)**

    for guides for editors, administrators and developers

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

Welcome to [CoreMedia Labs](https://blog.coremedia.com/labs/)! This repository
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
