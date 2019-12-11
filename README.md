![CoreMedia Labs Logo](https://documentation.coremedia.com/badges/banner_coremedia_labs_wide.png "CoreMedia Labs Logo Title Text")

# Translation via GlobalLink Connect Cloud

This open-source workspace enables CoreMedia CMS to communicate with GlobalLink
Connect Cloud (GCC) REST API in order to send contents to be translated, query
the translation status and to update contents with the received translation
result eventually.

For more detailed documentation visit [GitHub Pages](https://coremedia.github.io/coremedia-globallink-connect-integration/).

## Table of Contents

1. [Editorial Quick Start](docs/editorial-quick-start.md)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Workspace Structure](#workspace-structure)
5. [Design Details](#design-details)
6. [Questions & Answers](#questions-amp-answers)
7. [Third Party Libraries](#third-party-libraries)
8. [Manual Test Steps](#manual-test-steps)

## Prerequisites

Ensure you have your GCC parameters at hand:

* GCC REST Base URL
* Username
* Password
* File Type
* _Optional:_ Client Secret Key (see below)

For more details on available options and how to configure them in CoreMedia Studio read [this](#configuring-globallink-connection-settings).

### Retrieve Client Secret Key

If you did not receive a client secret key as part of your _Client Onboarding_
you can query it via REST API `/api/v2/connectors` which, provides a response
similar to this:

```json
{
    "status": 200,
    "message": "Connector details",
    "response_data": [
        {
            "connector_key": "8dccce941d37cc697f41724d50c487d2",
            "connector_name": "Connector 1"
        },
        {
            "connector_key": "c89bc6f7fdbba90ed4a98d5d8d8a2662",
            "connector_name": "Connector 2"
        }
    ]
}
```

### Setup of GlobalLink Connect Cloud

There is some mandatory configuration required in GlobalLink Connect Cloud so that the integration between both systems runs smoothly:
* The connector uses the XLIFF file format to exchange translatable texts with GlobalLink. Make sure that your GlobalLink instance is configured accordingly and request the file format identifier from your contacts at Translations.com. This identifier is configured [here](#configuring-globallink-connection-settings).
* The connector automatically detects submissions that have been cancelled in GlobalLink and shows this state to editors in CoreMedia Studio. Canceling individual jobs of a submission is not supported yet and will most likely yield unexpected results. To prevent this from happening, make sure that GlobalLink Cloud Connect only allows the cancellation of submissions. 
* Re-opening of submissions is not yet supported by the connector. Ask your contacts at Translations.com to disable this functionality in GlobalLink Connect Cloud to avoid misunderstandings.

## Installation

In order to start using the GCC Labs Project you have to add the project to
your Blueprint extensions. The following process assumes, that you will add
the GCC extension as GIT submodule to your workspace. You may as well decide
to copy the sources to your workspace.

To summarize the steps below, everything you need to do:

1. Add GCC to your Blueprint workspace at `modules/extensions/gcc`.
2. Configure your extension tool.
3. Run your extension tool to activate the gcc extension.
4. Add `translation-global-link.xml` to your workflow server deployment.
5. Later on: Ensure that your homepages link to `/Settings/Options/Settings/GlobalLink`
    in their linked settings.

### Adding GCC Submodule

In order to add GCC as submodule to Blueprint workspace you will use
`git submodule`. You need to add the submodule for your 
gcc extension under the following path: `modules/extensions/gcc`

### Adding GCC as extension

In order to add the gcc extension to your workspace you need to configure your extension tool. 
The configuration for the tool can be found under `workspace-configuration/extensions`. 
Make sure that you use at least version 4.0.1 of the extension tool.
Here you need to add the following configuration for the `extensions-maven-plugin`
```
        <configuration>
            <projectRoot>../..</projectRoot>
            <extensionsRoot>modules/extensions</extensionsRoot>
            <extensionPointsPath>modules/extension-config</extensionPointsPath>
        </configuration>
```

After adapting the configuration you may run the commands `mvn extensions:sync` & `mvn extensions:sync -Denable=gcc` in the 
`workspace-configuration/extensions` folder. This will activate the globalLink extension.
The extension tool will also set the relative path for the parents of the extension modules.

### Adding GCC Workflow to Workflow Server Deployment

You need to add `translation-global-link.xml` to your _builtin_ workflow definitions
in `docker/management-tools/src/docker/import-default-workflows`. Using
_builtin_ will make the definition to be read from the JAR.


The first part of the expression is the _address_ where to add the workflow,
the second part just replaces the closing parenthesis with an additional
entry for the workflow definition.

### Patch/Edit Site Homepages

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

### Configuring GlobalLink connection settings

After you have created your GlobalLink settings and linked them to your site, you need to configure your personal GlobalLink
parameters.


Therefore you need to add a struct to the GlobalLink settings, named `globalLink`. Within that struct you need to provide the following 
parameters:
* `url` for GCC REST Base URL  (type:`String`)
* `username` Username (type:`String`)
* `password` Password (type:`String`)
* `key` Client Secret Key (type:`String`)
* `fileType` (_optional_) If there is more than one file format in your GlobalLink setup, then this has to be set to the XLIFF file type identifier to be used by your connector. (type:`String`)
* `type` (_optional_) determines which facade implementation will be used (see [Facade Documentation](apps/workflow-server/gcc-facade/gcc-restclient-facade/README.md)). (type:`String`)
* `dayOffsetForDueDate` set the default value of the `Due Date`, that is set for your GlobalLink translation. The parameter defines the offset for the `Due Date` to lie within the future in days. (type:`Integer`)
* `retryCommunicationErrors` number of retries in case of a communication error with GlobalLink. By default the value is set to 5. (type:`Integer`)

You can also define Parameters for testing with the mock facade (see [Mock Facade Documentation](apps/workflow-server/gcc-facade/gcc-restclient-facade-mock/README.md)).


By default the offset is set to `20` within the test data.

## Workspace Structure

### workflow-server
Manages the workflow-server extension and the gcc-restclient-facade

#### gcc-restclient-facade*

Facades to GCC Java REST Client API. Please see corresponding
[README.md](apps/workflow-server/gcc-facade/gcc-restclient-facade/README.md) for details.

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

## Questions &amp; Answers

### What to do when deriving a new site?

**Short:** _Enable Target Language at GCC, Configure Language Mapping_

When you derive a new site and want to propagate translations to this site
via GlobalLink Translation Workflow, you need to ensure that your target locale
is supported by GlobalLink and that (if required) GlobalLink knows how to
represent the CMS locale tag (represented as IETF BCP 47 language tag) within
GlobalLink Project Director.

You can validate the configuration by retrieving the Connectors Config via
REST. It will contain a section `supported_locales` where `pd_locale` maps
to the locale representation within Project Director and `connector_locale`
should be equal to your derived site locale as IETF BCP 47 language tag.

You will find the language tags in `/Settings/Options/Settings/LocaleSettings`
in your CMS.

## Manual Test Steps

Manul Test Steps for the different use cases can be found [here](global/docs/test/manualTestSteps)


*******


# CoreMedia Labs

Welcome to [CoreMedia Labs](https://blog.coremedia.com/labs/)! This repository is part of a platform for developers who want to have a look under the hood or get some hands-on understanding of the vast and compelling capabilities of CoreMedia. Whatever your experience level with CoreMedia is, we've got something for you.

Each project in our Labs platform is an extra feature to be used with CoreMedia, including extensions, tools and 3rd party integrations. We provide some test data and explanatory videos for non-customers and for insiders there is open-source code and instructions on integrating the feature into your CoreMedia workspace. 

The code we provide is meant to be example code, illustrating a set of features that could be used to enhance your CoreMedia experience. We'd love to hear your feedback on use-cases and further developments! If you're having problems with our code, please refer to our issues section. 

<!-- Links, keep at bottom -->

[DOC-CM-PEXT]: <https://documentation.coremedia.com/cmcc-10/artifacts/1907/webhelp/coremedia-en/content/projectExtensions.html> "Blueprint Developer Manual / Project Extensions"
[DOC-CM-TRANSLATION]: <https://documentation.coremedia.com/cmcc-10/artifacts/1907/webhelp/coremedia-en/content/translationWorkflow_configurationAndCustomization.html> "Blueprint Developer Manual / Configuration and Customization"
[DOC-CM-TRANSLATION-UI]: <https://documentation.coremedia.com/cmcc-10/artifacts/1907/webhelp/coremedia-en/content/TranslationWorkflowUiCustomization.html> "Blueprint Developer Manual / Translation Workflow Studio UI"
[DOC-WF-VARS]: <https://documentation.coremedia.com/cmcc-10/artifacts/1907/webhelp/workflow-developer-en/content/WorkflowVariables.html> "Workflow Manual / Workflow Variables"

