# Website Dependency Maintenance

The website dependencies maintain themselves: a GitHub workflow updates
Docusaurus, fixes vulnerabilities, cleans up overrides, and tests that the
website still works. People only review pull requests, and decide in the rare
case that a fix breaks the website.

## What to Do When…

* **A pull request "website dependency maintenance" appears:**
  Read the report in the description, then merge. Build and smoke test have
  already passed.
* **A security alert (CVE) needs a fix now:**
  Run the [Website Dependencies](#run-the-workflow) workflow with action
  `maintain`.
* **The workflow failed with "Action required":**
  Open the run's summary and follow the instructions listed there.
* **The summary says a fix breaks the website:**
  Run the workflow with action `block-advisory`
  (see [Block an Advisory](#block-an-advisory)).
* **A Dependabot pull request fails:**
  Close it if the maintenance pull request covers the same update; otherwise
  investigate the failing check.

### Run the Workflow

1. Open **Actions** → **Website Dependencies** → **Run workflow**.
2. Choose an action:
   * `maintain` (default): update and fix everything possible. Check
     _retry all blocked advisories_ to retry blocks without waiting for the
     next Docusaurus update.
   * `block-advisory`: accept an advisory (see below).
3. Wait for the run. It opens or updates a pull request with a report.

### Block an Advisory

Block an advisory only when its fix breaks the website, for example when the
maintenance report lists it under "Action Required":

1. Run the workflow with action `block-advisory`.
2. Enter the advisory ID, for example `GHSA-h67p-54hq-rp68`, and a rationale.
3. Review and merge the pull request.

A block is temporary. Each maintenance run removes blocks that no longer
apply, and retries the fix after each Docusaurus update or when a new fixed
version is published.

## How It Works

The workflow `.github/workflows/website-dependencies.yml` runs every Monday,
after changes to `package.json` or `pnpm-workspace.yaml` on `main` (for
example, merged Dependabot pull requests), and on demand. The action
`maintain` runs `scripts/deps/maintain.mjs`:

1. **Docusaurus first:** updates all `@docusaurus/*` packages to the latest
   minor or patch release, then verifies the website. New Docusaurus releases
   often update the transitive dependencies that overrides patch. Major
   releases require a manual migration and are only reported.
2. **Review blocks:** removes blocked advisories that no longer apply.
3. **Rebuild overrides:** removes all overrides, then lets pnpm fix the
   vulnerabilities again:
   * `pnpm audit --fix update` updates vulnerable packages within the version
     ranges their parents allow; this changes only the lockfile.
   * `pnpm audit --fix override` adds an override only where a parent does
     not allow a fixed version. Each new override gets a comment with the
     severity and advisory link.

   This keeps the smallest set of overrides; obsolete overrides disappear
   automatically. If the result breaks the website, the run falls back to
   adding fixes to the existing overrides.
4. **Verify:** every change is checked by `pnpm install`, `pnpm run build`,
   and `pnpm run smoke-test`. Changes that fail are reverted and reported.
5. **Report:** the pull request description lists the Docusaurus status,
   fixed and remaining vulnerabilities, removed and added overrides, and
   blocks.

The smoke test (`scripts/deps/smoke-test.mjs`) serves `build/` on a free
port, requests every page from the sitemap and the scripts and styles of the
home page, and stops the server afterward.

### Division of Labor With Dependabot

* **Dependabot** proposes version updates of direct dependencies, grouped into
  one pull request for minor and patch updates, and one for React. Security
  updates are grouped into one pull request as well. Each pull request is
  tested by `pages-test-deploy.yml` (typecheck, build, smoke test).
* **The maintenance workflow** owns Docusaurus updates (Dependabot ignores
  `@docusaurus/*`), overrides, and blocked advisories.

## Policy File

`dependency-policy.json` stores decisions made by people. The scripts update
it; edit it manually only for `keepOverrides`.

* **`blockedAdvisories`:** accepted advisories, mirrored into
  `auditConfig.ignoreGhsas` in `pnpm-workspace.yaml` and into Dependabot
  `ignore` rules. Each entry records the affected `package`, the
  `ignoreVersions` that Dependabot must not propose (the fixed versions), the
  `rationale`, and `reviewedWithDocusaurus`, the Docusaurus version of the last
  review.
* **`keepOverrides`:** override keys (as in `pnpm-workspace.yaml`) that the
  rebuild must keep, mapped to a rationale. Use it for overrides that fix
  compatibility rather than vulnerabilities.

## Local Commands

Run these commands from the `website/` folder:

```bash
# Run the maintenance locally; inspect the result with `git diff`.
pnpm deps:maintain
# Same, retrying all blocked advisories.
pnpm deps:maintain --recheck-blocks
# Block an advisory.
pnpm deps:block --ghsa GHSA-xxxx-xxxx-xxxx --rationale "Why the fix breaks"
# Smoke-test an existing build/.
pnpm smoke-test
```

`deps:maintain` exits with `0` when done, `2` when a person needs to act (see
the report), and `1` on errors.

## Limitations

* **Release age:** pnpm installs only releases older than
  `minimumReleaseAge` (one day). Very recent fixes are applied by a later
  run.
* **Unfixable advisories:** advisories without a published fix stay in the
  report until a fix is available. Block them only to silence `pnpm audit`.
* **Pull request checks:** pull requests created by the workflow do not
  trigger other workflows (a GitHub restriction for `GITHUB_TOKEN`). The
  maintenance run itself has built and smoke-tested the changes.
* **Smoke test scope:** the smoke test checks served pages and assets; it does
  not execute JavaScript in a browser.
