---
description: Preparation of Test Setup
tags:
  - test
  - setup
---

# Setup

Prior to starting with the manual (and half-automated tests), ensure to have
set up your system accordingly.

## GlobalLink Settings

Adjust GlobalLink-Settings within settings document at
`/Settings/Options/Settings/Translation Services/GlobalLink` for quicker
feedback during manual test-steps as well as to access the relevant sandbox
via corresponding credentials:

```yaml
globalLink:
  # [... other settings]
  # key (type: String)
  # Adjust either for automatic or manual GCC processing. To ease
  # switching keys, hold them in the prepared extra Struct
  # at `additionalConfigurationOptions.exampleKeys` for an easy copy & paste:
  key: "..."
  # apiKey (type: String)
  # Only used for testing. Otherwise, expected to be set via Spring for
  # security reasons.
  apiKey: "..."
  # type (type: String)
  # Adjust to "default" for processing at GCC backend or "mock" for manual
  # processing. See also: `additionalConfigurationOptions.availableTypes`.
  type: "default"
  # isSendSubmitter (type: Boolean)
  # Adjust to `true` to send the submitter with the request.
  isSendSubmitter: false
  # Section: Timing Adjustments — Get Faster Feedback on Manual Test Steps
  # Despite `retryCommunicationErrors`, which just needs to be adjusted,
  # find copy & paste ready values for the other settings in the
  # `additionalConfigurationOptions` Struct.
  sendTranslationRequestRetryDelay: 1m
  downloadTranslationRetryDelay: 3m
  downloadTranslationEarlyRetryDelay: 1m
  cancelTranslationRetryDelay: 1m
  retryCommunicationErrors: 1
  # Section: Submission Instruction Type Settings
  submissionInstruction:
    # [...]
    # characterType (type: String)
    # Adjust to "unicode" to provoke errors in the GCC backend, as it does
    # not support SMP characters.
    characterType: "unicode"
  submissionName:
    # [...]
    # characterType (type: String)
    # Adjust to "unicode" to provoke errors in the GCC backend, as it does
    # not support SMP characters.
    characterType: "unicode"
  # Section: Mock Settings
  # Referenced below, like to provoke errors are certain states.
  mock:
    # [...]
```

## Test Convenience in CoreMedia Studio

To ease following the test-steps it is recommended to prepare as follows:

* **Settings as Favorite**:

  It is recommended to add the settings document
  `/Settings/Options/Settings/Translation Services/GlobalLink` to the
  "Favorites" in Studio. It is required to be adapted multiple times during
  the manual test steps.

* **Start Workflow Options**:

  There are various ways to start localization workflows. Choose one of them
  during your test-steps and possibly switch to others for further steps. Some
  available options are:

  * Drag an article into the "Localization Workflows" drop area in Control
    Room.
  * Choose "Start Translation Workflow" within the Locale Switcher of an
    opened document.

* **Workflow Interaction Options**:

  Just as for when starting the workflow, you have multiple ways to interact
  with the workflow. Choose one of them during your test-steps and possibly
  switch to others for further steps. Some available options are:

  * **In Localization Nagbar**: To directly switch from a document to its
    assigned localization workflow, click on one of the available options in
    the nagbar, which is either clicking the link, to jump to the
    _Workflow App_ or click on the button, to open the workflow within a
    sidebar.

  * **Control Room**: Provides an overview of running workflows and allows
    you to interact with them.

  * **Workflow App**: Provides a more detailed view of the workflow.

  * **Workflow Sidebar**: Opened within the Content App, providing an
    overview of the workflow.

* **Studio Localization**:

  It is good practice, to switch the language of the Studio UI from time to
  time, to ensure also German labels are available.
