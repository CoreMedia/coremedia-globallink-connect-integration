---
description: "Scenario: Wrong Or Invalid Connector Key Recovery."
tags:
  - test
  - scenario
  - manual
  - error-handling
---

# Connector Key Recovery

This test is about a robustness layer introduced with 2406.1 approval: If during
a running translation workflow you change the connector key (stored as
`globalLink.key`) to another (but valid connector key), the workflow should
be able to recover from that scenario ones the key is valid again. Also, if the
key is invalid, the workflow should be able to recover from that scenario.

* **Connector Type**: `default`
* **Key Type**: `manual` (initially)

## Quick Steps

1. Log in as Rick C.
2. Use `default` type, and the "manual" connector key.
3. Start a translation of an article and wait for the submission ID to be
   retrieved.
4. Change the connector key to "automatic".
5. See that error-handling-task is reached with an issue about an unknown
   submission.
6. Change the connector key to an invalid key.
7. Trigger retry.
8. See that error-handling-task is reached with an issue about an invalid
   connector key.
9. Set the connector key back to "manual".
10. Trigger retry.
11. The workflow should continue polling the translation state without issues.

## Detailed Steps

1. Log in as Rick C.

2. Open the GlobalLink
   settings `/Settings/Options/Settings/Translation Services/GlobalLink`

   1. `type` is set to `default`
   2. Credentials for gcc are entered (**manual** workflow key)

3. Start a translation for a given article.

4. **To Workflow App:** Navigate, for example via the nagbar, to the
   _Workflow App_ and wait until the workflow is in the "Translate" state
   (Current task: "Awaiting translation results").

5. **Content App:** Change the connector key to another valid key (thus,
   "automatic").

6. **Workflow App:** Wait until the current task changes to "Download Error".

7. Accept the task.

8. **Content App:** Change the connector key to an invalid key, for example,
   prefix it with an additional `0`.

9. **Workflow App:** Click "Next Step". Validate an error is shown about an
   unknown submission, triggered by our previous misconfiguration.

10. Choose "Continue and retry".

11. Wait until the current task changes to "Download Error" again.

12. Accept the task.

13. **Content App:** Change the connector key back to our initial, valid key
    (thus, "manual").

14. **Workflow App:** Click "Next Step". Validate an error is shown about an
   invalid connector key, triggered by our previous misconfiguration.

15. Choose "Continue and retry".

16. The workflow should continue polling the translation state without issues.
