import React from 'react';
import { context } from '../ts/context';

/**
 * Props for the DocLink component
 */
interface DocLinkProps {
  /**
   * The path to the documentation page, relative to the webhelp directory.
   * Example: "workflow-developer-en/content/WorkflowVariables.html"
   */
  path: string;
  /**
   * The link text to display. Should be descriptive for accessibility.
   */
  children: React.ReactNode;
  /**
   * Optional title attribute for additional context on hover
   */
  title?: string;
}

/**
 * DocLink Component
 *
 * A reusable component that generates URLs to CoreMedia documentation
 * using version information from the application context. This ensures
 * that all documentation links automatically point to the correct version
 * without requiring manual updates across multiple files.
 *
 * The component is globally available in all MDX files without requiring imports.
 *
 * The generated URL follows this pattern:
 * https://documentation.coremedia.com/cmcc-{main}/artifacts/{major}.{minor}/webhelp/{path}
 *
 * Example usage in MDX files:
 * ```mdx
 * <DocLink path="workflow-developer-en/content/WorkflowVariables.html">
 *   Workflow Manual / Workflow Variables
 * </DocLink>
 * ```
 *
 * @param props - The component props
 * @returns A link element pointing to the CoreMedia documentation
 */
export default function DocLink({ path, children, title }: DocLinkProps): React.ReactElement {
  // Build the documentation URL using version context
  const docUrl = `https://documentation.coremedia.com/cmcc-${context.cmcc.version.main}/artifacts/${context.cmcc.version.major}.${context.cmcc.version.minor}/webhelp/${path}`;

  return (
    <a
      href={docUrl}
      target="_blank"
      rel="noopener noreferrer"
      title={title}
    >
      {children}
    </a>
  );
}
