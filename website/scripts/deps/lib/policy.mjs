/**
 * Reads and writes `dependency-policy.json` and mirrors blocked advisories
 * into `pnpm-workspace.yaml` (`auditConfig.ignoreGhsas`) and
 * `.github/dependabot.yml` (ignore rules).
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { syncDependabotIgnores } from './files.mjs';

/** The website directory (current working directory of all scripts). */
export const SITE_DIR = process.cwd();
export const POLICY_PATH = join(SITE_DIR, 'dependency-policy.json');
export const WORKSPACE_PATH = join(SITE_DIR, 'pnpm-workspace.yaml');
export const PACKAGE_JSON_PATH = join(SITE_DIR, 'package.json');
export const LOCKFILE_PATH = join(SITE_DIR, 'pnpm-lock.yaml');
export const DEPENDABOT_PATH = join(SITE_DIR, '..', '.github', 'dependabot.yml');
export const DEPENDABOT_DIRECTORY = '/website';

/**
 * @typedef {object} BlockedAdvisory
 * @property {string} ghsa - the accepted advisory
 * @property {string} package - the affected package
 * @property {string} [ignoreVersions] - versions Dependabot must not propose
 * @property {string} rationale - why the fix cannot be applied
 * @property {string} reviewedWithDocusaurus - Docusaurus version of the last review
 */

/**
 * @typedef {object} Policy
 * @property {BlockedAdvisory[]} blockedAdvisories - accepted advisories
 * @property {Record<string, string>} keepOverrides - override keys never removed, with rationale
 */

/** @returns {Policy} the policy */
export const loadPolicy = () => {
  const raw = JSON.parse(readFileSync(POLICY_PATH, 'utf8'));
  return {
    ...raw,
    blockedAdvisories: raw.blockedAdvisories ?? [],
    keepOverrides: raw.keepOverrides ?? {},
  };
};

/** @param {Policy} policy - the policy to write */
export const savePolicy = (policy) => {
  const sorted = {
    ...policy,
    blockedAdvisories: [...policy.blockedAdvisories].sort((a, b) => a.ghsa.localeCompare(b.ghsa)),
  };
  writeFileSync(POLICY_PATH, `${JSON.stringify(sorted, null, 2)}\n`);
};

/**
 * Writes blocked advisories into the workspace ignore list and Dependabot.
 *
 * @param {import('./files.mjs').WorkspaceFile} workspace - the workspace file (saved by the caller)
 * @param {BlockedAdvisory[]} previous - blocks before the change
 * @param {BlockedAdvisory[]} current - blocks after the change
 */
export const applyBlocks = (workspace, previous, current) => {
  const previousGhsas = new Set(previous.map((block) => block.ghsa));
  const unmanaged = workspace.ignoredGhsas().filter((ghsa) => !previousGhsas.has(ghsa));
  workspace.setIgnoredGhsas([...unmanaged, ...current.map((block) => block.ghsa)]);

  const managedNames = new Set([...previous, ...current].map((block) => block.package));
  const rules = new Map();
  // Without a known fixed version there is no update for Dependabot to skip.
  for (const block of current.filter((candidate) => candidate.ignoreVersions)) {
    const key = `${block.package}|${block.ignoreVersions}`;
    const rule = rules.get(key) ?? { name: block.package, versions: block.ignoreVersions, ghsas: [] };
    rule.ghsas.push(block.ghsa);
    rules.set(key, rule);
  }
  syncDependabotIgnores(
    DEPENDABOT_PATH,
    DEPENDABOT_DIRECTORY,
    managedNames,
    [...rules.values()].map((rule) => ({
      ...rule,
      comment: `Blocked ${rule.ghsas.join(', ')}: managed via website/dependency-policy.json`,
    })),
  );
};
