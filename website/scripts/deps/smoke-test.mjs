#!/usr/bin/env node
/**
 * Smoke test for the built website.
 *
 * Serves `build/` with `docusaurus serve` on a free port, then verifies that
 * every page listed in `build/sitemap.xml` responds with a rendered
 * Docusaurus page, that the home page shows the expected heading, and that
 * all scripts and stylesheets referenced by the home page are reachable.
 *
 * The server runs in its own process group, which is terminated as a whole
 * after the test, so no server survives the test run.
 *
 * Usage (from `website/`, after `pnpm run build`):
 *   node ./scripts/deps/smoke-test.mjs
 *
 * Exit codes: 0 = passed, 1 = failed.
 */

import { spawn } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { createServer } from 'node:net';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';

/** Text the home page must contain. */
const HOME_PAGE_MARKER = 'CoreMedia GlobalLink Connect Cloud Integration';
/** Marker present in every page rendered by Docusaurus. */
const PAGE_MARKER = 'id=__docusaurus';
const PAGE_MARKER_QUOTED = 'id="__docusaurus"';
const SERVER_STARTUP_TIMEOUT_MS = 60_000;
const SERVER_POLL_INTERVAL_MS = 500;
const SERVER_SHUTDOWN_TIMEOUT_MS = 5_000;
const REQUEST_TIMEOUT_MS = 15_000;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Finds a free TCP port on the loopback interface.
 *
 * @returns {Promise<number>} the port
 */
const findFreePort = () =>
  new Promise((resolve, reject) => {
    const server = createServer();
    server.unref();
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const { port } = server.address();
      server.close(() => resolve(port));
    });
  });

/**
 * Reads the page paths from the sitemap of the build.
 *
 * @param {string} buildDir - the build directory
 * @returns {string[]} absolute URL paths, the shortest (home page) first
 */
const readSitemapPaths = (buildDir) => {
  const sitemap = readFileSync(join(buildDir, 'sitemap.xml'), 'utf8');
  const paths = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)].map(([, loc]) => new URL(loc).pathname);
  if (paths.length === 0) {
    throw new Error('No pages found in build/sitemap.xml.');
  }
  return [...new Set(paths)].sort((a, b) => a.length - b.length);
};

/**
 * Extracts script and stylesheet URLs from an HTML page.
 *
 * @param {string} html - the HTML source
 * @returns {string[]} the referenced asset paths
 */
const extractAssetPaths = (html) => {
  const attribute = (tag, name) => {
    const match = tag.match(new RegExp(`\\s${name}=(?:"([^"]+)"|'([^']+)'|([^\\s>]+))`));
    return match ? (match[1] ?? match[2] ?? match[3]) : undefined;
  };
  const assets = [];
  for (const [tag] of html.matchAll(/<(?:script|link)\b[^>]*>/g)) {
    if (tag.startsWith('<script')) {
      const src = attribute(tag, 'src');
      if (src) assets.push(src);
    } else if (attribute(tag, 'rel') === 'stylesheet') {
      const href = attribute(tag, 'href');
      if (href) assets.push(href);
    }
  }
  return assets.filter((asset) => asset.startsWith('/'));
};

/**
 * Requests a URL with a timeout.
 *
 * @param {string} url - the URL
 * @returns {Promise<{status: number, body: string}>} status and body
 */
const get = async (url) => {
  const response = await fetch(url, { signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS), redirect: 'follow' });
  return { status: response.status, body: await response.text() };
};

/**
 * Starts `docusaurus serve` in its own process group.
 *
 * @param {string} siteDir - the website directory
 * @param {number} port - the port to listen on
 * @returns {{child: import('node:child_process').ChildProcess, output: () => string}} the server
 */
const startServer = (siteDir, port) => {
  const cli = join(siteDir, 'node_modules', '@docusaurus', 'core', 'bin', 'docusaurus.mjs');
  if (!existsSync(cli)) {
    throw new Error(`Docusaurus CLI not found at ${cli}. Run "pnpm install" first.`);
  }
  const child = spawn(
    process.execPath,
    [cli, 'serve', '--no-open', '--host', '127.0.0.1', '--port', String(port)],
    { cwd: siteDir, detached: true, stdio: ['ignore', 'pipe', 'pipe'] },
  );
  let output = '';
  child.stdout.on('data', (data) => (output += data));
  child.stderr.on('data', (data) => (output += data));
  return { child, output: () => output };
};

/**
 * Terminates the server's whole process group, escalating to SIGKILL.
 *
 * @param {import('node:child_process').ChildProcess} child - the server process
 * @returns {Promise<void>} resolves when the server has exited
 */
