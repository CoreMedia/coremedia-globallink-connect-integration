---
id: archive-branch
sidebar_label: Archive Branch
description: How to Archive Maintenance Branches.
---

# Archive Branch

CoreMedia GlobalLink Connect Cloud Integration is only maintained for
CoreMedia Content Cloud (CMCC) Releases that are actively maintained (thus, for
example, maintenance ends when a CMCC version reaches extended support).

Because of this, it should be considered to remove `maintenance/MMMM.x` branches
once they are not maintained anymore.

## archive-branch.sh

For convenience, you may use <RepositoryLink path="bin/archive-branch.sh"/>.
It will tag the last commit of a given branch with `archive/<branch name>` and
eventually delete the branch (both locally and remotely).

```bash
./bin/archive-branch origin "maintenance/2406.x"
```

If `<remote>` (here: `origin`) is omitted, it defaults to `origin`, so you may
also just call:

```bash
./bin/archive-branch "maintenance/2406.x"
```
