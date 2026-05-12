# Dependency Overrides Maintenance Guide

Overrides are applied (and documented) in `pnpm-workspace.yaml`.

## Quick Reference

| Command                  | Description                                         |
|--------------------------|-----------------------------------------------------|
| `pnpm overrides`         | Check which overrides are removable (CI-friendly)   |
| `pnpm overrides:fix`     | Auto-remove unnecessary overrides with verification |
| `pnpm fix-audit`         | Auto-add overrides for new vulnerabilities          |
| `pnpm fix-audit:dry-run` | Report fixable vulnerabilities without changes      |

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
