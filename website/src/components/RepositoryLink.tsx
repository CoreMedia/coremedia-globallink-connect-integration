import React from 'react';
import { context } from '../ts/context';

/**
 * Props for the RepositoryLink component
 */
interface RepositoryLinkConfig {
  /**
   * The path to the file in the repository, relative to the repository root.
   * Example: "README.md", "apps/studio-server/pom.xml"
   */
  path: string;
  /**
   * Optional title/text for the link. If not provided, defaults to the path.
   */
  title?: string;
}

/**
 * RepositoryLink Component
 *
 * A reusable component that generates URLs to files within the project repository
 * using the repository context. This ensures that all repository links automatically
 * point to the correct repository and version without hardcoding URLs.
 *
 * Example usage in MDX files:
 * ```mdx
 * <RepositoryLink path="README.md" />
 * <RepositoryLink path="apps/studio-server/pom.xml" title="Studio Server POM" />
 * ```
 *
 * @param config - The component configuration
 * @returns A link element pointing to the repository file
 */
const RepositoryLink = (config: RepositoryLinkConfig) => {
  const { path, title = config.path } = config;
  return (
    <a href={context.git.repository.resolve(path)} target="_blank" rel="noopener noreferrer">
      {title}
    </a>
  );
};

export default RepositoryLink;
export type { RepositoryLinkConfig };
