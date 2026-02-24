---
description: "Scenario: General Error Handling"
tags:
  - test
  - scenario
  - manual
  - error-handling
---

# General Error Handling

* **Connector Type**: `default`
* **Key Type**: `automatic`

1. Log in as Rick C and open Content as well as Workflow App.

2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`
   1. `type` is set to `default`
   2. Credentials for gcc are entered (**automatic** workflow key)

3. **Break it:** Change the `apiKey` to something wrong, for example, add a `-`
   (minus) to the beginning.

4. Start a translation workflow for an article.

5. Wait for the workflow to appear in _Workflow App_'s "Open" workflows

   1. The icon should be a warning sign
   2. The "Current Task" in _Workflow App_'s detail view should be “Upload
      Error”

6. Click "Accept Task" and "Next Step"

   1. You have the ability to “Abort and rollback changes” or
      “Continue and retry”
   2. A click on “Abort and rollback changes” should perform a rollback of the
      content (if you perform that you need to redo steps above)

7. Set `apiKey` in GlobalLink settings to its valid value and click “Continue
   and retry”

8. Open the workflow's details in the Workflow App and wait for this state
   before you continue:

   1. The status should change to “Translate”.
   2. The "Submission ID" should be set.

9. Set `apiKey` in GlobalLink settings to something wrong again.

10. Wait for the task to change to "Download Error".

11. Accept the task.

    1. Validate available options when pressing "Next" are:
       1. Abort and rollback changes
       2. Continue and retry

12. Select “Abort and rollback changes” (which will subsequently try to
    trigger a cancellation at GlobalLink).

13. Wait for the current task to change to "Cancellation Error".

    1. The icon should signal a warning.

14. Accept the task.

    1. Validate available options when pressing "Next" are:

        1. Abort and rollback without canceling at GlobalLink
        2. Retry cancellation
        3. Continue translation

15. Set `apiKey` in GlobalLink settings to its valid value.

16. Choose "Continue translation"

17. The workflow should continue without errors translating the document.
