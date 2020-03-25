# Development

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

## Table of Content

1. [Introduction](#introduction)
2. [Adding GCC Submodule](#adding-gcc-submodule)
3. [Adding GCC as extension](#adding-gcc-as-extension)
4. [Adding GCC Workflow to Workflow Server Deployment](#adding-gcc-workflow-to-workflow-server-deployment)
5. [Patch/Edit Site Homepages](#patchedit-site-homepages)
6. [Extension Point for Custom Properties](#extension-point-for-custom-properties)
7. [Design Details](#design-details)
6. [Workspace Structure](#workspace-structure)
6. [See Also](#see-also)

## Introduction

In order to start using the GCC Labs Project you have to add the project to
your Blueprint extensions. The following process assumes, that you will add
the GCC extension as GIT submodule to your workspace. You may as well decide
to copy the sources to your workspace.

To summarize the steps below, everything you need to do:

1. Add GCC to your Blueprint workspace at `modules/extensions`.
2. Configure your extension tool.
3. Run your extension tool to activate the gcc extension.
4. Add `translation-global-link.xml` to your workflow server deployment.
5. Later on: Ensure that your homepages link to `/Settings/Options/Settings/GlobalLink`
    in their linked settings.

## Adding GCC Submodule

The GCC extension can be added the Blueprint workspace as a Git submodule as follwos:
```bash
$ mkdir -p modules/extensions
$ cd modules/extensions
$ git submodule add https://github.com/CoreMedia/coremedia-globallink-connect-integration.git gcc
$ git submodule init
$ git checkout -b <tag-name> <your-branch-name>
```
If you plan to customize the extension, create a fork of the repository and adjust the repository parameters accordingly. 

## Adding GCC as extension

In order to add the gcc extension to your workspace you need to configure your
extension tool. The configuration for the tool can be found under
`workspace-configuration/extensions`. Make sure that you use at least version
4.0.1 of the extension tool.

Here you need to add the following configuration for the `extensions-maven-plugin`
```xml
<configuration>
  <projectRoot>../..</projectRoot>
  <extensionsRoot>modules/extensions</extensionsRoot>
  <extensionPointsPath>modules/extension-config</extensionPointsPath>
</configuration>
```

After adapting the configuration you may run the extension tool in
`workspace-configuration/extensions`:

```bash
$ mvn extensions:sync
$ mvn extensions:sync -Denable=gcc
``` 

This will activate the globalLink extension. The extension tool will
also set the relative path for the parents of the extension modules.

## Adding GCC Workflow to Workflow Server Deployment

You need to add `translation-global-link.xml` to your workflow definitions
in `global/management-tools/docker/management-tools/src/docker/import-default-workflows`. Add `TranslationGlobalLink:/com/coremedia/labs/translation/gcc/workflow/translation-global-link.xml`
to the variable `DEFAULT_WORKFLOWS`.

## Patch/Edit Site Homepages

For each master site for which you want to start GlobalLink Translation Workflows
from, you need to add `/Settings/Options/Settings/GlobalLink` settings document
to the linked settings. To patch a homepage in server export you can use
the following SED command:

```bash
sed --in-place --expression \
  "\\#.*<linkedSettings>.*#a <link href=\"../../../../../../Settings/Options/Settings/GlobalLink.xml\" path=\"/Settings/Options/Settings/GlobalLink\"/>" \
  "${HOMEPAGE}"
```

where `${HOMEPAGE}` is the server export XML file of your homepage to patch.

As alternative you may manually edit the corresponding homepages later on
in CoreMedia Studio.

## Extension Point for Custom Properties

In case you need additional properties for interacting with GlobalLink REST
backend, you may need to extend the Studio Workflow UI as well as the
Workflow Actions. You will find details how to do that here:

* [Blueprint Developer Manual / Configuration and Customization][DOC-CM-TRANSLATION]
* [Blueprint Developer Manual / Translation Workflow Studio UI][DOC-CM-TRANSLATION-UI]
* [Workflow Manual / Workflow Variables][DOC-WF-VARS]

## Design Details

### Translation Types

In CoreMedia CMS there exist two translation types:

1. Translation to derived sites, and
2. Translation to preferred site.

While for _Translation to derived sites_ the site-managers of the master site send
localization/translation items to the derived sites, the local site-managers of
each derived site may as well trigger translation from master site to their
derived site (assumed to be set as preferred site).

This implementation is designed to support _Translation to derived sites_ and instead
of local site-managers accepting the translation, it is designed, so that the
site-manager of the master site will also take care of accepting the translation
results.

### CMS Workflow to GCC Workflow

GCC uses specific terms for the structure of their translation workflow. The
terms are important to understand, especially how they map to the CoreMedia
CMS translation workflow:

* **Submission:**

    The CMS translation workflow creates and starts a submission when handing
    over the contents to be translated to GCC. A submission has one source
    locale and consists of several jobs.
    
* **Job:**

    One job is bound to one target locale. It may consist of several tasks.
    In this implementation jobs are not really visible. See _Task_ documentation
    below.
    
* **Task:**

    One task is bound to one file to translate. As the CoreMedia CMS translation
    workflow creates one XLIFF document per target site/target locale, all jobs
    of this implementation only contain one task. 
 
#### Workflow Stages

A rough sketch of the CoreMedia CMS translation workflow shows how the
GCC translation workflow is embedded into the CMS workflow (here: standard
processing):

1. **Preprocessing Phase:** In this phase, the target contents are prepared to
    receive the translation results later on. Missing contents are created,
    links are adjusted, some properties automatically merged (like linklists
    for example).
2. **Translation Phase:** Contents are handed over as XLIFF documents to GCC.
    The state is regularly polled. XLIFF documents from completed tasks are
    automatically downloaded and applied. Changes are applied as
    translation-workflow-robot user.
3. **Postprocessing Phase:** Once the submission is completed, the CMS workflow
    switches to post-processing phase. Editors have the change to review the
    translation and eventually accept the translation. As soon as they accept
    the translation, the last step is to update the master version number in
    the target contents, to signal from which master version they received
    the updates.

### Cancellation

GCC offers cancellation at task and submission level. Note, that the
CoreMedia CMS translation workflow does not support cancellation at task
level.

The reason can be found in the _Workflow Stages_ mentioned above.
When a cancellation is detected, target contents may have received some changes 
already, and cancellation requires to revert all those changes. As there is no partial
revert of some contents, all contents which are part of the CMS translation
workflow need to be reverted.

Thus, as the existing CMS API does not support partial cancellation, the same applies
to the GCC submission which must not be partially cancelled.

The current implementation is aware of partial cancellation, though: If only some
tasks are cancelled, the implementation will stop downloading results from these
tasks and wait for the whole submission to be marked as cancelled. Such wait
loops are logged.

_Planned/Later:_ If you perform cancellation within the CMS workflow, it is always ensured,
that the complete submission is cancelled. 

### Not supported: Reopening

Reopening submissions is not supported by this implementation. Instead, please
start a new translation workflow for contents where you want to get the translation
result adjusted.

Implementing reopening would require to cope with challenges like the following:

* **Polling:** The implementation uses polling the translation state, while a
    CoreMedia workflow is active. Polling ends as soon as the CoreMedia workflow
    is done. In order to respond to reopening submissions at GCC you either need
    to keep polling even after the workflow is done, or you need to change the
    implementation to use push notifications from GCC backend instead. Push
    notifications is not part of this implementation as it would require to
    expose an additional service of the CoreMedia CMS backend.
* **Updated Contents/Resolving Conflicts:** As reopening may occur after several
    days or even months, it is most likely that your target contents got updated
    meanwhile. Trying to re-import new translation results may cause hard to
    resolve conflicts, because of for example missing linked documents in
    CoreMedia RichText.

## Workspace Structure

### workflow-server
Manages the workflow-server extension and the gcc-restclient-facade

#### gcc-restclient-facade*

Facades to GCC Java REST Client API. Please see corresponding
_Facade Documentation_ in the workspace for details.

#### gcc-workflow-server

The workflow definition and classes to send and receive translation via GCC REST API.

### studio-client

Extension for the Studio client that registers the workflow definition and configures the UI.

### studio-server

Extension for the Studio REST server that registers the workflow definition.

### user-changes

Extension for the User Changes web application to enable Studio notifications for the GlobalLink workflow.

### test-data

Contains a (quite empty) settings document which, after content import, needs to
be linked to your site root documents (also known as Homepages).

## See Also

* [Administration](administration.md)

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

<!-- Links, keep at bottom -->

[DOC-CM-PEXT]: <https://documentation.coremedia.com/cmcc-10/artifacts/2001/webhelp/coremedia-en/content/projectExtensions.html> "Blueprint Developer Manual / Project Extensions"
[DOC-CM-TRANSLATION]: <https://documentation.coremedia.com/cmcc-10/artifacts/2001/webhelp/coremedia-en/content/translationWorkflow_configurationAndCustomization.html> "Blueprint Developer Manual / Configuration and Customization"
[DOC-CM-TRANSLATION-UI]: <https://documentation.coremedia.com/cmcc-10/artifacts/2001/webhelp/coremedia-en/content/TranslationWorkflowUiCustomization.html> "Blueprint Developer Manual / Translation Workflow Studio UI"
[DOC-WF-VARS]: <https://documentation.coremedia.com/cmcc-10/artifacts/2001/webhelp/workflow-developer-en/content/WorkflowVariables.html> "Workflow Manual / Workflow Variables"
