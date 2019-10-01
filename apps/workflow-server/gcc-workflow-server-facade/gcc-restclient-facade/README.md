# Facade for GCC REST Java library

This facade is meant to encapsulate all calls to GCC REST, the
Java API as well as the REST backend.

Having this, we can clearly define the contract between us (the consumer) and
the GCC API (the producer). We can test the contract and/or we can mock the
GCC API and replace translation with some mock results without having the
a GCC sandbox at hand.

## Java ServiceLoader

The module `gcc-restclient-facade` provides the base for the GCC REST Java
facade. It defines especially the interfaces of the facade and the
provider for retrieving these facades, the so called `SessionProvider`.

The default `SessionProvider` will use Java `ServiceLoader` to detect all
available types of facades, described below as _Connection Types_.

In order to change the different connection types, the workflow module just
needs to get its runtime dependencies adapted to the desired connection
type modules. For example, to remove the mock facade, just remove the runtime
dependency to `gcc-restclient-facade-mock` from module `gcc-workflow`.

## Standard Connection Types

This facade provides different connection types available via the
`GCExchangeFacadeFactory`:

* **Type _default_:** The default type, which uses a standard GCC connection.
* **Type _disabled_:** Will answer any trials to communicate with GCC with an
    exception which states, that the GCC service is currently unavailable.
    You may use this type for example for planned GCC maintenance slots.
* **Type _mock_:** Will use a mocked translation service, which will just replace
    some characters as _translation_. Useful for demos and local development
    where you do not have a GCC sandbox at hand.

## Switching Connection Types

Note, that it may be harmful to switch connection types while translation
workflows are running as it may produce an inconsistent state. For example
if you switch from _mock_ to _default_ the submission IDs will be completely
different.

The only exception to this is the _disabled_ connection type, as it will not
generate any state but just block any communication, so that it is similar for
the workflow as if the GCC service is currently not reachable.

## Adding New Connection Types

In order to add a new connection type, you create a new module and add at least
these files:

* `resources/META-INF/services/`
    * `com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider`
    
        This contains the class reference of the provider implementation
        to instantiate.
* **Implementations:**
    * `MyGCExchangeFacade` implementing `GCExchangeFacade`
        
        This is the facade which implements all required methods to
        perform the translation process, like uploading and downloading
        XLIFF for example.

    * `MyGCExchangeFacadeProvider` implementing `GCExchangeFacadeProvider`
    
        This is the ServiceProvider Interface (SPI) which is responsible for
        instantiating the facade. The SPI will decide by type token (like
        `mock`, `default`, ...) if it is responsible for creating the
        facade implementation or not.

## Delegate

By default a real connection to GCC is established. In order to do so, the
facade internally uses `org.gs4tr.gcc.restclient.GCExchange`. If you need to
access GCC API which is not available via the facade, you may consider
using `GCExchangeFacade.getDelegate()` to use the raw API. For production use
you should remove any usages of `getDelegate` and extend the facade instead
and also extend the corresponding `DefaultGCExchangeFacadeContractTest` in order to
be able to regularly test the contract you rely on when using the GCC Java RestClient
as well as the GCC REST Backend. See below for details.

## See Also

* **Facade Implementation Readmes:**
    * [Default Facade](../gcc-restclient-facade-default/README.md)
    * [Disabled Facade](../gcc-restclient-facade-disabled/README.md)
    * [Mock Facade](../gcc-restclient-facade-mock/README.md)
