---
sidebar_position: 1
description: General overview on branches and tags.
---

# Introduction

In order to start using the GCC Labs Project you have to add the project to
your Blueprint extensions. The following process assumes, that you will add the
GCC extension as GIT submodule to your workspace. You may as well decide to copy
the sources to your workspace.

To summarize the steps below, everything you need to do:

1. Add GCC to your Blueprint workspace at `modules/extensions`.
2. Configure your extension tool.
3. Run your extension tool to activate the gcc extension.
4. Add `translation-global-link.xml` to your workflow server deployment.
5. Later on: Ensure that your GlobalLink Settings reside in
    `/Settings/Options/Settings/Translation Services/GlobalLink`
