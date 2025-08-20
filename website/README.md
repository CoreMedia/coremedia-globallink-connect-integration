# Website

This website is built using [Docusaurus](https://docusaurus.io/), a modern
static website generator.

## Installation

```bash
pnpm install
```

## Local Development

```bash
pnpm start
```

This command starts a local development server and opens up a browser window.
Most changes are reflected live without having to restart the server.

If you observe some caching issues, you may want to call this before start:

```bash
pnpm docusaurus clear
```

The site will be reachable here:

* <http://localhost:3000/coremedia-globallink-connect-integration/>

For debugging, you may want to open:

* <http://localhost:3000/coremedia-globallink-connect-integration/__docusaurus/debug>

## Create new Version

To create a new versioned documentation branch (thus, a new versioned folder
within the website), run, for example:

```bash
pnpm version-snapshot 2412.x
```

See details in "Contributors" section of the website.

## Covered by GitHub Action

The following steps are covered by a GitHub action. You may still want to
trigger them manually for testing purpose, for example.

### Build

```bash
pnpm build
```

This command generates static content into the `build` directory and can be
served using any static contents hosting service.

### Deployment

```bash
GIT_USER=<Your GitHub username> pnpm deploy
```

This will push the website to the `gh-pages` branch.

## Structure

The documentation is separated into two aspects: a _versioned_ `docs/` folder
and a _not versioned_ `dev/` folder. The primary site is shipped from the
`docs/` folder, while you will find the unversioned part at _"Contributors"_
from the header navigation.
