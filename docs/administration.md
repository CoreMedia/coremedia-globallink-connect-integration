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
* Username
* Password
* File Type
* _Optional:_ Client Secret Key (see below)

For more details on available options and how to configure them in CoreMedia
Studio read [this](#configuring-globallink-connection-settings).

## Retrieve Client Secret Key

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

After you have created your GlobalLink settings at `/Settings/Options/Settings/GlobalLink` 
and linked them to your site, you need to configure your personal 
GlobalLink parameters. There can be more settings content items like this with different
names and values to enable site-specific connections. However, global settings 
like `dayOffsetForDueDate` have to be defined in `/Settings/Options/Settings/GlobalLink`.

Therefore, you need to add a struct to the GlobalLink settings, named
`globalLink`. **Make sure to restrict read and write rights of this content to those user groups that actually need access to prevent leaking sensitive information.**

Within that struct you need to provide the following 
parameters:

* `url` for GCC REST Base URL  (type:`String`)
* `username` Username (type:`String`)
* `password` Password (type:`String`)
* `key` the GCC connector key (type:`String`)
* `fileType` (_optional_) If there is more than one file format in your
    GlobalLink setup, then this has to be set to the XLIFF file type identifier
    to be used by your connector. (type:`String`)
* `type` (_optional_) determines which facade implementation will be used
    (see [Facade Documentation](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/master/apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade/README.md)). (type:`String`)
* `dayOffsetForDueDate` defines the offset for the
   `Due Date` of the workflow "Translation with GlobalLink" in the Start Workflow 
   Window to lie within the future in days. (type:`Integer`, scope:**global**)
* `retryCommunicationErrors` number of retries in case of a communication error
    with GlobalLink. By default the value is set to 5. (type:`Integer`)
  
By default, the offset is set to `20` within the test data.

In addition to the configuration approach described above, the settings can be
defined in the properties file of the `gcc-workflow-server` module (see [gcc-workflow.properties](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/master/apps/workflow-server/gcc-workflow-server/src/main/resources/META-INF/coremedia/gcc-workflow.properties)). These settings
will be used as a global default or fallback, if there are no settings defined for the respective site
in the content. 
If you only have one GlobalLink connector for all your sites, 
this approach also provides the advantage of not accidentally leaking the 
credentials to editors.

You can also define Parameters for testing with the mock facade
(see [Mock Facade Documentation](https://github.com/CoreMedia/coremedia-globallink-connect-integration/tree/master/apps/workflow-server/gcc-workflow-server-facade/gcc-restclient-facade-mock/README.md)).


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
