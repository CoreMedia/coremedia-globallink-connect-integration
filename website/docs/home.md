---
title: Home
sidebar_position: 1
slug: /
---

![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

# Translation via GlobalLink Connect Cloud

This open-source workspace enables CoreMedia CMS to communicate with GlobalLink
Connect Cloud (GCC) REST API in order to send contents to be translated, query
the translation status and to update contents with the received translation
result eventually.

## Feature Overview

This extension adds the following functionality to the CoreMedia Studio:
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

## Table of Contents

1. [Editorial Quick Start](editorial-quick-start.md)

    Use GlobalLink Translation Workflow in CoreMedia Studio.

2. [Administration](administration.md)

    How to administrate GlobalLink extension (especially in CoreMedia Studio).

3. [Development](development.md)

    How to integrate the extension to your Blueprint workspace.

4. [Release Steps](release/README.md)

    How to release this workspace.

5. [Changelog](changelog/README.md)

   Release information (since v12.2406.1.0-1).
