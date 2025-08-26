---
sidebar_position: 3
description: Insights to the design of the CoreMedia GlobalLink Connector.
---

# Design Details

## Translation Types

In CoreMedia CMS there exist two translation types:

1. Translation to derived sites, and
2. Translation to preferred site.

While for _Translation to derived sites_ the site-managers of the master site
send localization/translation items to the derived sites, the local
site-managers of each derived site may as well trigger translation from master
site to their derived site (assumed to be set as preferred site).

This implementation is designed to support _Translation to derived sites_ and
instead of local site-managers accepting the translation, it is designed, so
that the site-manager of the master site will also take care of accepting the
translation results.

## CMS Workflow to GCC Workflow

GCC uses specific terms for the structure of their translation workflow. The
terms are important to understand, especially how they map to the CoreMedia CMS
translation workflow:

* **Submission:**

  The CMS translation workflow creates and starts a submission when handing
  over the contents to be translated to GCC. A submission has one source
  locale and consists of several jobs.

* **Job:**

  One job is bound to one target locale. It may consist of several tasks. In
  this implementation jobs are not really visible. See _Task_ documentation
  below.

* **Task:**

  One task is bound to one file to translate. As the CoreMedia CMS translation
  workflow creates one XLIFF document per target site/target locale, all jobs
  of this implementation only contain one task.

### Workflow Stages

A rough sketch of the CoreMedia CMS translation workflow shows how the GCC
translation workflow is embedded into the CMS workflow (here: standard
processing):

1. **Preprocessing Phase:** In this phase, the target contents are prepared to
   receive the translation results later on. Missing contents are created,
   links are adjusted, some properties automatically merged (like linklists
   for example).

2. **Translation Phase:** Contents are handed over as XLIFF documents to GCC.
   The state is regularly polled. XLIFF documents from completed tasks are
   automatically downloaded and applied. Changes are applied as
   translation-workflow-robot user.

3. **Postprocessing Phase:** Once the submission is completed, the CMS workflow
   switches to post-processing phase. Editors have the change to review the
   translation and eventually accept the translation. As soon as they accept
   the translation, the last step is to update the master version number in
   the target contents, to signal from which master version they received
   the updates.

## Cancellation

GCC offers cancellation at the task and the submission level. Note, that the
CoreMedia CMS translation workflow does not support cancellation at task level.

The reason can be found in the _Workflow Stages_ mentioned above. When a
cancellation is detected, target contents may have received some changes
already, and cancellation requires to revert all those changes. As there is no
partial revert of some contents, all contents which are part of the CMS
translation workflow need to be reverted.

Thus, as the existing CMS API does not support partial cancellation, the same
applies to the GCC submission which must not be partially cancelled.

The current implementation is aware of partial cancellation, though: If only
some tasks are cancelled, the implementation will stop downloading results from
these tasks and wait for the whole submission to be marked as cancelled. Such
wait loops are logged.

_Planned/Later:_ If you perform cancellation within the CMS workflow, it is
always ensured, that the complete submission is cancelled.

## One Workflow for all Locales vs. One Workflow per Locale

Per default the GCC extension will create one workflow instance for all locales,
that were chosen in the _StartWorkflowWindow_. Each locale results into a
separate Job, which are all bundled under one submission, that is tracked by one
workflow.

This means that the workflow is only marked as completed, when all locales
(Jobs) have been marked as completed.

The GCC extension can also be configured, to start one workflow instance per
locale. This means that one submission, holding only one job is created per
chosen locale.

This can be achieved by setting the value _createWorkflowPerTargetSite_ in the
`GccStudioPlugin` to _true_ (this is actually the default, therefore you can
also completely remove this configuration). Furthermore, you need to change the
type of the workflow variable _targetSiteId_ in the workflow definition
_translation-global-link.xml_ to _String_.

## Not supported: Reopening

Reopening already delivered submissions is not supported by this implementation.
Instead, please start a new translation workflow for contents where you want to
get the translation result adjusted.

Implementing reopening from already delivered submissions would require to cope
with challenges like the following:

* **Polling:** The implementation uses polling the translation state, while a
    CoreMedia workflow is active. Polling ends as soon as the CoreMedia workflow
    is done. In order to respond to reopening submissions at GCC you either need
    to keep polling even after the workflow is done, or you need to change the
    implementation to use push notifications from GCC backend instead. Push
    notifications is not part of this implementation as it would require to
    expose an additional service of the CoreMedia CMS backend.

* **Updated Contents/Resolving Conflicts:** As reopening may occur after several
    days or even months, it is most likely that your target contents got updated
    meanwhile. Trying to re-import new translation results may cause hard to
    resolve conflicts, because of for example missing linked documents in
    CoreMedia RichText.

**Redelivered Submissions:** Slightly different to that, so-called redelivered
submissions are supported. Redelivered submissions are submissions that were
completed already, but have not been marked as delivered (thus, successful
download and import signalled by this API), for example, because the resulting
XLIFF is invalid. In these cases an extra twist is supported, where the
translators instead mark the submission as "Redelivered" and subsequently send
the XLIFF by different means (like email).
