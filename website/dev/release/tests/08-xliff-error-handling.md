---
description: "Scenario: XLIFF Import Error Handling."
tags:
  - test
  - scenario
  - manual
  - error-handling
---

# XLIFF Error Handling

* **Connector Type**: `mock`
* **Key Type**: _irrelevant_

1. Log in as Rick C.

2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`

    1. `type` is set to `mock`
    2. `mock.scenario` is set to `translate-invalid-xliff`
       (alternative scenario: `translate-string-too-long`)

3. Start a translation for an article.

4. Wait for the workflow to appear in _Workflow App_'s "Open" workflows

    1. The icon should be a warning sign

5. Open the workflow details:

    1. Field "Current Task" should be “Download error”
    2. In the section "More", a download link for the broken XLIFF should be
       shown under "Issue Details"

6. Click the XLIFF download link

    1. Both the XLIFF and an issue details text file should be included

7. Click "Accept Task" and "Next Step"

    1. You should have the ability to “Abort and rollback changes” or
       “Continue and retry”
