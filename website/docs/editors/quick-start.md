---
sidebar_position: 1
description: Quick start information for editors.
---

# Quick Start

Assuming that you are familiar with the CoreMedia Studio and that you created a
new campaign in the English master site that has now to be translated into
French, German, and Spanish. This guide shows how this task can be accomplished
by means of the GlobalLink Connect Cloud connector.

## Configure Connection to GlobalLink Connect Cloud

If the connection is not set up yet, go to `/Settings/Options/Settings/` create
a _Settings_ content called _GlobalLink_ and add it to the _Linked Settings_
property of the master site's homepage.

![GCC Settings](/img/gcc-settings.png)

## Send Content to GlobalLink

Once finished working on the campaigns content, open the Control Room and click
the _Start a localization workflow_ button in the toolbar of the
_Localization workflows_.

![GCC Start Workflow](/img/gcc-start-wf.png)

In the _Start Localization Workflow_ window, select the workflow type
_Translation with GlobalLink_, set a self-describing name, a due date, drop the
to-be-translated content, and set the target locales.
The notes are sent as instructions for the translators.

![GCC Select](/img/gcc-select-type.png)

After having started the workflow, it is shown in the pending workflow section. 
Double-click the workflow to open it in the detail view in the Workflow app.
Here you will see the submission state, the id and other information once
available.

![GCC Running](/img/gcc-running.png)

In case an error occurs, the workflow re-appears in your inbox, and you can 
select to cancel the workflow, or you can try to fix the problem and retry.

If there was an issue with the XLIFF retrieved from the GCC backend (such as
being corrupted or invalid), you will have the option to download the XLIFF
as well as details about the issues (in a ZIP file).

![GCC Error Handling](/img/gcc-connect-error.png)

After the translation is finished, you will receive a notification. The workflow
is shown in the inbox and once accepting the task, you can review the content in
the content app and finish it. 

![GCC Success](/img/gcc-success.png)

Well done.

## Further Scenarios

### Manual XLIFF Override &amp; Redelivered Submission State

Translators at GlobalLink may decide to mark a submission as redelivered. It is
expected, that in these cases, the XLIFF is sent via different means to the
editorial team, like, for example, email. The editorial team can then manually
upload the XLIFF to the submission in the Workflow app.

This redelivered state is represented as an extra task "Review Translation
(redelivered)" (with status: "Redelivered"). If, as expected, the original
XLIFF provided by the GCC backend had issues, you can download the (corrupted)
XLIFF along with its issue details just as for the error handling.

![GCC Redelivered](/img/gcc-redelivered.png)

## Pitfalls

**Publishing content of active submissions**: Once a language is completely 
translated at GlobalLink, it is downloaded and applied to the content for 
editorial review in the Studio. While it is fine to review this content and even 
publish it, especially publishing content can cause issues later on. When 
publishing content the content server can destroy previous versions of this 
content, that is otherwise required to be able to roll back the changes, if 
triggered with "Abort and rollback changes". So, you should make sure that
a submission does not have to be aborted when publishing content of an active
workflow.

**Canceling submissions**: Additionally to _Abort and destroy workflows_ the 
workflows of type _Translation with GlobalLink_ can be canceled so that also
GlobalLink gets notified about the cancelation. In rare cases, when for example
the submission was at the same time completed in GlobalLink, then you will see 
the workflow with a symbol for being canceled in the _Closed_ list of the 
Workflow App, but the detail view will state that the status is completed. 
In this case, the translation was completed at GlobalLink, but the translated
texts were not applied because of the cancelation.
