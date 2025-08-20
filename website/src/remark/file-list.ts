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
 * transformLinks plugin (by design).
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

        if (filteredFiles.length === 0) {
          logger.warn(`[File-List-Plugin] No files found in ${targetDir} after filtering.`);
          return;
        } else {
          logger.info(`[File-List-Plugin] Found ${filteredFiles.length} files.`);
        }

        let containsHtml = false;

        // Create regular markdown links for all files
        //
        // Note: HTML files won't be processed as assets by Docusaurus
        // (by design). Only exception here are those in `static/` folder, which
        // we cannot use, as (at least for licenses) we need to have them as
        // versioned artifacts.
        //
        // After several attempts to trick Docusaurus into processing these
        // files giving up, as we found no good option that works with
        // versioned documentation.
        //
        // See Also:
        // * <https://docusaurus.io/feature-requests/p/support-html-files-as-file-assets-in-docs>
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

        // Parse the markdown into AST
        const listAst = fromMarkdown(markdownList.join('\n'));

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
