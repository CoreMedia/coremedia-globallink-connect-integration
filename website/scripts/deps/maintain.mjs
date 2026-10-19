#!/usr/bin/env node
/**
 * Website dependency maintenance: one run keeps Docusaurus current, fixes
 * vulnerabilities with the smallest set of overrides, and reviews blocked
 * advisories. Every change is verified by install, build, and smoke test;
 * changes that break the site are reverted and reported.
 *
 * Steps:
 *   1. Update `@docusaurus/*` to the latest minor or patch release.
 *   2. Drop blocked advisories that no longer apply. Blocks last reviewed
 *      with another Docusaurus version are retried in step 3.
 *   3. Rebuild overrides: remove all (except `keepOverrides`), then
 *      `pnpm audit --fix update` (lockfile only) and
 *      `pnpm audit --fix override` for the rest. Fallbacks: keep retried
 *      blocks, then only add fixes to the existing overrides.
 *   4. Write a Markdown report.
 *
 * Usage (from `website/`):
 *   node ./scripts/deps/maintain.mjs [--report <file>] [--recheck-blocks] [--skip-docusaurus]
 *
 * Exit codes: 0 = done; 2 = done, but action required (see report); 1 = error.
 */

import { appendFileSync, writeFileSync } from 'node:fs';
import { parseArgs } from 'node:util';
import semver from 'semver';
import { changedSince, restore, snapshot, WorkspaceFile } from './lib/files.mjs';
import { audit, installedVersion, latestRelease, pnpm, startGroup } from './lib/pnpm.mjs';
import {
  applyBlocks,
  DEPENDABOT_PATH,
  loadPolicy,
  LOCKFILE_PATH,
  PACKAGE_JSON_PATH,
  POLICY_PATH,
  savePolicy,
  WORKSPACE_PATH,
} from './lib/policy.mjs';
import { runSmokeTest } from './smoke-test.mjs';

const { values: options } = parseArgs({
  options: {
    report: { type: 'string' },
    'recheck-blocks': { type: 'boolean', default: false },
    'skip-docusaurus': { type: 'boolean', default: false },
  },
});

const TRACKED_FILES = [PACKAGE_JSON_PATH, WORKSPACE_PATH, LOCKFILE_PATH, POLICY_PATH, DEPENDABOT_PATH];
const DOCUSAURUS_CORE = '@docusaurus/core';

/** Collected results for the report. */
const report = {
  docusaurus: { status: 'skipped', current: '', latest: '' },
  vulnerabilitiesBefore: [],
  vulnerabilitiesAfter: [],
  overridesBefore: {},
  overridesAfter: {},
  securityPlan: '',
  securityFailures: [],
  blocksRemoved: [],
  blocksUnblocked: [],
  blocksKept: [],
  actionRequired: [],
};

/**
 * Installs, builds, and smoke-tests the website.
 *
 * @param {string} label - what is verified
 * @param {Map<string, string>} [unchangedFrom] - skip build and smoke test if the
 *   files equal this snapshot after install
 * @returns {Promise<{ok: boolean, step?: string, details?: string[]}>} the result
 */
const verify = async (label, unchangedFrom) => {
  console.log(`\n▶ Verifying: ${label}`);
  let end = startGroup(`pnpm install (${label})`);
  const installed = pnpm(['install', '--no-frozen-lockfile']).ok;
  end();
  if (!installed) return { ok: false, step: 'install' };
  if (unchangedFrom && !changedSince(unchangedFrom)) {
    console.log('No changes; build and smoke test skipped.');
    return { ok: true };
  }

  end = startGroup(`pnpm run build (${label})`);
  const built = pnpm(['run', 'build']).ok;
  end();
  if (!built) return { ok: false, step: 'build' };

  end = startGroup(`smoke test (${label})`);
  const smoke = await runSmokeTest();
  end();
  if (!smoke.passed) return { ok: false, step: 'smoke test', details: smoke.failures };

  console.log(`✔ Verified: ${label}`);
  return { ok: true };
};

const describeFailure = (result) =>
  `${result.step} failed${result.details?.length ? `: ${result.details.slice(0, 5).join('; ')}` : ''}`;

/**
 * Step 1: updates all `@docusaurus/*` catalog entries to the latest minor or
 * patch release, if it is old enough for `minimumReleaseAge`.
 *
 * @returns {Promise<string>} the Docusaurus version after this step
 */
