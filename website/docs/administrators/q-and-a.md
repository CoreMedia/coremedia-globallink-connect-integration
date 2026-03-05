---
sidebar_position: 5
description: Answers to some more frequently asked questions.
---

# Questions & Answers

## What to do when deriving a new site?

**Short:** _Enable Target Language at GCC, Configure Language Mapping_

When you derive a new site and want to propagate translations to this site
via GlobalLink Translation Workflow, you need to ensure that your target locale
is supported by GlobalLink and that (if required) GlobalLink knows how to
represent the CMS locale tag (represented as IETF BCP 47 language tag) within
GlobalLink Project Director.

You can validate the configuration by retrieving the Connectors Config via
REST. It will contain a section `supported_locales` where `pd_locale` maps
to the locale representation within Project Director and `connector_locale`
should be equal to your derived site locale as IETF BCP 47 language tag.

You will find the language tags in `/Settings/Options/Settings/LocaleSettings`
in your CMS.

## Why does my workflow unexpectedly state "Status: Completed"?

Sometimes, you may find your workflow showing _"Status: Completed"_ while the
content is not translated, and the workflow is still in the list of running
workflows.

**Short:** _Ask GlobalLink to check task states and to complete all tasks
of the submission._

In GlobalLink the state can be handled separately for the submission, and the
actual translation tasks. A submission can be accidentally marked as completed
by the translator while the actual tasks might not be completed yet.

The workflow in CoreMedia Studio requires the tasks to be completed, so that it
can download the translated content of a single task. Other tasks might still be
in translation. Only if all tasks in GlobalLink are completed the workflow in
CoreMedia Studio can finish.

This only happened rarely in the past, but if you are wondering why a workflow
seems to hang forever, then ask the GlobalLink support to check the states of
the individual tasks of the submission and complete them if possible.

As a system administrator you can enable DEBUG logging for
`com.coremedia.labs.translation.gcc` in the workflow-server application.
You should find an entry like `Checked for update of submission 1669718090545
(PD ID [45360]) in state COMPLETED with completed locales [[]].` which indicates
that tasks at GlobalLink that are represented by `completed locales`
have not been completed properly.
