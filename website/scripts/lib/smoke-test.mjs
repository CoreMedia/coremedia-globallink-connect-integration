/**
 * Smoke Test Module for the Docusaurus website.
 *
 * Starts a local server from the `build/` directory, checks a configurable
 * list of pages for HTTP 200 responses and expected content, then shuts down.
 *
 * Design: cleanly separated so the page list can grow over time as new
 * issues are discovered. Each entry specifies a URL path and a string
 * that must appear in the response body.
 *
 * @module smoke-test
 */

import { execSync, spawn } from 'node:child_process';
import { colors } from './yaml-overrides.mjs';

/**
 * Pages to verify after build.
 *
 * Each entry defines:
 * - `path`: URL path relative to the base URL
 * - `expectedContent`: a string that must appear in the HTML response body
 * - `description`: human-readable label for reporting
 *
 * Extend this list when new critical pages or content markers are identified.
 */
const SMOKE_TEST_PAGES = [
  {
    path: '/',
    expectedContent: 'CoreMedia GlobalLink Connect Cloud Integration',
    description: 'Homepage renders with expected heading',
  },
];

/** Base URL used by `docusaurus serve` */
const BASE_URL = 'http://localhost:3000/coremedia-globallink-connect-integration';

/** Maximum time (ms) to wait for the server to become ready */
const SERVER_STARTUP_TIMEOUT = 30_000;

/** Interval (ms) between server readiness checks */
const POLL_INTERVAL = 500;

/**
 * Wait for a URL to respond with HTTP 200.
 *
 * @param {string} url - the URL to poll
 * @param {number} timeoutMs - maximum wait time
 * @returns {Promise<boolean>} true if server responded before timeout
 */
async function waitForServer(url, timeoutMs) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const response = await fetch(url);
      if (response.ok) return true;
    } catch {
      // Server not ready yet
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL));
  }
  return false;
}

/**
 * Run smoke tests against the built website.
 *
 * Starts `pnpm serve` (which serves the `build/` directory),
 * verifies each page in the smoke test list, then terminates the server.
 *
 * @returns {Promise<{ passed: boolean, results: Array<{ page: string, passed: boolean, error?: string }> }>}
 */
export async function runSmokeTests() {
  const results = [];

  console.log(`\n${colors.cyan}▶ Smoke test: starting server...${colors.reset}`);

  // Start the serve process
  const serverProcess = spawn('pnpm', ['exec', 'docusaurus', 'serve', '--no-open'], {
    stdio: ['pipe', 'pipe', 'pipe'],
    detached: false,
    cwd: process.cwd(),
  });

  let serverOutput = '';
  serverProcess.stdout?.on('data', (data) => {
    serverOutput += data.toString();
  });
  serverProcess.stderr?.on('data', (data) => {
    serverOutput += data.toString();
  });

  try {
    // Wait for server readiness
    const ready = await waitForServer(BASE_URL, SERVER_STARTUP_TIMEOUT);
    if (!ready) {
      results.push({
        page: '(server startup)',
        passed: false,
        error: `Server did not start within ${SERVER_STARTUP_TIMEOUT / 1000}s`,
      });
      return { passed: false, results };
    }

    console.log(`  ${colors.green}Server ready at ${BASE_URL}${colors.reset}`);

    // Test each page
    for (const page of SMOKE_TEST_PAGES) {
      const url = `${BASE_URL}${page.path}`;
      try {
        const response = await fetch(url);
        if (!response.ok) {
          results.push({
            page: page.path,
            passed: false,
            error: `HTTP ${response.status} (expected 200)`,
          });
          continue;
        }

        const body = await response.text();
        if (!body.includes(page.expectedContent)) {
          results.push({
            page: page.path,
            passed: false,
            error: `Expected content not found: "${page.expectedContent.slice(0, 60)}..."`,
          });
          continue;
        }

        results.push({ page: page.path, passed: true });
        console.log(
          `  ${colors.green}✅${colors.reset} ${page.description} ${colors.dim}(${page.path})${colors.reset}`,
        );
      } catch (fetchError) {
        results.push({
          page: page.path,
          passed: false,
          error: `Fetch error: ${fetchError.message}`,
        });
      }
    }
  } finally {
    // Kill server process
    try {
      serverProcess.kill('SIGTERM');
    } catch {
      // Already terminated
    }
  }

  const passed = results.every((r) => r.passed);
  if (!passed) {
    console.log(`  ${colors.red}❌ Smoke test failed:${colors.reset}`);
    results
      .filter((r) => !r.passed)
      .forEach((r) => {
        console.log(`     ${r.page}: ${r.error}`);
      });
  } else {
    console.log(`  ${colors.green}✅ All ${results.length} smoke test(s) passed${colors.reset}`);
  }

  return { passed, results };
}