const updateDocusaurus = async () => {
  const current = installedVersion(DOCUSAURUS_CORE);
  report.docusaurus.current = current;
  if (options['skip-docusaurus']) return current;

  const latest = latestRelease(DOCUSAURUS_CORE);
  report.docusaurus.latest = latest.version;
  if (!semver.gt(latest.version, current)) {
    report.docusaurus.status = 'up-to-date';
    return current;
  }
  if (semver.major(latest.version) !== semver.major(current)) {
    // Informational only: a major upgrade is a planned task, not a failure.
    report.docusaurus.status = 'major-available';
    return current;
  }

  const workspace = new WorkspaceFile(WORKSPACE_PATH);
  const ageMinutes = (Date.now() - latest.published.getTime()) / 60_000;
  if (ageMinutes < workspace.minimumReleaseAgeMinutes()) {
    report.docusaurus.status = 'too-new';
    return current;
  }

  const saved = snapshot(TRACKED_FILES);
  for (const name of Object.keys(workspace.catalog()).filter((key) => key.startsWith('@docusaurus/'))) {
    workspace.setCatalogEntry(name, `^${latest.version}`);
  }
  workspace.save();

  const result = await verify(`Docusaurus ${latest.version}`);
  if (result.ok) {
    report.docusaurus.status = 'updated';
    return latest.version;
  }
  restore(saved);
  report.docusaurus.status = 'failed';
  report.actionRequired.push(
    `The update to Docusaurus ${latest.version} breaks the site (${describeFailure(result)}). Update manually.`,
  );
  return current;
};

/**
 * Step 2: sorts blocked advisories into obsolete, to retry, and to keep.
 * A block is retried when the Docusaurus version changed since its last
 * review, or when a fixed version was published since.
 *
 * @param {import('./lib/policy.mjs').BlockedAdvisory[]} blocks - the blocked advisories
 * @param {string} docusaurusVersion - the current Docusaurus version
 * @returns {{obsolete: object[], retry: object[], keep: object[], patched: Map<string, string>}} the classification
 *   and the currently known fixed versions by advisory
 */
const reviewBlocks = (blocks, docusaurusVersion) => {
  const patched = new Map();
  if (blocks.length === 0) return { obsolete: [], retry: [], keep: [], patched };

  // Audit with the blocked advisories not ignored, to see which still apply.
  const saved = snapshot([WORKSPACE_PATH]);
  const blockedGhsas = new Set(blocks.map((block) => block.ghsa));
  const workspace = new WorkspaceFile(WORKSPACE_PATH);
  workspace.setIgnoredGhsas(workspace.ignoredGhsas().filter((ghsa) => !blockedGhsas.has(ghsa)));
  workspace.save();
  const reported = new Set();
  try {
    for (const advisory of audit()) {
      reported.add(advisory.ghsa);
      if (advisory.patched) patched.set(advisory.ghsa, advisory.patched);
    }
  } finally {
    restore(saved);
  }

  const obsolete = blocks.filter((block) => !reported.has(block.ghsa));
  const relevant = blocks.filter((block) => reported.has(block.ghsa));
  const retry = relevant.filter(
    (block) =>
      options['recheck-blocks'] ||
      block.reviewedWithDocusaurus !== docusaurusVersion ||
      (patched.has(block.ghsa) && patched.get(block.ghsa) !== block.ignoreVersions),
  );
  const keep = relevant.filter((block) => !retry.includes(block));
  return { obsolete, retry, keep, patched };
};

/**
 * Applies vulnerability fixes according to a plan and verifies the site.
 *
 * @param {{label: string, rebuild: boolean, unblock: object[]}} plan - the plan
 * @param {Set<string>} keepOverrides - override keys never removed
 * @returns {Promise<{ok: boolean, step?: string, details?: string[]}>} the result
 */
