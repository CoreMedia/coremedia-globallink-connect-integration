---
sidebar_position: 4
description: How to configure GlobalLink connection credentials and behavior.
---

# Configuring Connection Settings

## Server-side configuration

Various default values are defined globally in the properties file of
the `gcc-workflow-server` module (see
[gcc-workflow.properties](<https://github.com/CoreMedia/coremedia-globallink-connect-integration/blob/main/apps/workflow-server/gcc-workflow-server/src/main/resources/META-INF/coremedia/gcc-workflow.properties> "apps/workflow-server/gcc-workflow-server/src/main/resources/META-INF/coremedia/gcc-workflow.properties")).

The following configuration shall only be stored there so that you do not
accidentally leak it to clients that have read access to the content repository:

`gcc.apiKey` the API key to authenticate at GlobalLink. In the content configuration
it is just called `apiKey` (type:`String`).

If the API key is to be set upon system startup, you can do so by defining
variable `GCC_APIKEY` with the appropriate value. Check with development that
the required actions have been taken on the code (see
[Enabling External Definition of API Key](<../developers/coremedia-blueprint#enabling-external-definition-of-api-key> "Developers | CoreMedia Blueprint | Enabling External Definition of API Key")).
In context of a CoreMedia-hosted cloud instance, store the values in
_Cloud Manager Secrets_ and request activation through _CoreMedia
Cloud Support_.

You can theoretically set it in the content like the parameters in the
next chapter, but it is not recommended. The same applies to the parameters
`gcc.username` and `gcc.password` in previous versions of this integration.

Property setting `gcc.cms-retry-delay` defines the delay in seconds between two
attempts to retrieve data from or write data to the Content Management Server,
should the Content Management Server be unavailable temporarily (_optional_,
default: `60` type: `Integer`). Intervals shorter than 60 seconds or longer
than a day are not allowed and will fall back to the corresponding max or min
values.

If the delay is to be set upon system startup, you can do so by defining
variable `GCC_CMS_RETRY_DELAY` with the appropriate value.

## Configuration in Studio

GlobalLink Settings can be configured globally for all sites or specifically
for some site. Site-specific settings override global settings except for
`dayOffsetForDueDate`. The Settings can be located in the following folders:

* `/Settings/Options/Settings/Translation Services`: Once you have configured
  the integration in a Settings content in this folder the GlobalLink
  workflow will be available to all sites.
* `<SITE_ROOT>/Options/Settings/Translation Services`: Only define
  the Settings here, if the GlobalLink workflow should only be available
  if you translate content from this site to one of its derived sites. But, you
  can also define Settings in this folder to overwrite specific parameters from
  the global Settings.

After you have created your GlobalLink settings for example at
`/Settings/Options/Settings/Translation Services/GlobalLink`
you need to configure your personal GlobalLink parameters. Open the Settings
in CoreMedia Studio and add a struct named
`globalLink`. Within that struct the following parameters must/can be specified:

* `url` for GCC REST Base URL  (type:`String`)
* `key` The GCC connector key. If there is only one key, then setting it as
  part of the _Server-side configuration_ is recommended. Otherwise, you can
  create separate site-specific GlobalLink settings that only contain
  this parameter. (type:`String`)
* `fileType` If there is more than one file format in your
  GlobalLink setup, then this has to be set to the XLIFF file type identifier
  to be used by your connector. (_optional_, default: `xliff`, type:`String`)
* `type` Determines which facade implementation will be used (see
  [Facade Documentation](<https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/main/apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade/README.md> "apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade/README.md")).
  (_optional_, type:`String`)
* `dayOffsetForDueDate` Defines the offset for the
  `Due Date` of the workflow "Translation with GlobalLink" in the Start Workflow
  Window to lie within the future in days.
  (_optional_, default: `0`, type:`Integer`, scope:**global**)
* `retryCommunicationErrors` Number of retries in case of a communication error
  with GlobalLink. (_optional_, default: `5`, type:`Integer`)
* `isSendSubmitter` Defines if the name of the editor that started the workflow
  is sent to GlobalLink as part of the submission.
  (_optional_, default: `false`, type:`Boolean`)
* `submissionInstruction` Defines the behavior of submission instructions.
  For details, see `GCSubmissionInstruction`.
  (_optional_, default: see `GCSubmissionInstruction`, type:`Struct`)
* `submissionName` Defines the behavior of submission names.
  For details, see `GCSubmissionName`.
  (_optional_, default: see `GCSubmissionName`, type:`Struct`)

Be aware that the `dayOffsetForDueDate` can only be configured in the global
Settings location.

The following parameters of the struct should be handled carefully and only
after consulting Translations.com. They affect all new and running workflows,
and as such they can instantly cause high loads on GlobalLink's servers or
unexpectedly long update intervals. Intervals shorter than 60 seconds or longer
than a day are not allowed and will fall back to the corresponding max or min
values. The interval is also limited to be not longer than one day because if
you accidentally set it to a very big value, there would be no turning back. You
would have to wait until it is expired.

* `sendTranslationRequestRetryDelay` Overrides the default interval (secs)
  between retries if the XLIFF could not be sent on first try.
  (_optional_, default: `180`, type:`Integer`)
* `downloadTranslationRetryDelay` Overrides the update interval (secs) of the
  submission's state, and the translated XLIFF(s) are not immediately ready.
  (_optional_, default: `1800`, type:`Integer`)
* `cancelTranslationRetryDelay` Overrides the interval (secs) for retrying the
  cancellation of a submission. (_optional_, default: `180`, type: `Integer`)

You can also define parameters for testing with the mock facade
(see [Mock Facade Documentation](<https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/main/apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-mock/README.md> "apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-mock/README.md")).

:::warning Restrict Access to Settings

Make sure to restrict read and write rights to the Settings content to
those user groups that actually need access. Do not publish the
Settings and follow the recommendations from the previous chapter.
This will reduce the risk of accidentally leaking sensitive information.
