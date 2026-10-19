#!/usr/bin/env node
/**
 * Blocks (accepts) a vulnerability advisory whose fix breaks the website.
 *
 * Adds the advisory to `dependency-policy.json`, to `auditConfig.ignoreGhsas`
 * in `pnpm-workspace.yaml`, and as an ignore rule to `.github/dependabot.yml`.
 * The affected package and the versions to ignore are taken from
 * `pnpm audit`. The maintenance run removes the block once the advisory no
 * longer applies, and retries the fix after each Docusaurus update.
 *
 * Usage (from `website/`):
 *   node ./scripts/deps/block.mjs --ghsa <GHSA-ID> --rationale <text> [--report <file>]
 *
 * Exit codes: 0 = blocked or already blocked; 1 = error.
 */

import { appendFileSync, writeFileSync } from 'node:fs';
import { parseArgs } from 'node:util';
import { WorkspaceFile } from './lib/files.mjs';
import { audit, installedVersion } from './lib/pnpm.mjs';
import { applyBlocks, loadPolicy, savePolicy, WORKSPACE_PATH } from './lib/policy.mjs';

const GHSA_PATTERN = /^GHSA(-[23456789cfghjmpqrvwx]{4}){3}$/;

const { values: options } = parseArgs({
  options: {
    ghsa: { type: 'string' },
    rationale: { type: 'string' },
    report: { type: 'string' },
  },
});

const fail = (message) => {
  console.error(`Error: ${message}`);
  process.exit(1);
};

const ghsa = options.ghsa?.trim() ?? '';
const rationale = options.rationale?.trim() ?? '';
if (!GHSA_PATTERN.test(ghsa)) fail(`"${ghsa}" is not a valid GitHub advisory ID (GHSA-xxxx-xxxx-xxxx).`);
if (rationale.length < 10) fail('Provide a rationale explaining why the fix cannot be applied.');

const policy = loadPolicy();
if (policy.blockedAdvisories.some((block) => block.ghsa === ghsa)) {
  console.log(`${ghsa} is already blocked. Nothing to do.`);
  process.exit(0);
}

const advisory = audit().find((candidate) => candidate.ghsa === ghsa);
if (!advisory) {
  fail(`${ghsa} is not reported by "pnpm audit" for the website. Nothing to block.`);
}

const block = {
  ghsa,
  package: advisory.module,
  ...(advisory.patched ? { ignoreVersions: advisory.patched } : {}),
  rationale,
  reviewedWithDocusaurus: installedVersion('@docusaurus/core'),
};
const updated = { ...policy, blockedAdvisories: [...policy.blockedAdvisories, block] };

const workspace = new WorkspaceFile(WORKSPACE_PATH);
applyBlocks(workspace, policy.blockedAdvisories, updated.blockedAdvisories);
workspace.save();
savePolicy(updated);

const markdown = [
  '## Blocked Advisory',
  '',
  `* Advisory: [${ghsa}](${advisory.url}) (${advisory.severity}): ${advisory.title}`,
  `* Package: \`${advisory.module}\` ${advisory.versions.join(', ')}`,
  `* Dependabot ignores: ${block.ignoreVersions ? `\`${block.ignoreVersions}\`` : 'nothing (no fixed version published yet)'}`,
  `* Rationale: ${rationale}`,
  `* Reviewed with Docusaurus: ${block.reviewedWithDocusaurus}`,
  '',
  'The maintenance run removes this block when the advisory no longer applies',
  'and retries the fix after the next Docusaurus update.',
  '',
].join('\n');
console.log(markdown);
if (options.report) writeFileSync(options.report, markdown);
if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, `${markdown}\n`);
