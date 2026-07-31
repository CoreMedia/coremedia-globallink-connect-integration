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
Plane (SMP) in previous GCC REST backend deployments (not reproducible anymore).

* **Connector Type**: `mock`
* **Key Type**: _irrelevant_

## Quick Steps

1. Log in as Rick C.
2. Use `mock` type.
3. Set `mock.scenario` to `submission-error`.
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
   2. `mock.scenario` is set to `submission-error`

3. Start a translation for an article.

4. Open the workflow in _Workflow App_ via nagbar shown in the article.

5. Wait until the current task is "Download error" and offered to you.

6. Accept the task.

7. Click the error issue information and see a "General submission failure."
