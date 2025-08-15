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
 * **Restriction:** Due to Docusaurus (or webpack) limitations, this plugin may
 * not work as expected with certain file types, such as HTML files. For HTML
 * files, as defensive option, no link will be rendered as it will not correctly
 * resolve to an asset path.
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

        // 1. Build a raw Markdown string for the list
        const markdownList = filteredFiles
          .map(f => `* ${f.endsWith(".html") ? f : `[${f}](<./${path.join(directory, f)}>)`}`)
          .join('\n');

        // 2. Parse the string into a valid AST
        const listAst = fromMarkdown(markdownList);

        // 3. Replace the original code block with the new nodes
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
