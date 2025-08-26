import React from 'react';

/**
 * Props for the Badge component
 */
interface BadgeConfig {
  /** The left-side label text */
  label: string;
  /** Badge style (e.g., "for-the-badge", "flat", "plastic") */
  style: string;
  /** Badge color (hex code without #) */
  color: string;
  /** The right-side message text */
  message: string;
  /** Optional alternative text for accessibility */
  alt?: string;
}

/**
 * Badge Component
 *
 * Creates a shields.io static badge for displaying version information,
 * build status, or other metadata. The component is globally available
 * in all MDX files without requiring imports.
 *
 * Example usage in MDX files:
 * ```mdx
 * <Badge
 *   label="CoreMedia Content Cloud"
 *   style="for-the-badge"
 *   color="672779"
 *   message="2412.0"
 * />
 * ```
 *
 * @param config - The badge configuration
 * @returns An img element displaying the shields.io badge
 */
const Badge = (config: BadgeConfig) => {
  const {
    label,
    style,
    color,
    message,
    alt = 'shields.io Badge'
  } = config;
  const encodedLabel = encodeURIComponent(label);
  const encodedStyle = encodeURIComponent(style);
  const encodedColor = encodeURIComponent(color);
  const encodedMessage = encodeURIComponent(message);
  // Example: https://img.shields.io/static/v1?message=2412.0&label=CoreMedia%20Content%20Cloud&style=for-the-badge&color=672779
  const imgUrl = `https://img.shields.io/static/v1?message=${encodedMessage}&label=${encodedLabel}&style=${encodedStyle}&color=${encodedColor}`;
  return (
    <img alt={alt} src={imgUrl} />
  );
};

export default Badge;
export type { BadgeConfig };
