# Dependency Overrides Maintenance Guide

Overrides are applied (and documented) in `pnpm-workspace.yaml`.

## Quick Reference

| Command                  | Description                                         |
|--------------------------|-----------------------------------------------------|
| `pnpm overrides`         | Check which overrides are removable (CI-friendly)   |
| `pnpm overrides:fix`     | Auto-remove unnecessary overrides with verification |
| `pnpm fix-audit`         | Auto-add overrides for new vulnerabilities          |
| `pnpm fix-audit:dry-run` | Report fixable vulnerabilities without changes      |
| `pnpm blocked-vuln:check` | Verify blocked vulnerability policies are still required |

## Automated Workflow

The **fix-vulnerability-overrides** GitHub Action runs weekly (Monday 06:30
UTC) and on manual dispatch. It:

1. Runs `pnpm audit --json` to detect vulnerabilities
2. Determines ranged overrides (`pkg@>=vulnerable <patched: ^patched`)
3. Adds overrides to `pnpm-workspace.yaml`
4. Verifies: `pnpm install` → `pnpm audit` → `pnpm build`
5. Opens a PR if all checks pass

Ranged overrides are self-documenting and automatically removable: once the
parent package updates its dependency declaration past the vulnerable range,
`pnpm overrides` will flag the override as "LIKELY REMOVABLE."

## Blocked Vulnerability Policies

Some advisories are intentionally accepted for a limited period when no safe
upgrade path exists. Policies are configured in
`scripts/blocked-vulnerabilities.json` and checked by the
**check-blocked-vulnerabilities** workflow.

The check fails when no installed versions remain in a policy's blocked range.
That failure is the signal to remove temporary suppressions.

### Adding a New Blocked Vulnerability

When you see a Dependabot alert for a package with a breaking issue (no safe
upgrade path):

1. **Navigate to Actions tab** and select the **Add Blocked Vulnerability**
   workflow
2. **Click "Run workflow"** and provide:
   - **Package name:** e.g., `js-yaml`
   - **Current version:** The version from the alert, e.g., `3.14.2`
   - **GHSA ID:** From the GitHub Advisory, e.g., `GHSA-h67p-54hq-rp68`
   - **Rationale:** Why updates are blocked, e.g., _Update to versions above 3.x
     break the build_

3. **Review and merge** the created PR

The workflow will:
- Update `website/scripts/blocked-vulnerabilities.json` with the new policy
- Add the GHSA to `website/pnpm-workspace.yaml` ignore list
- Update `.github/dependabot.yml` to block major version jumps (allowing patch
  updates)
- Create a PR with a summary of all changes

**Note:** If a **new GHSA** is discovered for an **already-blocked package**
(e.g., js-yaml with a second advisory), simply run the workflow again with the
same package name and new GHSA. The script will add the new advisory to the
existing policy without duplicating the rationale.

### Unblock Checklist

1. Update dependencies until blocked versions no longer appear in
   `pnpm-lock.yaml`.
2. Remove matching entries from `auditConfig.ignoreGhsas` in
   `pnpm-workspace.yaml`.
3. Remove matching `ignore` rules in `.github/dependabot.yml`.
4. Remove the policy from `website/scripts/blocked-vulnerabilities.json`.
5. Run:

   ```bash
   pnpm blocked-vuln:check
   pnpm audit
   pnpm run build
   ```

## Maintenance Process

### When to Review Overrides

1. **Before updating @docusaurus/core or major dependencies**
2. **When Dependabot flags new vulnerabilities**
3. **After major version updates of any dependency**

### Review Checklist

For each override, perform the following checks:

#### 1. Check if Override is Still Needed

```bash
# Run dependency tree analysis
pnpm why <package-name>

# Check if the parent dependency still uses the vulnerable version
# If the dependency tree no longer shows the old version, remove the override
```

#### 2. Check for Conflicts with Updated Dependencies

```bash
# After updating @docusaurus/core (or other major deps):
pnpm install

# Look for warnings like:
# "WARN  Issues with peer dependencies found"
# "WARN  Found incompatible version"
```

#### 3. Validate Build Still Works

```bash
# Full clean build test
pnpm run clear
pnpm run build

# Check for:
# - Build errors
# - Deprecation warnings
# - Runtime errors in the built site
pnpm run serve
```

#### 4. Check for Security Updates

```bash
# Run audit
pnpm audit

# Check CVE databases for new issues
# Review Dependabot alerts
```
