---
description: "Scenario: Contract Test"
tags:
  - test
  - scenario
  - half-automated
---

# Contract Test

Especially on updates of `gcc-restclient` you should run the half-automatic
test `DefaultGCExchangeFacadeContractTest`. Read the JavaDoc to know how to
configure and run the test.

Lately, when using the automatic workflow, the submission is not marked as
`DELIVERED` anymore. This is why the `translateXliff` test fails. It seems to
work with the manual workflow though.

## Set Up

To run the test you need to create a file with the name
[.gcc.properties](./files/example.gcc.properties.txt)
in your user home folder

:::info INFO: Profile Support
Some tests may require adapted properties. They define a so-called profile.
If the profile is available, selected configuration may be overridden in
a file `.gcc.<profile>.properties`.

Currently supported profile:

* `cancellation`: You may need to set a "manual transition" connector here
  for the test to work.
:::

## Manual Review

Please review the following aspects **in the management dashboard** of
GlobalLink manually (all submissions created by this test should have a name
starting with `CT` and a timestamp).

### Instructions

The test `shouldRespectInstructions` should have created submissions with
so-called "submission instructions" (in the workflow: "Notes") for these
scenarios (by ID):

* `BMP` (refers to Base Multilingual Plane)

  It is expected that each described character is represented visually as
  described within the test fixture. Thus, arrows should be visible as arrows
  and even high level Unicode character from BMP should be displayed correctly
  (e.g., the "Fullwidth Exclamation Mark": `！`).

* `FORMAT` (refers to newlines and tabs)

  As instructions in the GCC backend are expected to be HTML, the newlines
  should be replaced by `<br>` and tabs by `&nbsp;&nbsp;`. In other words:
  You should see visible line-breaks and indents.
  
* `HTML_AS_TEXT`

  Given the assumption, that in the Workflow App the instructions are given in
  plain-text, the HTML should be escaped. Thus, you should see no bold text,
  no newline triggered by a textual `<br>`, and also entities like `&amp;`
  should be visible as plain-text.

* `UNICODE_SMP` (`SMP` refers to the Supplementary Multilingual Plane)

  For now, the GCC backend is expected to not support SMP characters. Thus,
  corresponding characters like emojis (dove, for example: 🕊) should be
  replaced by a placeholder pattern. Current implementation is, to make them
  distinguishable, that the placeholder is the Unicode code point in hex, such
  as `U+1F54A`.

### Submitter

The test `shouldRespectSubmitter` should have created submissions with
overridden submitter names (thus, must be different to the account username).
Three submissions should be visible:

* `YES`: The submitter name should be the test's name.
* `NO`: The submitter name should be the account username.
* `DEFAULT`: The submitter name should be the account username.
