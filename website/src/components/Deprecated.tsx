import React from 'react';

/**
 * Props for the Badge component
 */
interface DeprecatedConfig {
  /** The right-side message text */
  value: string;
}

/**
 * Shields.io requires to escape some characters within `badgeContent`.
 *
 * Quoting from [Static Badges](https://shields.io/badges):
 *
 * | URL input               | Badge output   |
 * |-------------------------|----------------|
 * | Underscore `_` or `%20` | Space ` `      |
 * | Double underscore `__`  | Underscore `_` |
 * | Double dash `--`        | Dash `-`       |
 *
 * Note, that using this escaping requires to label color to be given in
 * `badgeContent` rather than as parameter `labelColor`.
 *
 * @param value message to escape
 */
const escapeMessage = (value: string) => {
  value = value.replace(/_/g, '__');
  value = value.replace(/-/g, '--');
  value = value.replace(/\s/g, '_');
  return value;
};

const label = "deprecated_since";
const logo = "semanticrelease";
const color = "dc3545";
const staticBadgeBaseUrl = "https://img.shields.io/badge";

/**
 * Deprecated Component
 *
 * Creates a shields.io static badge for displaying `@deprecated` information.
 *
 * Example usage in MDX files:
 * ```mdx
 * <Deprecated
 *   value="2506.0.0-1"
 * />
 * ```
 *
 * @param config - the badge configuration
 * @returns an `<img>` element displaying the shields.io badge
 */
const Deprecated = (config: DeprecatedConfig) => {
  const { value } = config;
  const encodedValue = escapeMessage(value);
  const alt = `deprecated since version ${value}`;
  const badgeContent = `${label}-${encodedValue}-${color}`;
  // Example: https://img.shields.io/static/v1?message=2412.0&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779
  const imgUrl = `${staticBadgeBaseUrl}/${badgeContent}?logo=${logo}`;
  return (
    <img className="badge badge-version badge-deprecated" alt={alt} src={imgUrl} />
  );
};

export default Deprecated;
export type { DeprecatedConfig };
