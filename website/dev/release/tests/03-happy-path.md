---
description: "Scenario: Happy Path"
tags:
  - test
  - scenario
  - manual
---

# Happy Path

* **Connector Type**: `default`
* **Key Type**: `automatic`

## Quick Steps

1. Log in as **Rick C**.
2. Use `globalLink.type=default` and the **"automatic"** connector key.
3. Link two articles A → B.
4. Start a translation workflow for the article A.
5. Validate the start workflow window to show no issues and to have a due date
   set 20 days from now.
6. Start the workflow.
7. In the Workflow App, see how information are continuously updated, like
   the state, the submission ID, and the completed locales.
8. Wait until the user-task "Review delivered translation" is offered to you.
9. Validate that the target articles have received an updated
   (pseudo-)translation.

## Detailed Steps

1. **User Rick**: Log in as Rick C.

2. **Adjust Settings**: Settings to adjust/verify in
   `/Settings/Options/Settings/Translation Services/GlobalLink`:

   ```yaml
   globalLink:
     key: "<automatic workflow connector key>"
     apiKey: "..."
     type: "default"
     dayOffsetForDueDate: 20
     isSendSubmitter: true
     # Adjusted retry settings, see above for details.
     # [...]
   ```

3. **Article A**: Create an article A and remove validation issues.

4. **Article B**:Create an article B and remove validation issues.

5. **A → B**: Create a link from the article A to the article B.

6. **Start Workflow Window**: Trigger starting the workflow and validate the
   default values:

    1. **No Issues**: There should be no warnings or errors
    2. **Due Date**: Field _Due Date_ should be set to the current date/time plus 20 days
    3. **Dependent Content**: Article B should be added as dependent content.
    4. **Notes**: Add some text to the field _Notes_.

7. **Start Workflow**: Click “Start”

    1. The dialog should close without any error
    2. A nagbar should appear in the document form, stating that the documents
       are being translated.

8. **Workflow App**: Navigate to the workflow details within the _Workflow App_.

9. **Workflow Details (In Progress)**: In the workflow's detail view, validate:

    1. After some time, _Status_, _Submission ID_, and _Due Date_ should be
       shown.
    2. As long as the workflow is in "Running", field “Completed Locales”
       should display “0 Locales”.

10. **Workflow Details (Done, Review)**: Later in the workflow's detail view,
    validate:

    1. The “Status” field should display “Delivered”
    2. The “Completed Locales” field should display the target locales of
       the workflow
    3. When clicking on the translated content, it should open in language
       comparison view in CoreMedia Studio.
    4. The text is expected to be pseudo-translated (behavior provided by the
       GCC backend).

11. **Finish Workflow**: In the _Workflow App_, click “Accept Task”,
    "Next Step", “Finish Content Localization”, and "Yes, continue"

    1. The workflow should disappear from “Open”
    2. The workflow should appear in “Closed”

In addition to the above steps and in addition to the results from the
contract test, you may want to review in GCC Management Dashboard:

* That `Rick C` is set as the submitter for the submission.
* That the notes are visible in the submission (as instructions).