const applySecurityPlan = async (plan, keepOverrides) => {
  console.log(`\n▶ Vulnerability fixes: ${plan.label}`);
  const saved = snapshot(TRACKED_FILES);
  const workspace = new WorkspaceFile(WORKSPACE_PATH);
  const unblock = new Set(plan.unblock.map((block) => block.ghsa));
  if (unblock.size > 0) {
    workspace.setIgnoredGhsas(workspace.ignoredGhsas().filter((ghsa) => !unblock.has(ghsa)));
  }
  if (plan.rebuild) workspace.removeOverridesExcept(keepOverrides);
  workspace.save();

  if (changedSince(saved)) {
    const end = startGroup('pnpm install (resolve without removed overrides)');
    const installed = pnpm(['install', '--no-frozen-lockfile']).ok;
    end();
    if (!installed) return { ok: false, step: 'install' };
  }

  let end = startGroup('pnpm audit --fix update');
  pnpm(['audit', '--fix', 'update']);
  end();

  const remaining = audit();
  if (remaining.some((advisory) => advisory.patched)) {
    end = startGroup('pnpm audit --fix override');
    pnpm(['audit', '--fix', 'override']);
    end();
    const byKey = new Map(remaining.map((advisory) => [`${advisory.module}@${advisory.vulnerable}`, advisory]));
    const annotated = new WorkspaceFile(WORKSPACE_PATH);
    annotated.annotateOverrides((key) => {
      const advisory = byKey.get(key);
      return advisory ? `${advisory.severity}: ${advisory.url}` : undefined;
    });
    annotated.save();
  }

  // Normalize key order, so an unchanged result is detected as unchanged.
  new WorkspaceFile(WORKSPACE_PATH).save();
  if (!changedSince(saved)) {
    console.log('No changes required.');
    return { ok: true };
  }
  return verify(plan.label, saved);
};

/**
 * Step 3: fixes vulnerabilities, trying the plans in order until one works.
 *
 * @param {{retry: object[]}} blocks - the block review result
 * @param {Set<string>} keepOverrides - override keys never removed
 * @returns {Promise<object | null>} the successful plan, or null
 */
const fixVulnerabilities = async (blocks, keepOverrides) => {
  const plans = [];
  if (blocks.retry.length > 0) {
    plans.push({ label: 'rebuild overrides and retry blocked advisories', rebuild: true, unblock: blocks.retry });
  }
  plans.push({ label: 'rebuild overrides', rebuild: true, unblock: [] });
  plans.push({ label: 'add fixes to existing overrides', rebuild: false, unblock: [] });

  const base = snapshot(TRACKED_FILES);
  for (const plan of plans) {
    const result = await applySecurityPlan(plan, keepOverrides);
    if (result.ok) return plan;
    report.securityFailures.push(`${plan.label}: ${describeFailure(result)}`);
    restore(base);
  }
  return null;
};

const advisoryLine = (advisory) =>
  `\`${advisory.module}\` ${advisory.versions.join(', ')} – ${advisory.severity} – [${advisory.ghsa}](${advisory.url})`;

/** @returns {string} the Markdown report */
const renderReport = () => {
  const lines = ['## Website Dependency Maintenance', ''];
  const status = report.actionRequired.length > 0 ? '⚠️ Action required' : '✅ No action required';
  lines.push(`**Result:** ${status}`, '');

  const { docusaurus } = report;
  const docusaurusText = {
    skipped: `Not checked (installed: ${docusaurus.current}).`,
    'up-to-date': `${docusaurus.current} is the latest version.`,
    updated: `Updated from ${docusaurus.current} to ${docusaurus.latest} (build and smoke test passed).`,
    'too-new': `${docusaurus.latest} is available but younger than \`minimumReleaseAge\`; the next run retries.`,
    'major-available': `${docusaurus.latest} is a new major version; upgrade manually following the Docusaurus migration guide.`,
    failed: `Updating to ${docusaurus.latest} breaks the site; kept ${docusaurus.current}.`,
  }[docusaurus.status];
  lines.push('### Docusaurus', '', docusaurusText, '');

  const key = (advisory) => `${advisory.ghsa}|${advisory.module}`;
  const after = new Set(report.vulnerabilitiesAfter.map(key));
  const before = new Set(report.vulnerabilitiesBefore.map(key));
  const fixed = report.vulnerabilitiesBefore.filter((advisory) => !after.has(key(advisory)));
  const added = report.vulnerabilitiesAfter.filter((advisory) => !before.has(key(advisory)));
  lines.push('### Vulnerabilities', '');
  lines.push(`Before: ${report.vulnerabilitiesBefore.length}, after: ${report.vulnerabilitiesAfter.length}.`, '');
  if (report.securityPlan) lines.push(`Applied: ${report.securityPlan}.`, '');
  if (fixed.length > 0) lines.push('Fixed:', '', ...fixed.map((a) => `* ${advisoryLine(a)}`), '');
  if (added.length > 0) lines.push('Newly reported:', '', ...added.map((a) => `* ${advisoryLine(a)}`), '');
  if (report.vulnerabilitiesAfter.length > 0) {
    lines.push('Remaining:', '');
    for (const advisory of report.vulnerabilitiesAfter) {
      const reason = advisory.patched ? 'fix breaks the site or is not yet installable' : 'no fixed version published';
      lines.push(`* ${advisoryLine(advisory)} (${reason})`);
    }
    lines.push('');
  }
  if (report.securityFailures.length > 0) {
    lines.push('Discarded attempts:', '', ...report.securityFailures.map((failure) => `* ${failure}`), '');
  }

  const removedOverrides = Object.keys(report.overridesBefore).filter((k) => !(k in report.overridesAfter));
  const addedOverrides = Object.keys(report.overridesAfter).filter((k) => !(k in report.overridesBefore));
  lines.push('### Overrides', '');
  lines.push(
    `Before: ${Object.keys(report.overridesBefore).length}, after: ${Object.keys(report.overridesAfter).length}.`,
    '',
  );
  if (removedOverrides.length > 0) {
    lines.push('Removed (no longer needed):', '', ...removedOverrides.map((k) => `* \`${k}\``), '');
  }
  if (addedOverrides.length > 0) {
    lines.push('Added:', '', ...addedOverrides.map((k) => `* \`${k}\`: \`${report.overridesAfter[k]}\``), '');
  }

  lines.push('### Blocked Advisories', '');
  const blockLine = (block) => `* [${block.ghsa}](https://github.com/advisories/${block.ghsa}) \`${block.package}\`: ${block.rationale}`;
  if (report.blocksRemoved.length > 0) lines.push('Removed (no longer applies):', '', ...report.blocksRemoved.map(blockLine), '');
  if (report.blocksUnblocked.length > 0) lines.push('Unblocked (fix works now):', '', ...report.blocksUnblocked.map(blockLine), '');
  if (report.blocksKept.length > 0) lines.push('Still blocked:', '', ...report.blocksKept.map(blockLine), '');
  if (report.blocksRemoved.length + report.blocksUnblocked.length + report.blocksKept.length === 0) {
    lines.push('None.', '');
  }

  if (report.actionRequired.length > 0) {
    lines.push('### Action Required', '', ...report.actionRequired.map((item) => `* ${item}`), '');
  }
  return lines.join('\n');
};

