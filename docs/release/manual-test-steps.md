# Manual Test Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#manual-test-steps)\]

--------------------------------------------------------------------------------

## Scenarios

1. [Contract Test](#scenario-contract-test)
2. [Happy Path](#scenario-happy-path)
3. [Cancellation](#scenario-cancelation)
4. [General Error Handling](#scenario-general-error-handling)
5. [XLIFF Import Error Handling](#scenario-xliff-import-error-handling)
6. [Redelivered State Handling](#scenario-redelivered-state-handling)

## Prepare

Adjust GlobalLink-Settings within settings document at
`/Settings/Options/Settings/Translation Services/GlobalLink` for quicker
feedback during manual test-steps as well as to access the relevant sandbox
via corresponding credentials:

```json5
{
  "globalLink": {
    // [... other settings]
    // key (type: String)
    // Adjust either for automatic or manual GCC processing. To ease
    // switching keys, hold them in the prepared extra Struct
    // at `additionalConfigurationOptions.exampleKeys` for an easy copy & paste:
    "key": "...",
    // apiKey (type: String)
    // Only used for testing. Otherwise, expected to be set via Spring for
    // security reasons.
    "apiKey": "...",
    // type (type: String)
    // Adjust to "default" for processing at GCC backend or "mock" for manual
    // processing. See also: `additionalConfigurationOptions.availableTypes`.
    "type": "default",
    // isSendSubmitter (type: Boolean)
    // Adjust to `true` to send the submitter with the request.
    "isSendSubmitter": false,
    // submissionInstructionType (type: String)
    // Adjust to "text", for some error behavior tests.
    "submissionInstructionType": "text-bmp",
    // Section: Timing Adjustments — Get Faster Feedback on Manual Test Steps
    // Despite `retryCommunicationErrors`, which just needs to be adjusted,
    // find copy & paste ready values for the other settings in the
    // `additionalConfigurationOptions` Struct.  
    "sendTranslationRequestRetryDelay": 60,
    "downloadTranslationRetryDelay": 60,
    "cancelTranslationRetryDelay": 60,
    "retryCommunicationErrors": 1,
    // Section: Mock Settings
    // Referenced below, like to provoke errors are certain states.
    "mock": {
      // [...]
    }
  }
}
```

## General Advice for Manual Test Steps Within CoreMedia Studio

* It is recommended to add the settings document
  `/Settings/Options/Settings/Translation Services/GlobalLink` to the
  "Favorites" in Studio. It is required to be adapted multiple times during
  the manual test steps.

* **Start Workflow**: There are various ways to start localization workflows.
  Choose one of them during your test-steps and possibly switch to others for
  further steps. Some available options are:
    * Drag an article into the "Localization Workflows" drop area in Control
      Room.
    * Choose "Start Translation Workflow" within the Locale Switcher of an
      opened document.

* **Workflow Interaction**: Just as for when starting the workflow, you
  have multiple ways to interact with the workflow. Choose one of them during
  your test-steps and possibly switch to others for further steps. Some
  available options are:
    
    * **In Localization Nagbar**: To directly switch from a document to its
      assigned localization workflow, click on one of the available options in
      the nagbar, which is either clicking the link, to jump to the
      _Workflow App_ or click on the button, to open the workflow within a
      sidebar.

    * **Control Room**: Provides an overview of running workflows and allows
      you to interact with them.

    * **Workflow App**: Provides a more detailed view of the workflow.

    * **Workflow Sidebar**: Opened within the Content App, providing an
      overview of the workflow.

* **Studio Localization**: It is good practice, to switch the language of the
  Studio UI from time to time, to ensure also German labels are available.

## Scenario: Contract Test

Especially on updates of `gcc-restclient` you should run the half-automatic
test `DefaultGCExchangeFacadeContractTest`. Read the JavaDoc to know how to
configure and run the test.
Lately, when using the automatic workflow, the submission is not marked as
`DELIVERED` anymore. This is why the `translateXliff` test fails. It seems to
work with the manual workflow though.

In order run the test you need to create a file with the name
[.gcc.properties](example.gcc.properties.txt) in your user
home folder

### Manual Review

Please review the following points in the management dashboard of GlobalLink
manually (all submissions created by this test should have a name starting with
`CT`:

* **Instructions:** The tests `shouldRespectInstructions` should have created
  submissions with so-called "submission instructions" (in the workflow:
  "Notes") for these scenarios (by ID):

  * `BMP` (refers to Base Multilingual Plane)

    It is expected that each described character is represented visually as
    described within the test fixture. Thus, arrows should be visible as arrows
    and even high level Unicode character from BMP should be displayed correctly
    (e.g., the "Fullwidth Exclamation Mark": `！`).

  * `FORMAT` (refers to newlines and tabs)

    As instructions in the GCC backend are expected to be HTML, the newlines
    should be replaced by `<br>` and tabs by `&nbsp;&nbsp;`. In other words:
    You should see visible line-breaks and indents.
  
  * `HTML_AS_TEXT`

    Given the assumption, that in the Workflow App the instructions are given in
    plain-text, the HTML should be escaped. Thus, you should see no bold text,
    no newline triggered by a textual `<br>`, and also entities like `&amp;`
    should be visible as plain-text.

  * `UNICODE_SMP` (`SMP` refers to the Supplementary Multilingual Plane)

    For now, the GCC backend is expected to not support SMP characters. Thus,
    corresponding characters like emojis (dove, for example: 🕊) should be
    replaced by a placeholder pattern. Current implementation is, to make them
    distinguishable, that the placeholder is the Unicode code point in hex, such
    as `U+1F54A`.

* **Submitter**: The test `shouldRespectSubmitter` should have created
  submissions with overridden submitter names (thus, must be different to the
  account username). Three submissions should be visible:

  * `YES`: The submitter name should be the test's name.
  * `NO`: The submitter name should be the account username.
  * `DEFAULT`: The submitter name should be the account username.

## Scenario: Happy Path

1. **User Rick**: Log in as Rick C.
2. **Adjust Settings**: Settings to adjust/verify in
   `/Settings/Options/Settings/Translation Services/GlobalLink`:

   ```json5
    {
      "globalLink": {
        "key": "<automatic workflow connector key>",
        "apiKey": "...",
        "type": "default",
        "dayOffsetForDueDate": 20,
        "isSendSubmitter": true,
        // Adjusted retry settings, see above for details.
        // [...]
      }
    }
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
       shown
    2. As long as the workflow is in "Running", field “Completed Locales”
       should display “0 Locales”
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

## Scenario: Cancelation

### Cancelation in Studio

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “default”
    2. Credentials for gcc are entered (**manual** workflow key)
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. There should be no warnings or errors
4. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
5. Select the workflow and click "X"
    1. A dialog should pop
6. Confirm the dialog
    1. The dialog should close
7. Go to the _Workflow App_'s overview
    1. After a while, the workflow should appear in Rick's "Closed" workflows
8. Double-click the canceled workflow
    1. Field “Status” should display “Canceled”
    2. The icon should mark the workflow as canceled (little “x” on the
       bottom right)

### Cancelation in GlobalLink

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “default”
    2. Credentials for gcc are entered (**manual** workflow key)
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. There should be no warnings or errors
4. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
5. Log in to the _GlobalLink Management Dashboard_ and cancel the workflow
6. Go back to the _Workflow App_
    1. After a while, the workflow should appear in Rick's "Open" workflows
    2. When accepting the task, Rick can only choose to abort and rollback the
       previous changes and accept the cancelation
    3. Choose "Abort and rollback changes"
7. Open the "Closed" area and double-click the canceled workflow
    1. Field “Status” should display “Canceled”
    2. The icon should mark the workflow as canceled (little “x” on the
       bottom right)

### Cancelation Failure Handling

1. Log in as **Rick C**. Required to use Rick C here, as Adam, for example, will
   not be able to do the multi-selection test, as local translations will appear
   in "Open" section for Adam.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “default”
    2. Credentials for gcc are entered (**manual** workflow key)
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. There should be no warnings or errors
4. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
5. Repeat workflow creation twice, once with a "Translation" workflow and once
   with a "Translation with GlobalLink" workflow
6. Open the "Running" area of Control Room's “Localization Workflows”
    1. An “X” should be activated in the header bar, if a "Translation with
       GlobalLink" workflow is selected
    2. An “X” should be activated in the header bar, if two "Translation with
       GlobalLink" workflows are selected (multi-selection)
    3. No “X” should be activated in the header bar, if a "Translation"
       workflow is selected
    4. No “X” should be activated in the header bar, if a "Translation"
       workflow and a "Translation with GlobalLink" workflow are selected
       (multi-selection)
7. Open the "Running" section in Workflow App.
    1. “X Cancel” should be available in three-dot-menu, if a "Translation with
       GlobalLink" workflow is selected
    2. “X Cancel” should be available in three-dot-menu, if two "Translation
       with GlobalLink" workflows are selected (multi-selection)
    3. “X Cancel” should **not** be available in three-dot-menu, if a
       "Translation" workflow is selected
    4. “X Cancel” should **not** be available in three-dot-menu, if a
       "Translation" workflow and a "Translation with GlobalLink" workflow are
       selected (multi-selection)
8. **Break it:** Change, for example `globalLink.key` to some invalid value
   like adding a `-` (minus) as first character. This will cause subsequent
   calls to GlobalLink to fail.

   Note, that "speed now matters", thus quickly proceed with the next step, as
   otherwise the communication error may cause other effects. The actual time
   depends on the configured retry-count as well as the configured timeouts.
   Typically, you will have at least 60 seconds.
9. Select both "Translation with GlobalLink" workflows and click the “X”
   (either in Workflow App or Control Room, as you like)
    1. A dialog should pop
10. Confirm the dialog
    1. The dialog should close
    2. The “X” should appear disabled in the header bar
    3. in the _Workflow App_, the workflows' icons should change (now they have
       a little “x” on the bottom right)
    4. The workflows should disappear from the "Running" list after some time
       (until then the status in _Workflow App_'s overview will signal that
       we are awaiting the cancelation to be processed, and the workflow detail
       view should have a corresponding signal in fields "Status" and
       "Current Task").
    5. Field "Current Task" in _Workflow App_'s detail view should
       show "Cancelation Error".
11. Click "Accept Task" and "Next Step"
    1. Options "Abort and rollback without canceling at
       GlobalLink", "Retry cancelation", and "Continue translation" should be
       presented
    2. Click "Abort and rollback without canceling at GlobalLink" and open the
       target site's document
        1. The document should remain in its previous version
    3. Go back to the _Workflow App_ overview
        1. The workflow should be moved from the "Running" to the "Closed" list
        2. The “Status” field in _Workflow App_'s overview should display
           “Workflow completed”
        3. The icon should mark the workflow as canceled (little “x” on the
           bottom right)

## Scenario: General Error Handling

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “default”
    2. Credentials for gcc are entered (**automatic** workflow key) **but**
       _apiKey_ (aka _API Token_) is set to something wrong. Add a `-` (minus),
       for example, to the beginning.
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. There should be no warnings or errors
4. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
5. Wait for the workflow to appear in _Workflow App_'s "Open" workflows
    1. The icon should be a warning sign
    2. The "Current Task" in _Workflow App_'s detail view should be “Upload
       Error”
6. Click "Accept Task" and "Next Step"
    1. You have the ability to “Abort and rollback changes” or
       “Continue and retry”
    2. A click on “Abort and rollback changes” should perform a rollback of the
       content (if you perform that you need to redo steps from 1 to 4)
7. Set _apiKey_ in GlobalLink settings to its valid value and click “Continue
   and retry”
8. Double-click the workflow in "Running" and wait for the field "Status" to
   change to “Translate” or for the field "Submission ID" to be set
9. Set _apiKey_ in GlobalLink settings to something wrong
10. Wait for the workflow to appear in _Workflow App_'s "Open" workflows
    1. The icon should be a Warning sign
    2. Column "Status" should show “Download Error”
    3. When accepting the task, you have the ability to “Abort and rollback
       changes” or “Continue and retry”
11. Click “Abort and rollback changes”
12. Wait for the workflow to appear in _Workflow App_'s "Open" workflows
    1. The icon should be a warning sign
    2. Column "Status" should show “Cancelation Error”
13. Double-click the workflow and click "Accept Task" and "Next Step"
    1. You have the ability to “Abort and rollback without canceling at
       GlobalLink.”, "Retry cancelation", or “Continue Translation”
14. Set _apiKey_ in GlobalLink settings to its valid value and click "Continue
    Translation"
    1. After some time, the workflow appears in _Workflow App_'s "Open"
       workflows
15. Accept and finish the workflow
    1. There should not be further errors

## Scenario: XLIFF Import Error Handling

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “mock”
    2. _mock.error_ is set to “DOWNLOAD_XLIFF”
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. There should be no warnings or errors
4. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
5. Wait for the workflow to appear in _Workflow App_'s "Open" workflows
    1. The icon should be a warning sign
6. Double-click the workflow
    1. Field "Current Task" should be “Download error”
    2. In the section "More", a download link for the broken XLIFF should be
       shown under "Issue Details"
7. Click the XLIFF download link
    1. Both the XLIFF and an issue details text file should be included
8. Click "Accept Task" and "Next Step"
    1. You should have the ability to “Abort and rollback changes” or
       “Continue and retry”

## Scenario: Redelivered State Handling

It has been observed, that, if the XLIFF is broken, it is not necessarily fixed
within the GCC backend. Instead, the workflow is marked as "Redelivered", and
the XLIFF is provided by different means, such as sent via email.

The following steps simulate this scenario:

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
   1. _type_ is set to “mock”
   2. _mock.error_ is set to “DOWNLOAD_XLIFF”
   3. **Submission State Mocking:** Add (or activate) the following mock
      settings:
      ```json5
      {
        "mock": {
          // [...]
          "submissionStates": {
            "Completed": {
              // Directly after "Completed" state, the submission is marked as
              // "Redelivered".
              "after": "Redelivered",
              // Mark the state as final, not to continue with the next
              // (standard) state.
              "final": true
            }
          }
        }
      }
      ```
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
   1. A window _Localization Workflow_ should pop up
   2. There should be no warnings or errors
4. Click “Start”
   1. The dialog should close without any error
   2. A new workflow process should pop up in the "Running" area of Control
      Room's “Localization Workflows”
5. Wait for the workflow to appear in _Workflow App_'s "Open" workflows
6. Double-click the workflow
   1. Field "Current Task" should be “Review Translation (redelivered)”
   2. Status should be “Redelivered”
   3. In the section "More", a download link for the broken XLIFF should be
      shown under "Issue Details" (if you skipped setting `mock.error` the
      field is not available).
7. Click the XLIFF download link
   1. Both the XLIFF and an issue details text file should be included
8. In the _Workflow App_, click “Accept Task”, "Next Step", “Finish Content
   Localization”, and "Yes, continue"
   1. The workflow should disappear from “Open”
   2. The workflow should appear in “Closed”

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#manual-test-steps)\]

--------------------------------------------------------------------------------
