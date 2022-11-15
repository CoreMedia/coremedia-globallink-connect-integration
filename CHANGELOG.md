Changelog
================================================================================

2210
--------------------------------------------------------------------------------

### Main Changes

* Updated dependencies to CoreMedia Content Cloud v11.2210.1.

2207
--------------------------------------------------------------------------------

### Main Changes

* Updated dependencies to CoreMedia Content Cloud v11.2207.1.
* Fixed [CoreMedia/coremedia-globallink-connect-integration#48](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/48)

2204
--------------------------------------------------------------------------------

### Main Changes

* Updated dependencies to CoreMedia Content Cloud v11.2204.1.

2201
--------------------------------------------------------------------------------

### Main Changes

* Updated to API v3 of GlobalLink Connect Cloud. The API now uses an API Key 
for authentication instead of username and password. You have to request the
API Key from your contacts at Translations.com.
   
  You will also have to adapt your configuration of the workflow in the content
repository or in your properties file. Remove `username` and `password` and 
set the `apiKey` instead. You will also have to update the `url` to point to v3. 

* A new error code was introduced to reflect the new authentication method  
  via `apiKey` and to be able to distinguish between the different types of 
  issues in the Studio.

* Updated dependencies to match the CMCC v11.2201.2 release.

CoreMedia v11.2110
--------------------------------------------------------------------------------

### Main Changes

* CMCCv11 introduces the Workflow App 🥳 and some changes to the way of handling
  extensions of the Studio Client. The Studio code was migrated to TypeScript
  and adapted to the new API of the Workflow App. The module structure is also
  compatible with the new version of the extension tool for centralized
  extensions.

* Renamed "Reject Changes" step to "Abort and rollback changes" to clearly point
  what actually happens when choosing this option.

* Tweaked log levels so that you can more easily follow state changes in the
  log file.

    * Opening and closing session is now logged at debug level. These messages
      were spamming the log without providing additional details why a session
      was opened at all.

    * Instead, state changing actions like downloading completed locales or
      canceling a submission are now logged at info level.

* When upgrading, you will have to upload workflow definition again to benefit
  from the latest changes:

    * There is a new optional next step in the _Handle Cancelation Error_ task
      that enables continuing with the regular translation process in case a
      submission cannot be canceled anymore at GlobalLink.

    * In v11.2110.1 the `FinalAction` API was introduced in the workflow server to
      archive workflows consistently. `ArchiveProcessFinalAction` now takes care of
      archiving the completed as well as the aborted (escalated) workflows. The workflow
      definition was updated accordingly.

* Renaming branch `master` to `main`

    Follow these steps to rename your local master branch as well:

    ```bash
    # Switch to the "master" branch
    $ git checkout master
    # Rename it to "main"
    $ git branch -m master main
    # Get the latest commits (and branches!) from the remote
    $ git fetch
    # Remove the existing tracking connection with "origin/master"
    $ git branch --unset-upstream
    # Create a new tracking connection with the new "origin/main" branch
    $ git branch -u origin/main
    ```

2107
--------------------------------------------------------------------------------

### Main Changes

* Added dependencies to Maven POM of studio-client module that are required 
    since CoreMedia Content Cloud 10.2107.2.

* Retry delays are now configured in a [properties file](https://github.com/CoreMedia/coremedia-globallink-connect-integration/blob/master/apps/workflow-server/gcc-workflow-server/src/main/resources/META-INF/coremedia/gcc-workflow.properties). 
    The corresponding timer was removed from the [workflow definition](https://github.com/CoreMedia/coremedia-globallink-connect-integration/blob/master/apps/workflow-server/gcc-workflow-server/src/main/resources/com/coremedia/labs/translation/gcc/workflow/translation-global-link.xml).
    The default update interval for pending submission was increased from 3 to
    30 minutes to reduce the number of requests to the GlobalLink Connect Cloud 
    API. If you expect numerous parallel translation submissions, we recommend 
    increasing this interval even further. Please talk to Translations.com to 
    define a reasonable interval.

    For testing purposes these delays can also be set in the GlobalLink settings 
    content. Do not use these settings in the content in a production environment 
    because they also affect running workflows and can cause high loads on 
    GlobalLink's server or unexpectedly long update intervals.

* Added _administratoren_ group the workflows' user tasks so that they can 
    intervene if a workflow is stalled for example.

* The _translation-manager-role_ also received the right to delegate and reject
    offered tasks. These features are however not available in the Studio (yet).

* The source locale of the submission is now also send as part of the file upload 
    call to the gcc API. Thank you @mtommila for suggesting this change and 
    providing a [Pull Request](https://github.com/CoreMedia/coremedia-globallink-connect-integration/pull/37).

    In the context of this change `GCExchangeFacade.uploadContent(String fileName, Resource resource, Locale sourceLocale)` 
    received another parameter `sourceLocale`. Custom implementations of the 
    interface have to be adapted.

2104
--------------------------------------------------------------------------------

### Main Changes

* Updated dependencies to reflect changes to content validators in CoreMedia 
    Content Cloud 10.2104.1.

* Added the possibility to configure the connection to GlobalLink in properties 
    file on the server with [CoreMedia/coremedia-globallink-connect-integration#25](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/25) - 
    Thank you @mtommila for suggesting this change and providing a Pull Request.

* The notes from the Start Translation Workflow Window are now sent as instructions 
    to GlobalLink. Optionally, the submitter's name can be added to the submission. 
    The workflow is also prepared to take an optional string that represents the 
    type of the workflow on the GlobalLink side. This still requires the UI to 
    be implemented in Studio. - [CoreMedia/coremedia-globallink-connect-integration#26](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/26) - 
    Thank you @mtommila for suggesting this change and providing a Pull Request.

* Fixed concurrency issue of `DefaultGCExchangeFacadeSessionProvider` which 
  could cause `java.util.NoSuchElementExceptions` [CoreMedia/coremedia-globallink-connect-integration#33](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/33)

2101
--------------------------------------------------------------------------------
### General Notes

* This release requires at least CoreMedia Content Cloud 10.2101.3.

### Main Changes

* Updated validators and validation configuration to align with improvements in 
    CoreMedia Content Cloud 10.2101. 

* Internal API `ILocalesService` was moved and renamed in 2101 AEP. Updated 
    usages. There is no public API for this functionality yet.

* Added support for bulk cancellation of workflows

* Fixed [CoreMedia/coremedia-globallink-connect-integration#18](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/18)

* Fixed [CoreMedia/coremedia-globallink-connect-integration#23](https://github.com/CoreMedia/coremedia-globallink-connect-integration/issues/18)

    The API of the `GCExchangeFacade` was updated to also return the submission identifier 
    shown in GlobalLink Project Director and Connect Cloud. If you had accessed the
    the submission state through `GCExchangeFacade.getSubmissionState(submissionId)`, you have
    to migrate to `GCExchangeFacade.getSubmission(submissionId).getState()`.

2010
--------------------------------------------------------------------------------

### Main Changes

* Replace `GccWorkflowDateTimeField` of `gcc-studio-client` with built-in 
    `WorkflowDateTimeField`.

* Replaced usage of internal API usage of `MessageBoxInternal` with newly introduced 
    public API `MessageBoxUtil`.

* Internal API `LocaleService` was moved and renamed in 2010 AEP. Updated usages. 
    There is no public API for this functionality yet.

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
