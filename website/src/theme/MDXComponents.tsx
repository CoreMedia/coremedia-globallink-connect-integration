import DocLink from '../components/DocLink';
import Deprecated from '../components/Deprecated';
import Since from '../components/Since';
import RepositoryLink from '../components/RepositoryLink';
import OriginalMDXComponents from '@theme-original/MDXComponents';

/**
 * MDX Components Configuration
 *
 * This file defines global components that are automatically available
 * in all MDX files without requiring explicit imports. This follows
 * Docusaurus's recommended approach for making frequently-used components
 * available across the entire documentation site.
 *
 * Components defined here can be used directly in any .mdx file:
 *
 * ```mdx
 * <DocLink path="workflow-developer-en/content/WorkflowVariables.html">
 *   Workflow Manual / Workflow Variables
 * </DocLink>
 *
 * <Since value="2506.0.0-1"/>
 *
 * <Deprecated value="2506.0.0-1"/>
 *
 * <RepositoryLink path="README.md" title="Repository README" />
 * ```
 *
 * For more information about MDX components in Docusaurus:
 * @see https://docusaurus.io/docs/markdown-features/react#mdx-component-scope
 */
const MDXComponents = {
  ...OriginalMDXComponents, // Include original MDX components provided by Docusaurus
  // Make components globally available in all MDX files
  DocLink,
  Deprecated,
  Since,
  RepositoryLink,
};

export default MDXComponents;
