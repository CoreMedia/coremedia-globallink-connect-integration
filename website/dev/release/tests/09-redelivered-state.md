---
description: "Scenario: Redelivered State Handling."
tags:
  - test
  - scenario
  - manual
---

# Redelivered State

* **Connector Type**: `mock`
* **Key Type**: _irrelevant_

It has been observed, that, if the XLIFF is broken, it is not necessarily fixed
within the GCC backend. Instead, the workflow is marked as "Redelivered", and
the XLIFF is provided by different means, such as sent via email.

The following steps simulate this scenario:

1. Log in as Rick C.

2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`

   1. `type` is set to `mock`
   2. Set `retryCommunicationErrors` to `0` (assumed to ease state mocking)
   3. `mock.error` is set to `DOWNLOAD_XLIFF`
   4. **Submission State Mocking**: Add (or activate) the following mock
      settings:

      ```yaml
      mock:
        # [...]
        submissionStates:
          Completed:
            # Directly after "Completed" state, the submission is marked as
            # "Redelivered".
            after:
              # We need to duplicate the state, as otherwise you may observe
              # the "Review Redelivered Translation" task, to be triggered
              # directly after "Completed" and not, after we got into the
              # error handling task.
              - Completed
              - Redelivered
            # Mark the state as final, not to continue with the next
            # (standard) state.
            final: true
      ```

      **Side Note** For now, we accept the duplicate state. We may want to
      review, though, if processing (or mocking) can be optimized here.

3. Start a translation for an article.

4. Wait for the workflow to appear in _Workflow App_'s "Open" workflows

5. Double-click the workflow

   1. Field "Current Task" should be “Review Translation (redelivered)”
   2. Status should be “Redelivered”
   3. In the section "More", a download link for the broken XLIFF should be
      shown under "Issue Details" (if you skipped setting `mock.error` the
      field is not available).

6. Click the XLIFF download link

   1. Both the XLIFF and an issue details text file should be included

7. In the _Workflow App_, click “Accept Task”, "Next Step", “Finish Content
   Localization”, and "Yes, continue"

   1. The workflow should disappear from “Open”
   2. The workflow should appear in “Closed”
