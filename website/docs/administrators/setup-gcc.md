---
sidebar_position: 3
description: Overview of the general setup.
---

# Setup of GlobalLink Connect Cloud

There is some mandatory configuration required in GlobalLink Connect Cloud so
that the integration between both systems runs smoothly:

* The connector uses the XLIFF file format to exchange translatable texts with
  GlobalLink. Make sure that your GlobalLink instance is configured
  accordingly and request the file format identifier from your contacts at
  Translations.com. Section
  [Configuring Connection Settings](<./configure-gcc-settings> "Administrators | Configuring Connection Settings")
  will tell you how to configure the identifier.

* The connector automatically detects submissions that have been canceled in
  GlobalLink and shows this state to editors in CoreMedia Studio. Canceling
  individual jobs of a submission is not supported yet and will most likely
  yield unexpected results. To prevent this from happening, make sure that
  GlobalLink Cloud Connect only allows the cancellation of submissions.

* Re-opening of submissions is not yet supported by the connector. Ask your
  contacts at Translations.com to disable this functionality in GlobalLink
  Connect Cloud to avoid misunderstandings.

* A connector instance at GlobalLink Connect Cloud currently requires unique
  ISO-639-1 language codes in the source locales of the sites. This essentially
  means that Translations.com would have to set up multiple connectors if your
  site hierarchy looks like the following example. Each site in CoreMedia
  consequently has to use the `key` of the corresponding connector in its
  settings (see
  [Configuring Connection Settings](<./configure-gcc-settings.mdx> "Administrators | Configuring Connection Settings")).

  ```text title="Example Site Hierarchy"
  en-US
   |
   |- en-FR
   |  |
   |  `- fr-FR
   |
   |- en-DE
   |  |
   |  `- de-DE
   |
  ```
