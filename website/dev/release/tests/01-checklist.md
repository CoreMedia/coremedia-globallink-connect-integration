---
description: Your Test Checklist.
tags:
  - test
  - checklist
---

# Checklist

The following table can be copied, to track your test results. Copy it to a
suitable application (like your approval task issue, for example) and fill
it with your results.

## All Scenarios

| Scenario                     | Connector Type | Key Type     | Status | Notes |
|------------------------------|----------------|--------------|--------|-------|
| Contract Test                | `default`      | `automatic`  |        |       |
| Happy Path                   | `default`      | `automatic`  |        |       |
| General Error Handling       | `default`      | `automatic`  |        |       |
| Cancelation (Studio)         | `default`      | `manual`     |        |       |
| Cancelation (GlobalLink)     | `default`      | `manual`     |        |       |
| Cancelation (Error Handling) | `default`      | `manual`     |        |       |
| Wrong/Invalid Connector Key  | `default`      | `manual`     |        |       |
| GCC Backend Error Handling   | `mock`         | _irrelevant_ |        |       |
| XLIFF Import Error Handling  | `mock`         | _irrelevant_ |        |       |
| Redelivered State Handling   | `mock`         | _irrelevant_ |        |       |

## Contract Test Results

| Test Case                                    | Status | Notes |
|----------------------------------------------|--------|-------|
| `shouldRespectInstructions` (`BMP`)          |        |       |
| `shouldRespectInstructions` (`FORMAT`)       |        |       |
| `shouldRespectInstructions` (`HTML_AS_TEXT`) |        |       |
| `shouldRespectInstructions` (`UNICODE_SMP`)  |        |       |
| `shouldRespectSubmitter` (`YES`)             |        |       |
| `shouldRespectSubmitter` (`NO`)              |        |       |
| `shouldRespectSubmitter` (`DEFAULT`)         |        |       |
