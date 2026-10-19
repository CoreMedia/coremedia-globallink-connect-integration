# Website Dependency Maintenance

The website dependencies maintain themselves: a GitHub workflow updates
Docusaurus and all other packages, fixes vulnerabilities, cleans up overrides,
and tests that the website still works. People only review pull requests, and
decide in the rare case that an update breaks the website.

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
* **A Dependabot security pull request appears:**
  If its check "Test deployment" passed, merge it. Otherwise, close it and run
  the workflow with action `maintain`, which fixes the same advisory. Dependabot
  also closes its pull request by itself once the advisory is fixed.

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

The workflow `.github/workflows/website-dependencies.yml` runs every Monday
and on demand. The action `maintain` runs `scripts/deps/maintain.mjs`:

1. **Docusaurus first:** updates all `@docusaurus/*` packages to the latest
   minor or patch release, then verifies the website. New Docusaurus releases
   often update the transitive dependencies that overrides patch. Major
   releases require a manual migration and are only reported.
2. **Package updates:** `pnpm update` updates all other packages, including
   transitive ones, to the latest versions their declared ranges allow; this
   changes only the lockfile. If the result breaks the website, the updates
   are discarded and the run asks a developer to investigate.
3. **Review blocks:** removes blocked advisories that no longer apply.
4. **Rebuild overrides:** removes all overrides, then lets pnpm fix the
   vulnerabilities again:
   * `pnpm audit --fix update` updates vulnerable packages within the version
     ranges their parents allow; this changes only the lockfile.
   * `pnpm audit --fix override` adds an override only where a parent does
     not allow a fixed version. Each new override gets a comment with the
     severity and advisory link.

   This keeps the smallest set of overrides; obsolete overrides disappear
   automatically. If the result breaks the website, the run falls back to
   adding fixes to the existing overrides.
5. **Verify:** every change is checked by `pnpm install`, `pnpm run build`,
   and `pnpm run smoke-test`. Changes that fail are reverted and reported.
6. **Report:** the pull request description lists the Docusaurus status,
   updated packages, newer versions outside the declared ranges, fixed and
   remaining vulnerabilities, removed and added overrides, and blocks.

The smoke test (`scripts/deps/smoke-test.mjs`) serves `build/` on a free
port, requests every page from the sitemap and the scripts and styles of the
home page, and stops the server afterward.

### Division of Labor With Dependabot

* **The maintenance workflow** owns all website dependency updates: Docusaurus,
  package updates, overrides, and blocked advisories. It verifies everything
  in one run and opens a single pull request.
* **Dependabot** only raises security updates for the website, as an early
  alert between the weekly runs. Its version updates are disabled
  (`open-pull-requests-limit: 0`), because they duplicate the package updates
  without adapting overrides. Each Dependabot pull request is tested by
  `pages-test-deploy.yml` (typecheck, build, smoke test); a passing check is
  the review.

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
# Only fix vulnerabilities, without Docusaurus and package updates.
pnpm deps:maintain --skip-docusaurus --skip-updates
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
