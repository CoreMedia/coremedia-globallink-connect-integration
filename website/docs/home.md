---
id: introduction
title: Home
description: Documentation of CoreMedia GlobalLink Connect Cloud Integration
sidebar_position: 1
slug: /
tags:
  - GCC
---

![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

# CoreMedia GlobalLink Connect Cloud Integration

> **Translation via GlobalLink Connect Cloud**

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

## Previous Version Documentation

To access documentation for a given released version, open
<RepositoryLink path="website/docs/"/> and switch to the corresponding
version tag.

Best experience is provided if you check out
<RepositoryLink path="website/"/> of the respective version and invoke:

```bash
pnpm install
pnpm start
```

to conveniently browse the documentation.

:::note NOTE: website/docs/ Unavailable
If `website/docs` is inaccessible for your version, you may be visiting
an older version. Navigate to repository path `docs/` instead.

The same applies to version tag v2412.0.0-1, although the first deployed version
of this site referenced v2412.0.0-1. This is due to the documentation change
applied directly after the release of v2412.0.0-1.
:::
