---
id: screenshots
sidebar_label: Screenshots
description: How to Update and Add Screenshots.
---

# Updating and Adding Screenshots

This document is a recommendation how to update or add screenshots to the
documentation. It is not a strict guideline, but a suggestion how to keep the
documentation consistent and easy to maintain.

First, there will be some general recommendation, while next, you will see
recommended steps to reach a given UI state per corresponding screenshot.

## General Recommendations

### Common Screenshot Size

**TL;DR**: Use device terminal emulation with viewport size 1280x800.

To best align the sizes of the screenshots, it is recommended to use the
browser's developer tools to set the viewport size to a common value. This
ensures that the screenshots have a similar size and are easier to compare.

The following steps are recommended to create a screenshot:

1. Open the page in the browser.
2. Open the development tools (F12).
3. Toggle the device terminal emulation (Ctrl+Shift+M).
4. Set the viewport size to a common value (e.g., 1280x800).

To take the screenshot, you may also use the development tools of the browser:

1. Open the development tools (F12).
2. Select the `<body>` element.
3. Right-click the element and select "Capture node screenshot".

For dynamic elements it may be easier just to use tools such as
[Greenshot](https://getgreenshot.org/), for example.

### GlobalLink Configuration

_Translation Settings_ to be adapted for screenshot scenarios to be found at

* `/Settings/Options/Settings/Translation Services/GlobalLink`

It is recommended to add this to your bookmarks while working on the
screenshots.

All the steps below assume, that you have set the GlobalLink connection type
to `mock`. This is recommended as some relevant states cannot be reached in
the GlobalLink backend, or are hard to reach.

Also, you should align the delays and retry counts to the following values
(excerpt of the Struct property):

```yaml
globalLink:
  # String
  type: mock
  # Boolean; changed within manual test steps
  isSendSubmitter: false
  # Integer
  sendTranslationRequestRetryDelay: 60
  # Integer
  downloadTranslationRetryDelay: 60
  # Integer
  cancelTranslationRetryDelay: 60
  # Integer; change existing value
  retryCommunicationErrors: 1
  # Mock settings to possibly adapt.
  mock:
    # ...
```

### User

Unless specified otherwise, use the user `Rick C` to log in to CoreMedia Studio.

### Sites

* **Preferred Site**: Chef Corp. (`en-US`) (should be the default site for Rick C)
* **Target Site**: Chef Corp. (`de-DE`)

The description assumes, that you have adapted the `de-DE` site by adding:

* **Site Manager Groups**: For `de-DE`, add `manager-c-en-US`

This allows taking screenshots without the need to switch users/sessions.

## Screenshot Advice

**Sorting**: Screenshot advices should be sorted by the file name of the
screenshot they are referring to.

### gcc-connect-error.png

A screenshot representing an upload communication error in the GlobalLink
connector.

1. **Translation Settings:** Set `mock.error` to `upload_communication`.
2. **Content:** Create a new article (name irrelevant) and make it valid.
3. **Workflow:**
   * Start a translation workflow for the article to `de-DE`.
   * As workflow name choose _Press Release With GlobalLink_.
4. **Workflow App:** From the "In localization" badge, for example, open the
   workflow within the Workflow App.
5. **Accept User Task:** As soon as available, accept the user task named
   _Upload error_.
6. **Next Step:** Click the _Next Step_ button to show the corresponding
   dialog, that will also contain the error message.

**Cleanup**: Consider selecting _Rollback_ to end the workflow.

### gcc-redelivered.png

A screenshot representing a redelivered submission state in the GlobalLink
connector.

1. **Translation Settings:**
   * Set `mock.error` to `download_xliff`.
   * Mock redelivered state by adjusting the subsequent state after the
     "Completed" state:

     ```yaml
     globalLink:
       # String
       type: mock
       mock:
         error: download_xliff
         submissionStates:
           Completed:
             after: Redelivered
             final: true
     ```

2. **Content:** Create a new article (name irrelevant) and make it valid.
3. **Workflow:**
   * Start a translation workflow for the article to `de-DE`.
   * As workflow name choose _Press Release With GlobalLink_.
4. **Workflow App:** From the "In localization" badge, for example, open the
   workflow within the Workflow App.
5. **Accept User Task:** As soon as available, accept the user task named
   _Review translation (Redelivered)_.


### gcc-running.png

A screenshot representing the running workflow when opened in the Workflow App.

1. **Translation Settings:** Ensure, that no provoked errors are adapted
   submission state adaptations are configured.
2. **Content:**
   * Create a new article named _Press Release_.
   * Add the content to the Chef Corp. Homepage.
3. **Workflow:**
   * Start a translation workflow for the homepage to `de-DE`.
   * As workflow name choose _Press Release With GlobalLink_.
4. **Workflow App:** From the "In localization" badge, for example, open the
   workflow within the Workflow App.
5. **Status:** To have an alike screenshot over versions, wait for the field
   _Status_ to switch to "Delivered".
6. **Contents:** Expand the _Contents_ section to show the content to be
   translated.

**Cleanup**: Consider accepting the user task and selecting _Rollback_ to end
the workflow.

### gcc-select-type.png

A screenshot that shows the Start Translation Workflow Window while selecting
the localization workflow type.

**Screenshot Tool Recommended**: As we are about to screenshot an element
(the dropdown) that will close on the blur event, it is recommended to use an
external screenshot tool, to trigger the screenshot via a hotkey.

1. **Translation Settings:** Ensure, that no provoked errors are adapted
   submission state adaptations are configured.
2. **Content:** (Hint: Same as for `gcc-running.png`, so you may reuse it.)
   * Create a new article named _Press Release_.
   * Add the content to the Chef Corp. Homepage.
3. **Content App:**
   * Minimize the preview before taking the screenshot. The currently opened
     document will appear as background to the start workflow window.
   * Close all tabs despite the _Press Release_ (background tab) and the
     homepage (active tab).
4. **Workflow:**
   * Trigger start of a translation workflow for the homepage to `de-DE`.
   * As workflow name choose _Press Release With GlobalLink_.
   * Open the _Workflow Type_ selection combo.

### gcc-settings.png

A screenshot that gives an impression on the `GlobalLink` settings document
and its location.

1. **Translation Settings**: We need a little trick not to break our other
   screenshot scenarios, but also only provide a minimal set of settings
   relevant for in-production use. Just create a copy of `GlobalLink` settings
   document in the home-folder of Rick C, for example. Remove all settings
   despite:
   * `url`
   * `key`
2. **Content App**:
   * Open your copied settings document.
   * Close the preview.
   * Open the library view by clicking on the content path in the _System_ tab
     of the original `GlobalLink` settings document.
   * Dock the library to the right side.
   * Move the splitters around, so that you get the best view on relevant
     parts of the settings document as well as in the library. Also, consider
     hiding irrelevant columns in the library.
   * Close all other tabs but that for the "mocked" settings document.

### gcc-start-wf.png

A screenshot that provides a hint on how to start a translation workflow.

**UX Hint**: Depending on updates in context of the workflow UI, new concepts
for starting translation workflows may be introduced and preferred as
"ease of use" pattern. Please consider this when updating the screenshot.

**Screenshot Tool Recommended**: As we are about to screenshot an element
(the dropdown) that will close on the blur event, it is recommended to use an
external screenshot tool, to trigger the screenshot via a hotkey. If possible,
adjust your screenshot tool to also capture the mouse cursor.

1. **Content:** (Hint: Same as for `gcc-running.png`, so you may reuse it.)
   * Create a new article named _Press Release_.
   * Add the content to the Chef Corp. Homepage.
2. **Content App:**
   * Minimize the preview before taking the screenshot. The currently opened
     document will appear as background to the start workflow window.
   * Close all tabs despite the _Press Release_ (background tab) and the
     homepage (active tab).
3. **Start Workflow Trigger**: Open the _Locale Switcher_. If your screenshot
   tool supports capturing the mouse cursor hover it above "Start localization".

### gcc-success.png

A screenshot that shows the task "Review Translation" in the Content App along
with the differencing view.

1. **Translation Settings:** Ensure, that no provoked errors are adapted
   submission state adaptations are configured.
2. **Content:**
   * Create a new article named _News_.
   * As "Article Text" (`en-US`) use (as a suggestion; generated by GitHub
     Copilot):

     > We are excited to announce the release of the new CoreMedia GlobalLink
     > connector! This update brings a host of shiny new features designed to
     > enhance your translation workflow, including improved integration with
     > GlobalLink, faster processing times, and a more intuitive user interface.
     > Upgrade now to experience the best in content localization and
     > management.

    * Process a first translation workflow for the article to `de-DE`.
    * Update the content to mark "CoreMedia GlobalLink connector" as bold.
3. **Workflow:**
   * Start a translation workflow for "News" article to `de-DE`.
   * As workflow name choose _Update News_.
4. **Content App:**
   * Close all other tabs despite the "News" article.
   * Open the workflow in the sidebar (for example, click on "Show Workflow"
     button within the "In Localization" badge)
   * Accept the user task.
   * Double-click on the "News" article in the workflow's content set to open
     the differencing view.
   * Override the pseudo-translated result with this suggested translation:

     > Wir freuen uns, die Veröffentlichung des neuen **CoreMedia GlobalLink
     > Connectors** bekannt geben zu können! Dieses Update bietet eine Vielzahl
     > neuer Funktionen, die Ihren Übersetzungs-Workflow verbessern, darunter
     > eine verbesserte Integration mit GlobalLink, schnellere
     > Verarbeitungszeiten und eine intuitivere Benutzeroberfläche. Holen Sie
     > sich jetzt das Upgrade und erleben Sie das Beste in Sachen
     > Inhaltslokalisierung und -verwaltung.

   * Change the pseudo-translated title just to "News".
   * Check-in the modified content.
   * Move the separators around to have a good view on the overall scenario.

**Cleanup**: Consider accepting the user task and selecting _Rollback_ to end
the workflow.
