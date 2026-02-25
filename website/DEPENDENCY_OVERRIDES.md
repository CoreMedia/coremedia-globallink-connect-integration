# Dependency Overrides Maintenance Guide

## Current Overrides (as of February 2026)

### Security-Related Overrides

| Package                    | Override       | Reason                                | Review Date | Status        |
|----------------------------|----------------|---------------------------------------|-------------|---------------|
| `ajv@>=8.0.0 <8.18.0`      | `^8.18.0`      | CVE-2025-69873 fix                    | 2026-02-19  | Active        |
| `ajv@6.12.6`               | N/A (Excluded) | Unmaintained `file-loader` dependency | 2026-02-19  | Accepted Risk |
| `baseline-browser-mapping` | `^2.9.19`      | Keep browser data current             | 2026-02-19  | Active        |
| `express`                  | `^4.22.1`      | Security vulnerability fix            | 2026-02-19  | Active        |
| `qs`                       | `^6.14.1`      | Security vulnerability fix            | 2026-02-19  | Active        |
| `lodash`                   | `^4.17.23`     | Security vulnerability fix            | 2026-02-19  | Active        |

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
