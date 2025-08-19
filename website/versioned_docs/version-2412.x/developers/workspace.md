---
sidebar_position: 4
description: Overview of the structure of this workspace.
---

# Workspace Structure

## workflow-server

Manages the `workflow-server` extension and the `gcc-restclient-facade`.

### gcc-restclient-facade*

Facades to GCC Java REST Client API. Please, see the corresponding
_Facade Documentation_ in the workspace for details.

### gcc-workflow-server

The workflow definition and classes to send and receive translation via GCC REST
API.

## studio-client

Extension for the Studio client that registers the workflow definition and
configures the UI.

## studio-server

Extension for the Studio REST server that registers the workflow definition.

## user-changes

Extension for the User Changes web application to enable Studio notifications
for the GlobalLink workflow.

## test-data

Contains a (quite empty) settings document which, after content import, needs to
be linked to your site root documents (also known as Homepages).
