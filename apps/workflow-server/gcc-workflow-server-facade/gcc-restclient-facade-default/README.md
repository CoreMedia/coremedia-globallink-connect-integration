# Default Facade for GCC REST Java library

This is the default and fallback facade used when nothing is defined in
settings &mdash; or if there is no other facade applicable.

The default facade connects to a real GCC REST Backend via GCC REST Java API.

This module comes with a so called _contract-test_ which is recommended to run
at least with every GCC REST Java API Update. It requires an available GCC
REST Backend to connect to. It will test, if the GCC REST Java API as well as the
GCC REST Backend still fulfill the requirements of this facade.

If the contract is not fulfilled, you typically adapt the facade's implementation.

## Running Contract Tests
 
 In order to run the contract tests you need:
 
 * A GCC sandbox.
 * The GCC sandbox credentials.
 * And a file `.gcc.properties` in your user home which contains these
     credentials and will look like this:
     
     ```properties
     apiKey=12ab34cd
     url=https://connect-dev.translations.com/api/v3/
     key=0e...abc
     fileType=xliff
     ```
     
 The contract tests then run as normal unit tests. These tests are ignored
 when the properties are not available or not readable.
 
 ## See Also
 
 * [GlobalLink API Documentation][GCC_API_DOC]
 * [translations-com/globallink-connect-cloud-api-java: GlobalLink Connect Cloud REST Java library][GCC_API_JAVA]
 
 [GCC_API_DOC]: <https://onlinehelp.translations.com/api/connect/GlobalLink_Connect_Cloud_API_Documentation.htm> "GlobalLink API Documentation"
 [GCC_API_JAVA]: <https://github.com/translations-com/globallink-connect-cloud-api-java> "translations-com/globallink-connect-cloud-api-java: GlobalLink Connect Cloud REST Java library"
