---
sidebar_position: 3
description: Overview of build profiles.
---

# Profiles

The CoreMedia GlobalLink Connector workspace comes with different Maven build
profiles that are relevant for your development processes. Find an overview of
the profiles below.

## create-test-data

* **Profile name:** `create-test-data`
* **Activation:** Active by default, suppress with `-DskipContent`
* **Description:** Creates a settings document that is required to run the GCC
  extension in your local Blueprint setup.

## gcc-strict-checks

* **Profile name:** `gcc-strict-checks`
* **Activation:** Activate with `-Pgcc-strict-checks`
* **Description:** Enables additional code quality checks like
  [Error Prone](https://errorprone.info/) during the build. Also, makes the
  build fail on warnings and reports deprecated API usages.

  For concise nullability checks (based on
  [JSpecify annotations](https://jspecify.dev/)), it is recommended to activate
  this profile.
