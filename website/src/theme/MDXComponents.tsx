import DocLink from '../components/DocLink';

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
 * ```
 *
 * For more information about MDX components in Docusaurus:
 * @see https://docusaurus.io/docs/markdown-features/react#mdx-component-scope
 */
const MDXComponents = {
  // Make DocLink component globally available in all MDX files
  DocLink,
};

export default MDXComponents;
