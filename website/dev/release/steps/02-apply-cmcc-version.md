---
description: Set CMCC Version in Workspace.
tags:
  - development
  - contributors
  - release
  - cmcc
---

# Set CMCC Version

If the release of this adapter targets a newer CMCC release, make sure that the
version reference `<cm.middle.core.version>` in `gcc-workflow-server-parent`
(POM path: `apps/workflow-server/pom.xml`) is updated.

:::info INFO: No Changes Needed for Studio-Client
Starting with release `v2512.0.0-1` it is not required anymore to apply
a similar change to the Studio client code. This is because we use the
`catalog:` version inside there to refer to corresponding artifacts of the
CMCC workspace.
:::

:::tip TIP: Update Documentation Links
The documentation references the CMCC documentation at
[documentation.coremedia.com](https://documentation.coremedia.com/).
While you are at adapting the CMCC version number, you may want to update the
documentation reference right away.

For details see the
[documentation update reference](./06-update-documentation.md).
:::
