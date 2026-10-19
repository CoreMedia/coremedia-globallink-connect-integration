/**
 * Comment-preserving access to `pnpm-workspace.yaml` and `.github/dependabot.yml`,
 * plus snapshots of the files a maintenance run may change.
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { isMap, isScalar, isSeq, parse, parseDocument } from 'yaml';

const TO_STRING_OPTIONS = { lineWidth: 0 };

/** Returns the value of a scalar node or a plain value. */
const scalarValue = (node) => (isScalar(node) ? node.value : node);

/** Returns the key of a map pair as a string; keys may be nodes or plain values. */
const keyName = (pair) => String(scalarValue(pair.key));

/**
 * Wraps `pnpm-workspace.yaml`. Call {@link WorkspaceFile#save} to persist changes.
 */
export class WorkspaceFile {
  /** @param {string} path - path to `pnpm-workspace.yaml` */
  constructor(path) {
    this.path = path;
    this.doc = parseDocument(readFileSync(path, 'utf8'));
  }

  /** Saves the file with top-level keys sorted, as pnpm writes them. */
  save() {
    if (isMap(this.doc.contents)) {
      this.doc.contents.items.sort((a, b) => keyName(a).localeCompare(keyName(b)));
    }
    writeFileSync(this.path, this.doc.toString(TO_STRING_OPTIONS));
  }

  /** @returns {Record<string, string>} the overrides */
  overrides() {
    const map = this.doc.get('overrides');
    return isMap(map) ? Object.fromEntries(map.items.map((pair) => [keyName(pair), String(scalarValue(pair.value))])) : {};
  }

  /**
   * Removes all overrides except the given keys.
   *
   * @param {Set<string>} keep - override keys to keep
   * @returns {string[]} the removed keys
   */
  removeOverridesExcept(keep) {
    const map = this.doc.get('overrides');
    if (!isMap(map)) return [];
    const removed = map.items.filter((pair) => !keep.has(keyName(pair))).map(keyName);
    map.items = map.items.filter((pair) => keep.has(keyName(pair)));
    if (map.items.length === 0) this.doc.delete('overrides');
    return removed;
  }

  /**
   * Adds a comment line above overrides that have none.
   *
   * @param {(key: string) => string | undefined} commentFor - returns the comment for a key
   */
  annotateOverrides(commentFor) {
    const map = this.doc.get('overrides');
    if (!isMap(map)) return;
    for (const pair of map.items) {
      const comment = commentFor(keyName(pair));
      if (comment && isScalar(pair.key) && !pair.key.commentBefore) pair.key.commentBefore = ` ${comment}`;
    }
  }

  /** @returns {string[]} the ignored advisory IDs */
  ignoredGhsas() {
    const seq = this.doc.getIn(['auditConfig', 'ignoreGhsas']);
    return isSeq(seq) ? seq.items.map((item) => String(item.value ?? item)) : [];
  }

  /** @param {string[]} ghsas - the advisory IDs to ignore */
  setIgnoredGhsas(ghsas) {
    const unique = [...new Set(ghsas)].sort();
    if (unique.length === 0) {
      // deleteIn() throws if auditConfig is missing, e.g., after the last block was removed.
      const auditConfig = this.doc.get('auditConfig');
      if (!isMap(auditConfig)) return;
      auditConfig.delete('ignoreGhsas');
      if (auditConfig.items.length === 0) this.doc.delete('auditConfig');
      return;
    }
    this.doc.setIn(['auditConfig', 'ignoreGhsas'], this.doc.createNode(unique));
  }

  /** @returns {Record<string, string>} the default catalog */
  catalog() {
    const map = this.doc.getIn(['catalogs', 'default']);
    return isMap(map) ? Object.fromEntries(map.items.map((pair) => [keyName(pair), String(scalarValue(pair.value))])) : {};
  }

  /**
   * @param {string} name - the package name
   * @param {string} range - the version range
   */
  setCatalogEntry(name, range) {
    this.doc.setIn(['catalogs', 'default', name], range);
  }

