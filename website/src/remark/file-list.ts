import { visit } from 'unist-util-visit';
import * as fs from 'fs';
import * as path from 'path';
import type { Plugin } from 'unified';
import type { Root } from 'mdast';
import type { Node } from 'unist';
import { fromMarkdown } from 'mdast-util-from-markdown';
import logger from '@docusaurus/logger';

/**
 * Remark plugin to process FileList MDX components and replace them with markdown.
 *
 * This plugin detects <FileList> MDX components and replaces them with markdown
 * file listings that Docusaurus can properly process for asset URLs and linking.
 *
 * Example usage in MDX:
 * ```mdx
 * <FileList directory="./files" exclude={["licenses.xml"]} />
 * ```
 *
 * **Note:** HTML files will not be processed as assets by Docusaurus
 * transformLinks plugin (by design).
 */
const remarkFileList: Plugin<[], Root> = () => {
  return (tree, file) => {
    // Handle MDX FileList components by replacing them with markdown
    visit(tree, 'mdxJsxFlowElement', (node: any, index: number, parent: Node) => {
      if (node.name === 'FileList') {
        logger.info(`[File-List-Plugin] Found FileList component in ${file.path}`);

        // Extract props from the JSX element
        const directoryAttr = node.attributes?.find((attr: any) => attr.name === 'directory');
        const excludeAttr = node.attributes?.find((attr: any) => attr.name === 'exclude');

        if (!directoryAttr?.value) {
          logger.error('[File-List-Plugin] FileList component requires directory prop.');
          return;
        }

        const directory = directoryAttr.value;

        // Handle exclude prop - it might come as different types from JSX parsing
        let exclude: string[] = [];
        if (excludeAttr?.value) {
          // Check if it's a JSX expression with data.estree (compiled JavaScript expression)
          if (excludeAttr.value && typeof excludeAttr.value === 'object' && excludeAttr.value.data?.estree) {
            // Extract from the compiled AST
            const estree = excludeAttr.value.data.estree;
            if (estree.body?.[0]?.expression?.type === 'ArrayExpression') {
              exclude = estree.body[0].expression.elements
                .filter((el: any) => el?.type === 'Literal' && typeof el.value === 'string')
                .map((el: any) => el.value);
            }
          } else if (Array.isArray(excludeAttr.value)) {
            exclude = excludeAttr.value;
          } else if (typeof excludeAttr.value === 'string') {
            // If it's a single string, wrap it in an array
            exclude = [excludeAttr.value];
          } else {
            logger.warn(`[File-List-Plugin] Invalid exclude prop type: ${typeof excludeAttr.value}. Expected array or string.`);
          }
        }

        // Generate markdown file list
        const markdownList = generateMarkdownFileList(file.path as string, directory, exclude);

        if (markdownList) {
          // Parse the markdown into AST
          const listAst = fromMarkdown(markdownList);

          // Replace the FileList component with the markdown nodes
          if (listAst.children.length > 0) {
            (parent as any).children.splice(index, 1, ...listAst.children);
          } else {
            (parent as any).children.splice(index, 1);
          }
        }
      }
    });
  };
};

/**
 * Generate markdown file list with proper links that Docusaurus can process
 */
function generateMarkdownFileList(filePath: string, directory: string, exclude: string[]): string | null {
  const currentDir = path.dirname(filePath);
  const targetDir = path.resolve(currentDir, directory);

  let files: string[];
  try {
    files = fs.readdirSync(targetDir);
  } catch (e) {
    logger.error(`[File-List-Plugin] Failed to read directory ${targetDir}: ${e.message}`);
    return null;
  }

  const filteredFiles = files.filter(f => exclude.indexOf(f) === -1);

  if (filteredFiles.length === 0) {
    logger.warn(`[File-List-Plugin] No files found in ${targetDir} after filtering.`);
    return null;
  } else {
    logger.info(`[File-List-Plugin] Found ${filteredFiles.length} files.`);
  }

  let containsHtml = false;

  // Create regular markdown links for all files
  // This allows Docusaurus to process them properly for asset URLs
  const markdownList = filteredFiles
    .map(f => {
      if (f.endsWith('.html')) {
        containsHtml = true;
        return `* ${f} \\*)`;
      }
      const relativePath = `./${path.join(directory, f)}`;
      return `* [${f}](<${relativePath}>)`;
    });

  // Add Disclaimer as Admonition, if HTML files are contained:
  if (containsHtml) {
    const disclaimerMarkdown = `

_\\*) Links to HTML files cannot be provided due to technical limitations.
Check the GitHub repository instead for these files._
`;
    markdownList.push(disclaimerMarkdown);
  }

  return markdownList.join('\n');
}

export default remarkFileList;
