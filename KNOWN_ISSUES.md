# Known Issues (and Open Questions)

## GCC RestClient Issues

* translations-com/globallink-connect-cloud-api-java#1:

    SubmissionStatus (Model): Misses State "Translate"
    
    SubmissionStatus: While the REST API returns a state "Pre-process", 
    the state in the gcc-restclient 2.2.1 is still "In Pre-process"
    
## Open Questions

### Submission Locales

What do the submission locales refer to? From Connectors - Config:

* `locale_label`,
* `pd_locale`, or
* `connector_locale`?
