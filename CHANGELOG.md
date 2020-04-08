Changelog
================================================================================

2001
--------------------------------------------------------------------------------

### Main Changes

* Updated to CoreMedia Content Cloud 10.2001.

* Updated documentation with additional information on setup of 
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
