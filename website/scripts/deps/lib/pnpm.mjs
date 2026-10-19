/**
 * Helpers to run pnpm and to parse `pnpm audit` results.
 */

import { spawnSync } from 'node:child_process';

const useShell = process.platform === 'win32';

/** Whether the scripts run in GitHub Actions. */
export const inGitHubActions = process.env.GITHUB_ACTIONS === 'true';

/**
 * Logs a section header; collapsible in GitHub Actions.
 *
 * @param {string} title - the section title
 * @returns {() => void} a function that ends the section
 */
export const startGroup = (title) => {
  if (inGitHubActions) {
    console.log(`::group::${title}`);
    return () => console.log('::endgroup::');
  }
  console.log(`\n=== ${title} ===`);
  return () => {};
};

/**
 * Runs pnpm with the given arguments.
 *
 * @param {string[]} args - the pnpm arguments
 * @param {{capture?: boolean}} [options] - capture output instead of streaming it
 * @returns {{ok: boolean, status: number | null, stdout: string, stderr: string}} the result
 */
export const pnpm = (args, { capture = false } = {}) => {
  const result = spawnSync('pnpm', args, {
    encoding: 'utf8',
    shell: useShell,
    stdio: capture ? ['ignore', 'pipe', 'pipe'] : 'inherit',
    maxBuffer: 256 * 1024 * 1024,
  });
  if (result.error) throw result.error;
  return {
    ok: result.status === 0,
    status: result.status,
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? '',
  };
};

/**
 * Runs pnpm and throws if it fails.
 *
 * @param {string[]} args - the pnpm arguments
 * @returns {string} captured standard output
 */
export const pnpmOrThrow = (args) => {
  const result = pnpm(args, { capture: true });
  if (!result.ok) {
    throw new Error(`"pnpm ${args.join(' ')}" failed (exit ${result.status}):\n${result.stderr || result.stdout}`);
  }
  return result.stdout;
};

/**
 * @typedef {object} Advisory
 * @property {string} ghsa - the GitHub advisory ID
 * @property {string} module - the affected package
 * @property {string} vulnerable - the vulnerable version range
 * @property {string | null} patched - the patched version range, if any
 * @property {string} severity - the severity
 * @property {string} title - the advisory title
 * @property {string} url - the advisory URL
 * @property {string[]} versions - the installed vulnerable versions
 */

/**
 * Runs `pnpm audit` and returns the reported advisories. Ignored advisories
 * (`auditConfig.ignoreGhsas`) are not reported.
 *
 * @returns {Advisory[]} the advisories, sorted by package and ID
 */
export const audit = () => {
  // pnpm audit exits non-zero when it finds vulnerabilities.
  const result = pnpm(['audit', '--json'], { capture: true });
  let report;
  try {
    report = JSON.parse(result.stdout);
  } catch {
    throw new Error(`"pnpm audit --json" returned no valid JSON:\n${result.stderr || result.stdout}`);
  }
  if (report.error) {
    throw new Error(`"pnpm audit" failed: ${JSON.stringify(report.error)}`);
  }
  return Object.values(report.advisories ?? {})
    .map((advisory) => ({
      ghsa: advisory.github_advisory_id,
      module: advisory.module_name,
      vulnerable: advisory.vulnerable_versions,
      patched: advisory.patched_versions && advisory.patched_versions !== '<0.0.0' ? advisory.patched_versions : null,
      severity: advisory.severity,
      title: advisory.title,
      url: advisory.url,
      versions: [...new Set((advisory.findings ?? []).map((finding) => finding.version))],
    }))
    .sort((a, b) => a.module.localeCompare(b.module) || a.ghsa.localeCompare(b.ghsa));
};

/**
 * Returns the installed version of a direct dependency.
 *
 * @param {string} name - the package name
 * @returns {string | undefined} the installed version
 */
export const installedVersion = (name) => {
  const [project] = JSON.parse(pnpmOrThrow(['list', name, '--depth', '0', '--json']));
  const entry = project?.dependencies?.[name] ?? project?.devDependencies?.[name];
  return entry?.version;
};

/**
 * Returns the latest published version and its release time.
 *
 * @param {string} name - the package name
 * @returns {{version: string, published: Date}} the latest release
 */
export const latestRelease = (name) => {
  const info = JSON.parse(pnpmOrThrow(['view', name, 'dist-tags', 'time', '--json']));
  const version = info['dist-tags'].latest;
  return { version, published: new Date(info.time[version]) };
};