const stopServer = async (child) => {
  if (child.exitCode !== null || child.signalCode !== null) return;
  const exited = new Promise((resolve) => child.once('exit', resolve));
  const signalGroup = (signal) => {
    try {
      process.kill(-child.pid, signal);
    } catch {
      // Process group already gone.
    }
  };
  signalGroup('SIGTERM');
  const timedOut = await Promise.race([exited.then(() => false), sleep(SERVER_SHUTDOWN_TIMEOUT_MS).then(() => true)]);
  if (timedOut) {
    signalGroup('SIGKILL');
    await exited;
  }
};

/**
 * Waits until the server answers on the base URL.
 *
 * @param {string} url - the base URL
 * @param {import('node:child_process').ChildProcess} child - the server process
 * @returns {Promise<boolean>} true if the server is ready
 */
const waitForServer = async (url, child) => {
  const deadline = Date.now() + SERVER_STARTUP_TIMEOUT_MS;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) return false;
    try {
      const { status } = await get(url);
      if (status === 200) return true;
    } catch {
      // Not ready yet.
    }
    await sleep(SERVER_POLL_INTERVAL_MS);
  }
  return false;
};

/**
 * Runs the smoke test against `build/` of the given website directory.
 *
 * @param {{siteDir?: string, log?: (message: string) => void}} [options] - options
 * @returns {Promise<{passed: boolean, failures: string[], checked: number}>} the result
 */
export const runSmokeTest = async ({ siteDir = process.cwd(), log = console.log } = {}) => {
  const buildDir = join(siteDir, 'build');
  if (!existsSync(join(buildDir, 'sitemap.xml'))) {
    return { passed: false, failures: ['build/sitemap.xml not found. Run "pnpm run build" first.'], checked: 0 };
  }

  const pagePaths = readSitemapPaths(buildDir);
  const homePath = pagePaths[0];
  const port = await findFreePort();
  const origin = `http://127.0.0.1:${port}`;
  const server = startServer(siteDir, port);
  const failures = [];
  let checked = 0;

  const cleanup = () => {
    try {
      process.kill(-server.child.pid, 'SIGKILL');
    } catch {
      // Already gone.
    }
  };
  process.once('exit', cleanup);

  try {
    log(`Smoke test: serving build/ at ${origin}${homePath}`);
    if (!(await waitForServer(`${origin}${homePath}`, server.child))) {
      failures.push(`Server did not become ready within ${SERVER_STARTUP_TIMEOUT_MS / 1000} s.`);
      return { passed: false, failures, checked };
    }

    let homeHtml = '';
    for (const path of pagePaths) {
      checked++;
      try {
        const { status, body } = await get(`${origin}${path}`);
        if (status !== 200) {
          failures.push(`${path}: HTTP ${status}`);
        } else if (!body.includes(PAGE_MARKER) && !body.includes(PAGE_MARKER_QUOTED)) {
          failures.push(`${path}: not a rendered Docusaurus page`);
        }
        if (path === homePath) homeHtml = body;
      } catch (error) {
        failures.push(`${path}: ${error.message}`);
      }
    }

    if (!homeHtml.includes(HOME_PAGE_MARKER)) {
      failures.push(`${homePath}: expected text "${HOME_PAGE_MARKER}" not found`);
    }

    const assetPaths = extractAssetPaths(homeHtml);
    if (assetPaths.length === 0) {
      failures.push(`${homePath}: no scripts or stylesheets found`);
    }
    for (const asset of assetPaths) {
      checked++;
      try {
        const { status } = await get(`${origin}${asset}`);
        if (status !== 200) failures.push(`${asset}: HTTP ${status}`);
      } catch (error) {
        failures.push(`${asset}: ${error.message}`);
      }
    }

    log(`Smoke test: checked ${pagePaths.length} pages and ${assetPaths.length} assets.`);
    return { passed: failures.length === 0, failures, checked };
  } finally {
    await stopServer(server.child);
    process.removeListener('exit', cleanup);
    if (failures.length > 0) {
      log('Server output:');
      log(server.output().trim() || '(none)');
    }
  }
};

const isMain = import.meta.url === pathToFileURL(process.argv[1] ?? '').href;
if (isMain) {
  const result = await runSmokeTest();
  if (result.passed) {
    console.log('Smoke test passed.');
  } else {
    console.error(`Smoke test failed (${result.failures.length} problem(s)):`);
    for (const failure of result.failures) console.error(`  - ${failure}`);
    process.exitCode = 1;
  }
}
