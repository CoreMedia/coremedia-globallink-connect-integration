# Known Issues (and Open Questions)

## DownloadFromGlobalLinkAction: Failure Message Flood

If a download fails (in the following example, invalid/unexpected XLIFF), `DownloadFromGlobalLinkAction`
files a warning. While this is ok, for the first time, the warning is actually
filed on every pull from GCC server which may end in a flood of messages.

Example message for such a failure report:
 
```log
2018-11-30 14:49:56 [WARN]  ccbwtgcc.DownloadFromGlobalLinkAction [] -
  XLIFF has major issues: [XliffImportResultItemImpl@531868202[
    code = EMPTY_TRANSUNIT_TARGET,
    content = Content[coremedia:///cap/content/9944],
    property = localSettings..callToActionCustomText,
    severity = MAJOR],
  XliffImportResultItemImpl@2060602118[
    code = EMPTY_TRANSUNIT_TARGET,
    content = Content[coremedia:///cap/content/9956],
    property = localSettings..callToActionCustomText,
    severity = MAJOR]] (LongActionManager-10)
```

## Open Questions

### Submission Locales

What do the submission locales refer to? From Connectors - Config:

* `locale_label`,
* `pd_locale`, or
* `connector_locale`?
