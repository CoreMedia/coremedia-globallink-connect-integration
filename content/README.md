# GCC Test Data

This module contains test-data for the GCC extension.

## /Settings/Options/Settings/Translation Services/GlobalLink

This settings document will tell the GCC extension which credentials
and endpoints to use. By default it contains some dummy values and
chooses "mock" as the default implementation for GCC interactions.
As a result any translations will be forwarded to the Mock GCC RestClient
Facade which just some character replacements to simulate a translation.

Note, that in order to activate GlobalLink, it is required, that you link
this settings document to the root documents (also known as _Homepage_) of
those master sites which shall use GlobalLink for translation.

The Settings Struct for GlobalLink contains the following entries:

* **`globalLink`**
    * **`url`:** for example `http://yourhostname:9095/api/v3`
    * **`key`:** connector key provided by GlobalLink
    * **`apiKey`:** API key provided by GlobalLink
    * **`fileType`:** _(optional)_ the name of the file type to use when uploading XLIFF. Defaults to first entry of supported file types as returned by GCC connector config. If set, it must be one of the file types from the connector config.
    * **`type`:** _(optional)_ the type of your connection; Available types:
        * `default`: _(default and fallback)_ the standard GCC connection which
            requires a GCC backend; also used for any unknown types
        * `disabled`: will cause all interaction with GCC to end with an
            exception which signals: _GCC Service disabled._ It may be used
            for GCC maintenance slots where you want to prevent connecting
            to GCC.
        * `mock`: Uses some simple mocked translation approach for translation.
            You will see some virtual latency before translations proceed and
            at the end your translated content will be the master content with
            some characters being replaced. This type is especially useful for
            local development when you have no GCC sandbox at hand.   