  /** @returns {number} the configured minimum release age in minutes */
  minimumReleaseAgeMinutes() {
    return Number(this.doc.get('minimumReleaseAge') ?? 0);
  }
}

/**
 * Replaces the ignore rules that mirror blocked advisories in the npm
 * section of `dependabot.yml` for the given directory.
 *
 * @param {string} path - path to `dependabot.yml`
 * @param {string} directory - the Dependabot `directory`, e.g. `/website`
 * @param {Set<string>} managedNames - dependency names whose rules are managed (old and new blocks)
 * @param {{name: string, versions: string, comment: string}[]} rules - the rules to write
 */
export const syncDependabotIgnores = (path, directory, managedNames, rules) => {
  const doc = parseDocument(readFileSync(path, 'utf8'));
  const updates = doc.get('updates');
  const entry = isSeq(updates)
    ? updates.items.find((item) => item.get('package-ecosystem') === 'npm' && item.get('directory') === directory)
    : undefined;
  if (!entry) throw new Error(`No npm update entry for directory ${directory} in ${path}.`);

  let ignore = entry.get('ignore');
  if (!isSeq(ignore)) {
    ignore = doc.createNode([]);
    entry.set('ignore', ignore);
  }
  ignore.items = ignore.items.filter((item) => !managedNames.has(String(item.get('dependency-name'))));
  for (const rule of rules) {
    const node = doc.createNode({ 'dependency-name': rule.name, versions: [rule.versions] });
    node.get('versions', true).flow = true;
    node.commentBefore = ` ${rule.comment}`;
    ignore.items.push(node);
  }
  if (ignore.items.length === 0) entry.delete('ignore');
  writeFileSync(path, doc.toString(TO_STRING_OPTIONS));
};

/**
 * Captures file contents so they can be restored later.
 *
 * @param {string[]} paths - the files
 * @returns {Map<string, string>} the snapshot
 */
export const snapshot = (paths) => new Map(paths.map((path) => [path, readFileSync(path, 'utf8')]));

/**
 * Restores files from a snapshot.
 *
 * @param {Map<string, string>} saved - the snapshot
 */
export const restore = (saved) => {
  for (const [path, content] of saved) writeFileSync(path, content);
};

/**
 * Tells whether any file differs from a snapshot.
 *
 * @param {Map<string, string>} saved - the snapshot
 * @returns {boolean} true if a file changed
 */
export const changedSince = (saved) => [...saved].some(([path, content]) => readFileSync(path, 'utf8') !== content);

/**
 * Lists the resolved packages of a `pnpm-lock.yaml`.
 *
 * @param {string} content - the lockfile content
 * @returns {Map<string, Set<string>>} the versions by package name
 */
const lockfilePackages = (content) => {
  const versions = new Map();
  for (const key of Object.keys(parse(content)?.packages ?? {})) {
    const at = key.lastIndexOf('@');
    if (at <= 0) continue;
    const name = key.slice(0, at);
    if (!versions.has(name)) versions.set(name, new Set());
    versions.get(name).add(key.slice(at + 1));
  }
  return versions;
};

/**
 * Compares the resolved packages of two lockfile versions.
 *
 * @param {string} before - the previous lockfile content
 * @param {string} after - the current lockfile content
 * @returns {{name: string, before: string[], after: string[]}[]} the changed packages, sorted by name
 */
export const lockfileChanges = (before, after) => {
  const previous = lockfilePackages(before);
  const current = lockfilePackages(after);
  const names = [...new Set([...previous.keys(), ...current.keys()])].sort();
  const sorted = (set) => [...(set ?? [])].sort((a, b) => a.localeCompare(b, 'en', { numeric: true }));
  return names
    .map((name) => ({ name, before: sorted(previous.get(name)), after: sorted(current.get(name)) }))
    .filter((change) => change.before.join() !== change.after.join());
};
