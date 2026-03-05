---
sidebar_position: 5
title: Pitfalls
description: How to deal with them or prevent them.
---

# Pitfalls

## Publishing content of active submissions

Once a language is completely translated at GlobalLink, it is downloaded and
applied to the content for editorial review in the Studio. While it is fine to
review this content and even publish it, especially publishing content can cause
issues later on. When publishing content the content server can destroy previous
versions of this content, that is otherwise required to be able to roll back the
changes, if triggered with "Abort and rollback changes". So, you should make
sure that a submission does not have to be aborted when publishing content of an
active workflow.

## Canceling submissions

Additionally to _Abort and destroy workflows_ the workflows of type
_Translation with GlobalLink_ can be canceled so that also GlobalLink gets
notified about the cancellation. In rare cases, when for example the submission
was at the same time completed in GlobalLink, then you will see the workflow
with a symbol for being canceled in the _Closed_ list of the Workflow App, but
the detail view will state that the status is completed. In this case, the
translation was completed at GlobalLink, but the translated texts were not
applied because of the cancellation.
