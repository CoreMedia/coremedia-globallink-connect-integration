# Branches

```mermaid
gitGraph:
  commit tag: "v1.0-1"
  branch develop
  checkout develop
  commit
  branch approval-2412.0
  checkout approval-2412.0
  commit
  commit
  checkout develop
  merge approval-2412.0
  checkout main
  merge develop tag: "v2412.0.0-1"
  checkout develop
  commit id: "fix 2412.0.0-1"
  checkout main
  merge develop tag: "v2412.0.0-2"
  checkout develop
  commit id: "merge approval 2412.0"
  checkout main
  merge develop tag: "v2412.0.0-1"
```

* **main:** Will be initially used to create `develop` branch. Afterward, it
  will just be used to merge changes from `develop` branch to `main`, i.e., it
  will just be recipient afterward. On _release_ the main merge commit will be
  tagged. See below for details on tagging.

* **develop:** After initial creation, all development by CoreMedia and merging
  pull request will happen here. Also, any pull requests for adjustments should
  have set this as the base branch.
