---
sidebar_position: 3
title: Send Content
description: Send Content to GlobalLink.
---

# Send Content to GlobalLink

Once finished working on the campaigns content, open the Control Room and click
the _Start a localization workflow_ button in the toolbar of the
_Localization workflows_.

![GCC Start Workflow](./img/gcc-start-wf.png)

In the _Start Localization Workflow_ window, select the workflow type
_Translation with GlobalLink_, set a self-describing name, a due date, drop the
to-be-translated content, and set the target locales.
The notes are sent as instructions for the translators.

![GCC Select](./img/gcc-select-type.png)

After having started the workflow, it is shown in the pending workflow section. 
Double-click the workflow to open it in the detail view in the Workflow app.
Here you will see the submission state, the id and other information once
available.

![GCC Running](./img/gcc-running.png)

In case an error occurs, the workflow re-appears in your inbox, and you can 
select to cancel the workflow, or you can try to fix the problem and retry.

If there was an issue with the XLIFF retrieved from the GCC backend (such as
being corrupted or invalid), you will have the option to download the XLIFF
as well as details about the issues (in a ZIP file).

![GCC Error Handling](./img/gcc-connect-error.png)

After the translation is finished, you will receive a notification. The workflow
is shown in the inbox and once accepting the task, you can review the content in
the content app and finish it.

![GCC Success](./img/gcc-success.png)

Well done.
