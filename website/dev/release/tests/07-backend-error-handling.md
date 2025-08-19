---
description: "Scenario: GCC Backend Error Handling (Submission Error State)."
tags:
  - test
  - scenario
  - manual
  - error-handling
---

# Backend Error Handling

This test is about submissions within the GCC backend, which are in an error
state. This has been observed, for example, when trying to send a submission
with instructions containing characters from the Supplementary Multilingual
Plane (SMP).

**Alternative Scenario:**
The given manual test steps "mock" this state, as we cannot ensure that an
error persists over time. If you want to test the real-life behavior, you
may choose using the "default" rather than the "mock" type and instead of
using `mock.error`, you may use the `submissionInstruction.characterType`
setting. Set it to `unicode` and add some SMP characters to the instructions
(thus, workflow notes), like for example, the dove emoji: 🕊.

**Advantage**: The advantage of the real-world scenario is, that you may also
test, that a submission in an error state may still be canceled.

* **Connector Type**: `mock`
* **Key Type**: _irrelevant_

## Quick Steps

1. Log in as Rick C.
2. Use `mock` type.
3. Set `mock.error` to `SUBMISSION_ERROR`. For a quick path, you may skip
   the extra submission state mocking as sketched in the detailed steps.
4. Start a translation of an article.
5. Open the started workflow process.
6. Expect a user-task "Download error" and accept it.
7. Validate that a localized error message like "General submission failure"
   is shown.

## Detailed Steps

1. Log in as Rick C.

2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`

   1. `type` is set to `mock`
   2. `mock.error` is set to `SUBMISSION_ERROR`
   3. **Submission State Mocking**: Add (or activate) the following mock
      settings (optional step, but recommended to test closer to the real-world
      scenario):

      ```yaml
      mock:
        # [...]
        submissionStates:
          Started:
            override: Pre-Process
            # Mark as final to ease mocking here. Prevents further states,
            # though, which is, that you also cannot cancel the submission
            # in this mocked scenario.
            final: true
      ```

3. Start a translation for an article.

4. Open the workflow in _Workflow App_ via nagbar shown in the article.

5. Wait until the current task is "Download error" and offered to you.

6. Accept the task.

7. Click the error issue information and see a "General submission failure."

In the real-world scenario, you may now continue like to "Abort and rollback
changes" as most likely, the submission cannot be repaired. This is not possible
in this simple mocked scenario, though.
