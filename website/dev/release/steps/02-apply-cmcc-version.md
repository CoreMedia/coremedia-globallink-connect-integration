---
description: Set CMCC Version in Workspace.
---

# Set CMCC Version

If the release of this adapter targets a newer CMCC release, make sure that the
version reference `<cm.middle.core.version>` in `gcc-workflow-server-parent`
(POM path: `apps/workflow-server/pom.xml`) is updated.

:::info
Since release `v2412.0.0-1` (February 2025) it is not required anymore to apply
a similar change to the Studio client code. This is because we use the
`workspace:` version inside there to refer to corresponding artifacts of the
CMCC workspace.
:::

:::tip Update Documentation Links
While you are at it, you may also consider already updating the documentation
links now. Note, that for minor release approvals this is done on an extra
branch.
For details see the
[documentation update reference](./06-update-documentation.mdx).
:::
