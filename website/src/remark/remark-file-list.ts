import { visit } from 'unist-util-visit';
import * as fs from 'fs';
import * as path from 'path';
import type { Plugin } from 'unified';
import type { Node } from 'unist';
import type { Root, Code } from 'mdast';
import { fromMarkdown } from 'mdast-util-from-markdown';
import logger from '@docusaurus/logger';

/**
 * Remark plugin to replace code blocks with `file-list` language
 * with a list of files from a specified directory.
 *
 * Example:
 *
 * ````markdown
 * ```file-list
 * directory=./files&exclude=licenses.xml
 * ```
 * ````
 *
 * **Note:** HTML files will not be processed as assets by Docusaurus
 * transformLinks plugin (by design). They will link directly to the raw files.
 * This is a known limitation of Docusaurus for HTML files.
 */
const remarkFileList: Plugin<[], Root> = () => {
  return (tree, file) => {
    visit(tree, 'code', (node: Code, index: number, parent: Node) => {
      if (node.lang === 'file-list') {
        const params = node.value;
        logger.info(`[File-List-Plugin] Found placeholder in ${file.path}`);

        const urlParams = new URLSearchParams(params);
        const directory = urlParams.get('directory');
        const exclude = urlParams.getAll('exclude');

        if (!directory) {
          logger.error('[File-List-Plugin] `directory` parameter is required.');
          return;
        }

        const currentDir = path.dirname(file.path as string);
        const targetDir = path.resolve(currentDir, directory);

        let files: string[];
        try {
          files = fs.readdirSync(targetDir);
        } catch (e) {
          logger.error(`[File-List-Plugin] Failed to read directory ${targetDir}: ${e.message}`);
          return;
        }

        const filteredFiles = files.filter(f => !exclude.includes(f));
        logger.info(`[File-List-Plugin] Found ${filteredFiles.length} files.`);

        // Create regular markdown links for all files
        // Note: HTML files won't be processed as assets by Docusaurus (by design)
        // We have an extra plugin copy-html-files that will take care of this
        // in production builds. Unfortunately, these files are not delivered
        // at the expected URL using `docusaurus serve`. But GitHub pages will
        // correctly link the files.
        const markdownList = filteredFiles
          .map(f => {
            const relativePath = `./${path.join(directory, f)}`;
            return `* [${f}](<${relativePath}>)`;
          })
          .join('\n');

        // Parse the markdown into AST
        const listAst = fromMarkdown(markdownList);

        // Replace the original code block with the new nodes
        if (listAst.children.length > 0) {
          (parent as any).children.splice(index, 1, ...listAst.children);
        } else {
          (parent as any).children.splice(index, 1);
        }
      }
    });
  };
};

export default remarkFileList;
