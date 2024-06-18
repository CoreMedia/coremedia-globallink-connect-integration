# Manual Test Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

## Scenarios

1. [Contract Test](#scenario-contract-test)
2. [Happy Path](#scenario-happy-path)
3. [Submitter and Instructions](#scenario-submitter-and-instructions)
4. [Cancellation](#scenario-cancelation)
5. [Error Handling](#scenario-error-handling)
6. [XLIFF Import Error Handling](#scenario-xliff-import-error-handling)

## Prepare

Adjust GlobalLink-Settings within settings document at
`/Settings/Options/Settings/Translation Services/GlobalLink` for quicker
feedback during manual test-steps as well as to access the relevant sandbox
via corresponding credentials:

```json5
{
    "globalLink": {
        // ... other settings
        "key": "...", // String; adjust either for automatic or manual GCC processing
        // Suggestion to ease switching keys is to hold them in an extra
        // Struct for easy copy & paste:
        "keys": {
            "automatic": "...", // String
            "manual": "...", // String
        },
        "apiKey": "...", // String; private key, typically stored in Spring configuration
        "type": "default", // String
        "isSendSubmitter": false, // Boolean; changed within manual test steps
        "sendTranslationRequestRetryDelay": 60, // Integer
        "downloadTranslationRetryDelay": 60, // Integer
        "cancelTranslationRetryDelay": 60, // Integer
        "retryCommunicationErrors": 1, // Integer; change existing value
    }
}
```

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

## Scenario: Happy Path

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “default”
    2. _dayOffsetForDueDate_ is set to 20
    3. Credentials for gcc are entered (**automatic** workflow key)
3. Create an article A and remove validation issues.
4. Create an article B and remove validation issues.
5. Create link from article A to article B.
6. Drag article A into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. There should be no warnings or errors
    3. Field _Due Date_ should be set to the current date/time plus 20 days
    4. Article B should be added as dependent content.
7. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
8. Double-Click the workflow
    1. A sidebar with workflow information should open
9. Click "Open workflow details"
    1. A new tab with the _Workflow App_'s detail view for the workflow should
       open
    2. After some time, _Status_, _Submission ID_, and _Due Date_ should be
       shown
    3. As long as the workflow is in "Running", field “Completed Locales”
       should display “0 Locales”
10. Click "Go to overview" and wait for the workflow to be finished
    1. The workflow should disappear from the "Running" workflows and appear on
       the “Open” list.
    2. The workflow should now also be shown in the "Open" workflows of Control
       Room's “Localization Workflows”.
    3. You should receive a notification, that the workflow is now available
       in your inbox.
11. Double-click the workflow in _Workflow App_'s “Open” list
    1. The “Status” field should display “Delivered”
    2. The “Completed Locales” field should display the target locales of
       the workflow
    3. When clicking on the translated content, it should open in language
       comparison view
       in _Studio_
    4. The text should be pseudo-translated
12. In the _Workflow App_, click “Accept Task”, "Next Step", “Finish Content
    Localization”, and "Yes, continue"
    1. The workflow should disappear from “Open”
    2. The workflow should appear in “Closed”

## Scenario: Submitter and Instructions

1. Log in as Rick C.
2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
    1. _type_ is set to “default”
    2. Credentials for gcc are entered (**manual** workflow key)
    3. Boolean property _isSendSubmitter_ is activated
3. Choose an article and drag it into Control Room's “Localization Workflows”
   drop area
    1. A window _Localization Workflow_ should pop up
    2. Add a text to field _Notes_
4. Click “Start”
    1. The dialog should close without any error
    2. A new workflow process should pop up in the "Running" area of Control
       Room's “Localization Workflows”
5. Log in to the **GlobalLink Management Dashboard** (not Project Director,
   as the PD, for example, does not display the "submitter" as set in request)

    1. The submitter and the instructions added to field _Notes_ are visible
       in the submission's detail view.
6. Switch back to Studio or _Workflow App_ and cancel the workflow in the
   "Running" list.

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
       we are awaiting the cancelation to be processed and the workflow detail
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
8. Double-click the workflow in "Running" and wait for field "Status" to change
   to
   “Translate” or for field "Submission ID" to be set
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
    2. _mockError_ is set to “DOWNLOAD_XLIFF”
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
    2. In section "More", a download link for the broken XLIFF should be shown
       under "Issue Details"
7. Click the XLIFF download link
    1. Both the XLIFF and an issue details text file should be included
8. Click "Accept Task" and "Next Step"
    1. You should have the ability to “Abort and rollback changes” or
       “Continue and retry”

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
