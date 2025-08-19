---
sidebar_position: 2
description: Integration into the CoreMedia Blueprint.
---

# CoreMedia Blueprint

How to integrate and enable the GlobalLink Connector into the CoreMedia
Blueprint.

## Adding GCC Adapter to the Blueprint

There are many approaches for integrating the extension into the Blueprint. Each
one comes with its own pros and cons depending on your use case.

With `git subtree` you can easily change the adapter code and cherry-pick
important fixes of upcoming releases without having to create a fork of the
adapter itself. This is one of the main advantages of the subtree approach over
`git submodule`. Contributing back upstream is however slightly more
complicated.

Feel free to choose the strategy that fits your needs best. For example:

* As a Git Subtree from the workspace root (recommended)

  ```bash
  mkdir -p modules/extensions
  # Add sub-project as a remote to enable short form
  git remote add -f gcc https://github.com/CoreMedia/coremedia-globallink-connect-integration.git
  git subtree add --prefix modules/extensions/gcc gcc main --squash
  # For example - update the sub-project at a later date...
  git fetch gcc main
  git subtree pull --prefix modules/extensions/gcc gcc main --squash
  ```

  See
  [Issue 28](<https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/28> "Git subtree approach · Issue #28 · CoreMedia/coremedia-globallink-connect-integration")
  for a more detailed description of a similar approach.

* Or as a Git Submodule from the workspace root

  ```bash
  git submodule add https://github.com/CoreMedia/coremedia-globallink-connect-integration.git modules/extensions/gcc
  git submodule update --init --recursive
  cd modules/extensions/gcc
  git checkout <release-tag>
  cd ..
  # Add and commit .gitmodules and current HEAD of submodule 
  git add .
  git commit -m "Initial integration of submodule based on <release-tag>"
  ```

If you want to contribute to this project — which we hope for — you need to fork
the project. For example, with the `git subtree` approach, pushing to your fork
could look as follows:

```bash
# Add your fork as another remote
git remote add -f my-gcc https://github.com/my-company/coremedia-globallink-connect-extended.git
# For example - update the sub-project at a later date...
git subtree push --prefix=modules/extensions/gcc my-gcc main
```

You can then send us the corresponding pull request.

## Enabling the Extension

Execute the following command in `workspace-configuration/extensions` below the
workspace root folder:

```bash
cd workspace-configuration/extensions
mvn extensions:sync -Denable=gcc
```

This will activate the extension. The extension tool will also set the relative
path for the parents of the extension modules.

## Adding GCC Workflow to Workflow Server Deployment

You need to add `translation-global-link.xml` to your workflow definitions in
`global/management-tools/management-tools-image/src/main/image/coremedia/import-default-workflows`.
Add
`TranslationGlobalLink:/com/coremedia/labs/translation/gcc/workflow/translation-global-link.xml`
to the variable `DEFAULT_WORKFLOWS`.

## Enabling External Definition of API Key

If the _API key_ for communication with GlobalLink is to be set externally
upon system startup, add the following lines to file
`apps/workflow-server/spring-boot/workflow-server-app/src/main/resources/application.properties`:

```text
# GlobalLink
gcc.apiKey=
```

If in doubt, check with the system's administrator how the API key is to be
defined. See [Server-side configuration](<../administrators/configure-gcc-settings#server-side-configuration> "Administrators | Configuring Connection Settings")
for details.

## Extension Point for Custom Properties

In case you need additional properties for interacting with GlobalLink REST
backend, you may need to extend the Studio Workflow UI as well as the Workflow
Actions. You will find details how to do that here:

* [Blueprint Developer Manual / Project Extensions](<https://documentation.coremedia.com/cmcc-12/artifacts/2412.0/webhelp/coremedia-en/content/projectExtensions.html> "Blueprint Developer Manual / Project Extensions")
* [Blueprint Developer Manual / Configuration and Customization](<https://documentation.coremedia.com/cmcc-12/artifacts/2412.0/webhelp/coremedia-en/content/translationWorkflow_configurationAndCustomization.html> "Blueprint Developer Manual / Configuration and Customization")
* [Blueprint Developer Manual / Translation Workflow Studio UI](<https://documentation.coremedia.com/cmcc-12/artifacts/2412.0/webhelp/coremedia-en/content/TranslationWorkflowUiCustomization.html> "Blueprint Developer Manual / Translation Workflow Studio UI")
* [Workflow Manual / Workflow Variables](<https://documentation.coremedia.com/cmcc-12/artifacts/2412.0/webhelp/workflow-developer-en/content/WorkflowVariables.html> "Workflow Manual / Workflow Variables")
