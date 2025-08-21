import React from 'react';

interface BadgeConfig {
  label: string;
  style: string;
  color: string;
  message: string;
  alt?: string;
}

/**
 * Create a shields.io static badge.
 *
 * Usage example:
 *
 * ```
 * <Badge
 *   label="CoreMedia Content Cloud"
 *   style="for-the-badge"
 *   color="672779"
 *   message="2412.0"
 * />
 * ```
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
