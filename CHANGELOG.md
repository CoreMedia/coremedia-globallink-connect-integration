Changelog
================================================================================

2101
--------------------------------------------------------------------------------

### Main Changes

* Updated validators and validation configuration to align with improvements in CoreMedia Content Cloud 1ß.2101.

* Internal API `ILocalesService` was moved and renamed in 2101 AEP. Updated usages. There is no public API for this functionality yet.

* Added support for bulk cancellation of workflows

* Fixed [CoreMedia/coremedia-globallink-connect-integration#18](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/18)

2010
--------------------------------------------------------------------------------

### Main Changes

* Replace `GccWorkflowDateTimeField` of `gcc-studio-client` with built-in `WorkflowDateTimeField`.

* Replaced usage of internal API usage of `MessageBoxInternal` with newly introduced public API `MessageBoxUtil`.

* Internal API `LocaleService` was moved and renamed in 2010 AEP. Updated usages. There is no public API for this functionality yet.

* Fixed [CoreMedia/coremedia-globallink-connect-integration#20](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/20)

* Fixed [CoreMedia/coremedia-globallink-connect-integration#21](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/21)

2007
--------------------------------------------------------------------------------

### Main Changes

* Updated to CoreMedia Content Cloud 10.2007.

* Add a custom override of the `DateTimePropertyField` that displays the `duedate`.

    The `dueDate` is validated by the new `GCCDateLiesInFutureValidator.java` on the server side. 

* Replace XML-based Spring configuration with Java-based Spring configuration.

* Fixed [CoreMedia/coremedia-globallink-connect-integration#15](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/15)
    by providing an example of the .gcc.properties. 

* Fixed [CoreMedia/coremedia-globallink-connect-integration#16](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/16)
    by renaming the module workflow-server to gcc-workflow-server-parent. 

* Fixed [CoreMedia/coremedia-globallink-connect-integration#17](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/17)
    by adding a new checkbox to the workflow window, that enables to choose
    between two strategies for the calculation of dependent content. The value
    of the checkbox is saved in the new workflow variable
    `placeholderPreparationStrategy`, which was added to the 
    `translation-global-link.xml`.

 * With CMCC 2007 the workflow extension point for additional fields was adapted.

    It is now possible to place additional fields (that display or write
    custom process values) anywhere in the `DefaultTranslationWorkflowDetailForm`. 
    An additional field, like the new `GccWorkflowDateTimeField.as` can now
    trigger a workflow validation, which can result in a validation error (e.g.,
    if the value of the dateTimeField is in the past). If you have added
    further `AdditionalWorkflowFields`, you need to make sure, that they are now
    added to the workflow window like the `GccWorkflowDateTimeField.as`.

2004
--------------------------------------------------------------------------------

### Main Changes

* Updated to CoreMedia Content Cloud 10.2004.

* Updated `ActionTestBaseConfiguration` to the latest version of the 
    `TranslatablePredicate` interface.

* Removed `translatableExpressions` configuration from  
    `TranslateGccConfiguration` which is now provided by the Blueprint's 
    multi-site module.
  
* Fixed [CoreMedia/coremedia-globallink-connect-integration#12](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/12)
    to allow the usage of other setting types within the facade. 

    The interfaces of `GCExchangeFacadeProvider`, `GCExchangeFacadeSessionProvider`
    and all its subclasses have been updated to pass `Map<String, Object>` instead
    of `Map<String, String>`. If you have customized these classes or created your
    own subclasses, you will have to update them and make sure to cast the value
    of the settings in your implementation accordingly.

2001
--------------------------------------------------------------------------------

### Main Changes

* Updated to CoreMedia Content Cloud 10.2001.

* Updated the documentation with additional information on the setup of 
    GlobalLink Connect Cloud.

1910
--------------------------------------------------------------------------------

### Main Changes

* Updated to CoreMedia Content Cloud 10.1910.

* Update GCC from 2.3.0 to 2.4.0.

    * `com.coremedia.labs.translation.gcc.facade.GCSubmissionState`:
    
        Adapted to changes of `org.gs4tr.gcc.restclient.model.SubmissionStatus`:
        
        * `IN_PROGRESS`: Has been deprecated (for removal).
        
            The GCC API removed this state completely. You should not rely
            on this state anymore.
            
        * `CANCELLED`: Changes from artificial state to explicit state.
        
            Prior to 2.4.0 cancellation had been tracked as flag at
            a given state. With 2.4.0 the REST backend changes to this
            state explicitly.
            
            Implementation of `DefaultGCExchangeFacade.getSubmissionState`
            has been adopted accordingly.
            
        * `TRANSLATE`: Workaround for bug in 2.3.0 Java REST API removed.
        
            It maps to `SubmissionStatus` directly now.

### Bug Fixes

* #2 Direct cancellation of workflow might result into failure 
* #3 Submission name might be too long
* #5 Links to mock facade documentation in README broken

### Other Changes

* Introduced [GitHub Pages](https://coremedia.github.io/coremedia-globallink-connect-integration/).
* Migrated some documentation to GitHub Pages.
* Introduced `license-maven-plugin` to generate an overview of used third-party
    libraries and licenses.
* Removed PDF documents for third-party libraries. Has been replaced by
    [THIRD-PARTY.txt](docs/THIRD-PARTY.txt).

### General Notes

* The GCC REST Backend changes the response of supported locales to contain
    a trailing space, such as `"fr-FR "` rather than `"fr-FR"`. On parsing,
    you should ensure trimming the result first.