const main = async () => {
  const initialPolicy = loadPolicy();
  report.overridesBefore = new WorkspaceFile(WORKSPACE_PATH).overrides();
  report.vulnerabilitiesBefore = audit();

  const docusaurusVersion = await updateDocusaurus();

  const blocks = reviewBlocks(initialPolicy.blockedAdvisories, docusaurusVersion);
  const keepOverrides = new Set(Object.keys(initialPolicy.keepOverrides));
  const plan = await fixVulnerabilities(blocks, keepOverrides);
  report.securityPlan = plan?.label ?? '';

  // Only blocks whose advisory is gone after the fixes count as unblocked;
  // the others stay blocked and are marked as reviewed.
  const reportedAfterFixes = new Set(audit().map((advisory) => advisory.ghsa));
  const unblocked = (plan?.unblock ?? []).filter((block) => !reportedAfterFixes.has(block.ghsa));
  report.blocksRemoved = blocks.obsolete;
  report.blocksUnblocked = unblocked;
  report.blocksKept = [
    ...blocks.keep,
    ...blocks.retry
      .filter((block) => !unblocked.includes(block))
      .map(({ ignoreVersions, ...block }) => {
        const fixedVersions = blocks.patched.get(block.ghsa) ?? ignoreVersions;
        return { ...block, ...(fixedVersions ? { ignoreVersions: fixedVersions } : {}), reviewedWithDocusaurus: docusaurusVersion };
      }),
  ];

  const policy = { ...loadPolicy(), blockedAdvisories: report.blocksKept };
  const workspace = new WorkspaceFile(WORKSPACE_PATH);
  applyBlocks(workspace, initialPolicy.blockedAdvisories, policy.blockedAdvisories);
  workspace.save();
  savePolicy(policy);

  report.overridesAfter = new WorkspaceFile(WORKSPACE_PATH).overrides();
  report.vulnerabilitiesAfter = audit();
  for (const advisory of report.vulnerabilitiesAfter.filter((a) => a.patched)) {
    report.actionRequired.push(
      `${advisory.ghsa} (\`${advisory.module}\`) has a fix that could not be applied. If the fix breaks the site, ` +
        `run the "Website Dependencies" workflow with action "block-advisory" and GHSA ${advisory.ghsa}.`,
    );
  }

  const markdown = renderReport();
  console.log(`\n${markdown}`);
  if (options.report) writeFileSync(options.report, markdown);
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, `${markdown}\n`);
  return report.actionRequired.length > 0 ? 2 : 0;
};

try {
  process.exitCode = await main();
} catch (error) {
  console.error(error);
  process.exitCode = 1;
}
