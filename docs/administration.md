# Administration

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

## Table of Content

1. [Prerequisites](#prerequisites)
2. [Retrieve Client Secret Key](#retrieve-client-secret-key)
3. [Setup of GlobalLink Connect Cloud](#setup-of-globallink-connect-cloud)
4. [Configuring GlobalLink Connection Settings](#configuring-globallink-connection-settings)
5. [Questions &amp; Answers](#questions-amp-answers)
6. [See Also](#see-also)

## Prerequisites

Ensure you have your GCC parameters at hand:

* GCC REST Base URL
* API Key
* File Type
* _Optional:_ Client Secret Key (see below)

For more details on available options and how to configure them in CoreMedia
Studio read [this](#configuring-globallink-connection-settings).

## Retrieve Client Secret Key

If you did not receive a client secret key as part of your _Client Onboarding_
you can query it via REST API `/api/v3/connectors` which, provides a response
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

## Setup of GlobalLink Connect Cloud

There is some mandatory configuration required in GlobalLink Connect Cloud so
that the integration between both systems runs smoothly:

* The connector uses the XLIFF file format to exchange translatable texts with
    GlobalLink. Make sure that your GlobalLink instance is configured
    accordingly and request the file format identifier from your contacts at
    Translations.com. This identifier is configured
    [here](#configuring-globallink-connection-settings).

* The connector automatically detects submissions that have been cancelled in
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
    [settings](#configuring-globallink-connection-settings).

    ```
    |
    |- en_FR
    |  |- fr_FR
    |
    |- en_DE
    |  |- de_DE
    |
    ```

## Configuring GlobalLink Connection Settings

### Server-side configuration

Various default values are defined globally in the properties file of
the `gcc-workflow-server` module (see [gcc-workflow.properties](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/main/apps/workflow-server/gcc-workflow-server/src/main/resources/META-INF/coremedia/gcc-workflow.properties)).

The following configuration shall only be stored there so that you do not
accidentally leak it to clients that have read access to the content repository:

`gcc.apiKey` the API key to authenticate at GlobalLink. In the content configuration
it is just called `apiKey` (type:`String`).

You can theoretically set it in the content like the parameters in the
next chapter, but it is not recommended. The same applies to the parameters
`gcc.username` and `gcc.password` in previous versions of this integration.

### Configuration in Studio

GlobalLink Settings can be configured globally for all sites or specifically 
for some site. The Settings can be located in the following folders:

* `/Settings/Options/Settings/Translation Services`: Once you have configured
the integration in a Settings content in this folder the GlobalLink
workflow will be available to all sites.
* `<SITE_ROOT>/Options/Settings/Translation Service`: Only define 
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
    part of the _Server-side configuration_ is recommended. Otherwise you can 
    create separate site-specific GlobalLink settings that only contain 
    this parameter. (type:`String`)
* `fileType` If there is more than one file format in your
    GlobalLink setup, then this has to be set to the XLIFF file type identifier
    to be used by your connector. (_optional_, default: `xliff`, type:`String`)
* `type` Determines which facade implementation will be used (see
    [Facade Documentation](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/master/apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade/README.md)
    ). (_optional_, type:`String`)
* `dayOffsetForDueDate` Defines the offset for the
    `Due Date` of the workflow "Translation with GlobalLink" in the Start Workflow 
    Window to lie within the future in days.
    (_optional_, default: `0`, type:`Integer`, scope:**global**)
* `retryCommunicationErrors` Number of retries in case of a communication error
    with GlobalLink. (_optional_, default: `5`, type:`Integer`)
* `isSendSubmitter` Defines if the name of the editor that started the workflow 
    is send to GlobalLink as part of the submission.
    (_optional_, default: `false`, type:`Boolean`)

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
    submission's state and the translated XLIFF(s) are not immediately ready. 
    (_optional_, default: `1800`, type:`Integer`)
* `cancelTranslationRetryDelay` Overrides the interval (secs) for retrying the 
    cancellation of a submission. (_optional_, default: `180`, type:`Integer`)

You can also define parameters for testing with the mock facade
(see [Mock Facade Documentation](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/main/apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-mock/README.md)).

**⚠️ Make sure to restrict read and write rights to the Settings content to
those user groups that actually need access. Do not publish the 
Settings and follow the recommendations from the previous chapter.
This will reduce the risk of accidentally leaking sensitive information.**

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

## See Also

* [Development](development.md)

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
