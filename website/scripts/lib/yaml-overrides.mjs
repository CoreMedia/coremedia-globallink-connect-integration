/**
 * Shared utilities for pnpm-workspace.yaml override manipulation.
 *
 * This module provides functions to read, write, add, and remove
 * override entries from pnpm-workspace.yaml without requiring an
 * external YAML library.
 *
 * @module yaml-overrides
 */

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { execSync } from 'node:child_process';

// ─── ANSI color codes ──────────────────────────────────────────────────────────

/** ANSI escape sequences for terminal coloring */
export const colors = {
  reset: '\x1b[0m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  dim: '\x1b[2m',
  bold: '\x1b[1m',
};

// ─── Shell utilities ───────────────────────────────────────────────────────────

/**
 * Execute a shell command and return trimmed output, or null on failure.
 *
 * @param {string} command - shell command to execute
 * @param {object} options - optional execSync options
 * @returns {string | null} trimmed stdout or null on failure
 */
export function execCommand(command, options = {}) {
  try {
    return execSync(command, {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'],
      ...options,
    }).trim();
  } catch {
    return null;
  }
}

/**
 * Run a shell command with live output, returning true on success.
 *
 * @param {string} label - human-readable label for the step
 * @param {string} command - shell command to execute
 * @returns {boolean} true if the command succeeded
 */
export function runStep(label, command) {
  console.log(`\n${colors.cyan}▶ ${label}${colors.reset}`);
  console.log(`  ${colors.dim}$ ${command}${colors.reset}`);
  try {
    execSync(command, { stdio: 'inherit' });
    console.log(`  ${colors.green}✅ ${label} passed${colors.reset}`);
    return true;
  } catch {
    console.log(`  ${colors.red}❌ ${label} failed${colors.reset}`);
    return false;
  }
}

// ─── Version comparison utilities ──────────────────────────────────────────────

/**
 * Parse a version string into a numeric triple [major, minor, patch].
 *
 * @param {string} v - version string like "6.14.0"
 * @returns {number[] | null} tuple [major, minor, patch] or null
 */
export function parseVersion(v) {
  const match = v.match(/(\d+)\.(\d+)\.(\d+)/);
  if (!match) return null;
  return [parseInt(match[1]), parseInt(match[2]), parseInt(match[3])];
}

/**
 * Compare two version strings.
 *
 * @param {string} a - first version
 * @param {string} b - second version
 * @returns {number} -1 if a < b, 0 if equal, 1 if a > b
 */
export function compareVersions(a, b) {
  const pa = parseVersion(a);
  const pb = parseVersion(b);
  if (!pa || !pb) return 0;
  for (let i = 0; i < 3; i++) {
    if (pa[i] < pb[i]) return -1;
    if (pa[i] > pb[i]) return 1;
  }
  return 0;
}

/**
 * Extract the minimum version from a range specifier.
 *
 * @param {string} range - a version range specifier
 * @returns {string | null} the minimum version string or null
 */
export function extractMinVersion(range) {
  const match = range.match(/(\d+\.\d+\.\d+)/);
  return match ? match[1] : null;
}

/**
 * Check whether a given version satisfies a dependency range specifier.
 *
 * Covers caret (^), tilde (~), exact, and >= patterns.
 *
 * @param {string} version - the concrete version to test
 * @param {string} range - the range specifier
 * @returns {boolean | 'unknown'} true/false or 'unknown' for complex ranges
 */
export function satisfiesRange(version, range) {
  const ver = parseVersion(version);
  if (!ver) return 'unknown';

  const trimmed = range.trim();

  if (/^\d+\.\d+\.\d+$/.test(trimmed)) {
    return compareVersions(version, trimmed) === 0;
  }

  const caretMatch = trimmed.match(/^\^(\d+)\.(\d+)\.(\d+)$/);
  if (caretMatch) {
    const major = parseInt(caretMatch[1]);
    const minor = parseInt(caretMatch[2]);
    const patch = parseInt(caretMatch[3]);
    if (major === 0 && minor === 0) {
      return ver[0] === 0 && ver[1] === 0 && ver[2] === patch;
    }
    if (major === 0) {
      return ver[0] === 0 && ver[1] === minor && ver[2] >= patch;
    }
    return ver[0] === major && compareVersions(version, `${major}.${minor}.${patch}`) >= 0;
  }

  const tildeMatch = trimmed.match(/^~(\d+)\.(\d+)\.(\d+)$/);
  if (tildeMatch) {
    const major = parseInt(tildeMatch[1]);
    const minor = parseInt(tildeMatch[2]);
    const patch = parseInt(tildeMatch[3]);
    return ver[0] === major && ver[1] === minor && ver[2] >= patch;
  }

  const gteMatch = trimmed.match(/^>=\s*(\d+\.\d+\.\d+)$/);
  if (gteMatch) {
    return compareVersions(version, gteMatch[1]) >= 0;
  }

  return 'unknown';
}

/**
 * Compute the exclusive upper bound of a common version range.
 *
 * @param {string} range - a version range like "^6.12.5", "~6.12.5"
 * @returns {string | null} the exclusive upper bound version string
 */
export function computeRangeUpper(range) {
  const trimmed = range.trim();

  if (/^\d+\.\d+\.\d+$/.test(trimmed)) {
    const v = parseVersion(trimmed);
    return v ? `${v[0]}.${v[1]}.${v[2] + 1}` : null;
  }

  const caretMatch = trimmed.match(/^\^(\d+)\.(\d+)\.(\d+)$/);
  if (caretMatch) {
    const major = parseInt(caretMatch[1]);
    if (major === 0) {
      const minor = parseInt(caretMatch[2]);
      if (minor === 0) return `0.0.${parseInt(caretMatch[3]) + 1}`;
      return `0.${minor + 1}.0`;
    }
    return `${major + 1}.0.0`;
  }

  const tildeMatch = trimmed.match(/^~(\d+)\.(\d+)\.\d+$/);
  if (tildeMatch) {
    return `${parseInt(tildeMatch[1])}.${parseInt(tildeMatch[2]) + 1}.0`;
  }

  return null;
}

// ─── YAML reading ──────────────────────────────────────────────────────────────

/**
 * Read the `overrides` map from pnpm-workspace.yaml.
 *
 * @param {string} filePath - absolute path to pnpm-workspace.yaml
 * @returns {Record<string, string> | null} the overrides map or null
 */
export function readWorkspaceYamlOverrides(filePath) {
  let content;
  try {
    content = readFileSync(filePath, 'utf8');
  } catch {
    return null;
  }

  const overrides = {};
  let inOverrides = false;

  for (const line of content.split('\n')) {
    if (/^overrides\s*:/.test(line)) {
      inOverrides = true;
      continue;
    }

    if (inOverrides) {
      if (line.trim() === '') continue;
      if (/^\S/.test(line)) {
        inOverrides = false;
        continue;
      }
      if (/^\s*#/.test(line)) continue;

      const quotedMatch = line.match(/^\s+(?:'([^']*)'|"([^"]*)")?\s*:\s*(.+)$/);
      if (quotedMatch && (quotedMatch[1] !== undefined || quotedMatch[2] !== undefined)) {
        const key = quotedMatch[1] ?? quotedMatch[2];
        overrides[key] = quotedMatch[3].trim();
        continue;
      }

      const unquotedMatch = line.match(/^\s+([\w@/.-][^:]*?)\s*:\s*(.+)$/);
      if (unquotedMatch) {
        overrides[unquotedMatch[1].trim()] = unquotedMatch[2].trim();
      }
    }
  }

  return Object.keys(overrides).length > 0 ? overrides : null;
}

// ─── YAML key extraction ───────────────────────────────────────────────────────

/**
 * Extract the override key from a YAML entry line.
 *
 * @param {string} line - a single YAML line from the overrides section
 * @returns {string | null} the override key, or null
 */
export function extractKeyFromYamlLine(line) {
  const quotedMatch = line.match(/^\s+(?:'([^']*)'|"([^"]*)")\s*:/);
  if (quotedMatch) return quotedMatch[1] ?? quotedMatch[2];

  const unquotedMatch = line.match(/^\s+([\w@/.-][^:]*?)\s*:/);
  if (unquotedMatch) return unquotedMatch[1].trim();

  return null;
}

/**
 * Extract package name from override key.
 *
 * @param {string} overrideKey - e.g., "ajv@>=8.0.0 <8.18.0" or "express"
 * @returns {string} the package name
 */
export function extractPackageName(overrideKey) {
  if (overrideKey.startsWith('@')) {
    const match = overrideKey.match(/^(@[^/]+\/[^@]+)/);
    return match ? match[1] : overrideKey;
  }
  return overrideKey.split('@')[0];
}

/**
 * Extract the version selector from a ranged override key.
 *
 * @param {string} overrideKey - the override key
 * @returns {string | null} the version selector, or null for unranged
 */
export function extractOverrideSelector(overrideKey) {
  const pkgName = extractPackageName(overrideKey);
  const rest = overrideKey.slice(pkgName.length);
  if (rest.startsWith('@')) {
    return rest.slice(1).trim() || null;
  }
  return null;
}

// ─── YAML manipulation ─────────────────────────────────────────────────────────

/**
 * Remove specific override entries from pnpm-workspace.yaml.
 *
 * @param {string} filePath - absolute path to pnpm-workspace.yaml
 * @param {string[]} keysToRemove - array of override keys to remove
 * @returns {{ removedCount: number, originalContent: string }}
 */
export function removeOverridesFromYaml(filePath, keysToRemove) {
  const originalContent = readFileSync(filePath, 'utf8');
  const lines = originalContent.split('\n');
  const result = [];
  let inOverrides = false;
  let overrideHeaderIndex = -1;
  let removedCount = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    if (/^overrides\s*:/.test(line)) {
      inOverrides = true;
      overrideHeaderIndex = result.length;
      result.push(line);
      continue;
    }

    if (inOverrides) {
      if (line.trim() === '') {
        result.push(line);
        continue;
      }
      if (/^\S/.test(line)) {
        inOverrides = false;
        result.push(line);
        continue;
      }
      if (/^\s*#/.test(line)) {
        result.push(line);
        continue;
      }

      const key = extractKeyFromYamlLine(line);
      if (key && keysToRemove.includes(key)) {
        while (
          result.length > 0 &&
          result.length > overrideHeaderIndex + 1 &&
          /^\s*#/.test(result[result.length - 1])
        ) {
          result.pop();
        }
        removedCount++;
        continue;
      }

      result.push(line);
    } else {
      result.push(line);
    }
  }

  // Remove empty overrides section
  if (overrideHeaderIndex >= 0) {
    let hasEntries = false;
    for (let i = overrideHeaderIndex + 1; i < result.length; i++) {
      const line = result[i];
      if (line.trim() === '') continue;
      if (/^\S/.test(line)) break;
      if (!/^\s*#/.test(line)) {
        hasEntries = true;
        break;
      }
    }
    if (!hasEntries) {
      let sectionEnd = overrideHeaderIndex + 1;
      while (sectionEnd < result.length) {
        const line = result[sectionEnd];
        if (line.trim() !== '' && /^\S/.test(line)) break;
        sectionEnd++;
      }
      result.splice(overrideHeaderIndex, sectionEnd - overrideHeaderIndex);
    }
  }

  writeFileSync(filePath, result.join('\n'));
  return { removedCount, originalContent };
}

/**
 * Add override entries to pnpm-workspace.yaml.
 *
 * If no `overrides:` section exists, one is created before the end of file.
 * Each entry can optionally have a comment line above it (e.g., advisory URL).
 *
 * @param {string} filePath - absolute path to pnpm-workspace.yaml
 * @param {Array<{ key: string, value: string, comment?: string }>} entries
 * @returns {{ addedCount: number, originalContent: string }}
 */
export function addOverridesToYaml(filePath, entries) {
  const originalContent = readFileSync(filePath, 'utf8');
  const lines = originalContent.split('\n');
  let addedCount = 0;

  // Find existing overrides section
  let overrideSectionEnd = -1;
  let overrideHeaderIndex = -1;
  let inOverrides = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (/^overrides\s*:/.test(line)) {
      inOverrides = true;
      overrideHeaderIndex = i;
      continue;
    }
    if (inOverrides) {
      if (/^\S/.test(line) && line.trim() !== '') {
        overrideSectionEnd = i;
        inOverrides = false;
        break;
      }
    }
  }

  // If still in overrides at end of file
  if (inOverrides) {
    overrideSectionEnd = lines.length;
  }

  // Build new lines for entries
  const newLines = [];
  for (const entry of entries) {
    // Determine if key needs quoting (contains @, spaces, or special chars)
    const needsQuoting = entry.key.includes('@') || entry.key.includes(' ');
    const keyStr = needsQuoting ? `'${entry.key}'` : entry.key;

    if (entry.comment) {
      newLines.push(`  # ${entry.comment}`);
    }
    newLines.push(`  ${keyStr}: ${entry.value}`);
    addedCount++;
  }

  if (overrideHeaderIndex >= 0) {
    // Insert before the end of the overrides section
    lines.splice(overrideSectionEnd, 0, ...newLines);
  } else {
    // No overrides section exists — create one at end of file
    const sectionLines = ['', 'overrides:', ...newLines];
    // Find last non-empty line
    let insertAt = lines.length;
    while (insertAt > 0 && lines[insertAt - 1].trim() === '') {
      insertAt--;
    }
    lines.splice(insertAt, lines.length - insertAt, ...sectionLines, '');
  }

  writeFileSync(filePath, lines.join('\n'));
  return { addedCount, originalContent };
}

/**
 * Restore a file to its previous content.
 *
 * @param {string} filePath - absolute path
 * @param {string} originalContent - the content to restore
 */
export function restoreYaml(filePath, originalContent) {
  writeFileSync(filePath, originalContent);
}
