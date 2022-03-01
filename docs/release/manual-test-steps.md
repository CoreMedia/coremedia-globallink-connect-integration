# Manual Test Steps

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------

## Scenarios

1. [Contract Test](#scenario-contract-test)
2. [Happy Path](#scenario-happy-path)
3. [Cancellation](#scenario-cancellation)
4. [Error Handling](#scenario-error-handling)
5. [XLIFF Import Error Handling](#scenario-xliff-import-error-handling)

## Prepare

For quicker test feedback set the following integer parameters in the 
GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
* sendTranslationRequestRetryDelay -> 60
* downloadTranslationRetryDelay -> 60
* cancelTranslationRetryDelay -> 60
* retryCommunicationErrors -> 1

## Scenario: Contract Test

Especially on updates of `gcc-restclient` you should run the half-automatic
test `DefaultGCExchangeFacadeContractTest`. Read the JavaDoc to know how to
configure and run the test. 
Lately, when using the automatic workflow, the submission is not marked as 
`DELIVERED` anymore. This is why the `translateXliff` test fails. It seems to 
work with the manual workflow though.

In order run the test you need to create a file with the name [.gcc.properties](example.gcc.properties.txt) in your user 
home folder

## Scenario: Happy Path

1. Login as Rick C.
2. Open the GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
    1. type should not be set to “mock”
    2. dayOffsetForDueDate should be set to 20
    3. Credentials for gcc should be entered (automatic workflow key)
3. Choose an article and drag it into the “Localization Workflows” drop area
    1. A StartTranslation window should pop up
    2. There should be no warnings or errors
    3. The Due Date should be set to the current dateTime plus 20 days
4. Click “Start”
    1. The dialog should close without any error
    2. In the "Running" area from “Localization Workflows” the process should 
   pop up
    3. Double Click on the Workflow to open it the Workflow App
5. The workflow can be tracked in the detail view in the Workflow App
    1. After some time the Status, Submission ID, Due Date are shown
    2. As long as the Workflow is in "Running" the variable “Completed Locales” 
   displays “0 Locales”
6. Wait for the workflow to be finished
    1. The Workflow should disappear from the "Running" workflows in the Workflow 
       App and appear on the “Open” list.
    2. In the Content App the Workflow should now also be shown in the Inbox.
7. Select the Workflow in your “Open” list in the Workflow App
    1. The “Status” variable should display “Delivered”
    2. The “Completed Locales” variable should display the target locales of 
   the workflow
    3. When clicking on the translated content, it opens in the comparison view 
   to its master content in the Content App
    4. The text should be pseudo translated
8. Click “Accept Task” in the Workflow App and then "Next Step" -> 
“Finish Content Localization”
    1. The workflow should disappear from “Open”
    2. The workflow should appear in “Closed”


## Scenario: Cancellation

1. Login as Rick C.
2. Open the GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
    1. type should not be set to “mock”
    2. dayOffsetForDueDate should be set to 20
    3. Credentials for gcc should be entered (manual workflow key)
3. Choose an article and drag it into the “Localization Workflows” drop area
    1. A StartTranslation window should pop up
    2. There should be no warnings or errors
    3. The Due Date should be set to the current dateTime plus 20 days
4. Click “Start”
    1. The dialog should close without any error
    2. In the "Running" area from “Localization Workflows” the workflow should 
       pop up
5. Open the "Running" area and select the started workflow
    1. A “X” should be activated in the header bar
    2. Multi-selection is possible. If you select a "normal" and a GlobalLink 
       workflow, the button remains deactivated. It is also  possible to select and 
       cancel multiple GlobalLink workflows at once.
6. Press the “X”
    1. A dialog should pop
7. Confirm the dialog
    1. The Dialog should close
    2. The icon of the workflow should change (now it has a little “x” on the 
   bottom right)
    3. The “X” should appear disabled in the header bar 
    4. The workflow should disappear in the "Running" list after some time
8. Depending on timing there might be a “Review Cancellation” task or a "Handle
Cancellation Error" task in your open workflows. Or, the workflow will be 
directly cancelled, if the workflow at GlobalLink completed in the meantime.
9. In case of the “Handle Cancellation Error” Task, try out "Continue 
translation" or "Abort and rollback without cancelling at GlobalLink.".
10. After regularly reviewing the cancellation open the "Closed" list in the
Workflow App.
    1. The workflow should appear here
    2. The “Status” variable should display “Cancelled” (not sure why but the
    automatic workflow shows "Delivered")
    3. The icon should mark the workflow as cancelled (little “x” on the 
    bottom right)
12. Login to the GlobalLink dashboard and the Project Director and verify that
the workflow appears as "Cancelled" here as well.

## Scenario: Submitter and Instructions

1. Login as Rick C.
2. Open the GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
    1. type should not be set to “mock”
    3. Credentials for gcc should be entered (manual workflow key)
    4. Activate the boolean property "isSendSubmitter"
3. Choose an article and drag it into the “Localization Workflows” drop area
    1. A StartTranslation window should pop up
    2. Add a text to the notes field
4. Click “Start”
    1. The dialog should close without any error
    2. In the "Running" area from “Localization Workflows” the workflow should 
   pop up
5. Login to the GlobalLink Connect Dashboard
    1. The submitter and the instructions added to the notes field are visible 
   in the submission's detail view
    2. Not required - If you login to the Project Director the submitter is set 
   to the API user. Instructions should be available though.
6. Switch back to the Studio or Workflow App and cancel the workflow in the 
pending list.

## Scenario: Cancellation in GlobalLink

1. Login as Rick C.
2. Open the GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
    1. type should not be set to “mock”
    2. dayOffsetForDueDate should be set to 20
    3. Credentials for gcc should be entered (manual workflow key)
3. Choose an article and drag it into the “Localization Workflows” drop area
    1. A StartTranslation window should pop up
    2. There should be no warnings or errors
    3. The Due Date should be set to the current dateTime plus 20 days
4. Click “Start”
    1. The dialog should close without any error
    2. In the "Running" area from “Localization Workflows” the workflow should 
   pop up
5. Login to GlobalLink project director by using the credentials for the manual
    workflow account.
6. Login to the GlobalLink dashboard and cancel the workflow.
7. Go back to the Workflow App
    1. The workflow appears in Rick's Open workflows.
    2. Rick can only choose to abort and rollback the previous changes and
   accept the cancellation.
    3. Choose "Abort and rollback changes"
8. Open the "Closed" area
    1. The workflow should appear here
    2. The “Status” variable should display “Cancelled”
    3. The icon should mark the workflow as cancelled (little “x” on the 
   bottom right)
  
## Scenario: Error Handling

1. Login as Rick C.
2. Open the GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
    1. type should not be set to “mock”
    2. dayOffsetForDueDate should be set to 20
    3. Credentials for gcc should be entered (automatic workflow key)
3. Choose an article and drag it into the “Localization Workflows” drop area
    1. A StartTranslation window should pop up
    2. There should be no warnings or errors
    3. The Due Date should be set to the current dateTime plus 20 days
4. Reset the “username” in the GlobalLinkSettings to something wrong
5. Click “Start”
    1. The dialog should close without any error
    2. In the "Running" area from “Localization Workflows” the workflow should 
   pop up
6. Wait for the Workflow to appear in the open workflows
    1. The icon should be a Warning sign
    2. The TaskName should be “Upload Error”
    3. You have the ability to “Abort and rollback changes” or 
   “Continue and retry”
    4. A click on “Abort and rollback changes” should perform a Rollback of the 
   content (if you perform that you need to redo steps from 1 to 6)
7. Reset the “username” to its valid value and click “Continue and retry”
8. Select the workflow in "Running" and wait for the Status to change to 
“Translate” or for the Submission ID to be set
9. Reset the “username” in the GlobalLinkSettings to something wrong
10. Wait for the Workflow to appear in the open workflows
    1. The icon should be a Warning sign
    2. The TaskName should be “Download Error”
    3. You have the ability to “Abort and rollback changes” or 
    “Continue and retry”
11. Click “Abort and rollback changes”
12. Wait for the Workflow to appear in the open workflows
    1. The icon should be a Warning sign
    2. The TaskName should be “Cancellation Error”
    3. You have the ability to “Abort and rollback without cancelling the 
    submission at GlobalLink.” or “Continue and retry”
13. Continue and retry should lead to a cancellation of the workflow, 
"Abort and rollback changes" should lead to a direct rollback

## Scenario: XLIFF Import Error Handling

1. Login as Rick C.
2. Open the GlobalLinkSettings `/Settings/Options/Settings/GlobalLink`
    1. type should be set to “mock”
    2. dayOffsetForDueDate should be set to 20
    3. mockError should be set to “DOWNLOAD_XLIFF”
3. Choose an article and drag it into the “Localization Workflows” drop area
    1. A StartTranslation window should pop up
    2. There should be no warnings or errors
    3. The Due Date should be set to the current dateTime plus 20 days
4. Click “Start”
    1. The dialog should close without any error
    2. In the "Running" area from “Localization Workflows” the workflow should 
   pop up
5. Wait for the Workflow to appear in the open workflows
    1. The icon should be a Warning sign
    2. The TaskName should be “Download Error”
    3. You have the ability to “Abort and rollback changes” or 
   “Continue and retry”
    4. There should be a field “Issue Details” with clickable link that links to 
   a download of the broken XLIFF

--------------------------------------------------------------------------------

\[[Up](README.md)\] \[[Top](#top)\]

--------------------------------------------------------------------------------
