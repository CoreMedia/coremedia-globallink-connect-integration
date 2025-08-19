import {promises as fs, existsSync, mkdirSync, readdirSync, copyFileSync} from 'fs';
import * as path from 'path';
import logger from '@docusaurus/logger';
import type {LoadContext, Plugin} from '@docusaurus/types';

const PluginName = 'copy-html-files-plugin';

interface CopyPath {
  source: string;
  dest: string;
}

/**
 * This plugin copies HTML files from the docs directory to the build directory
 * after the build process. This is a workaround for the limitation of the
 * Docusaurus Remark plugin `transformLinks` that by intention ignores certain
 * files, including `*.html` files.
 */
export default function copyHtmlFilesPlugin(
  context: LoadContext,
): Plugin<void> {
  return {
    name: PluginName,
    async postBuild(props) {
      const {siteDir, outDir} = props;

      // Define source and destination paths
      const copyPaths: CopyPath[] = [
        {
          source: path.join(siteDir, 'docs/third-party/files'),
          dest: path.join(outDir, 'third-party/files')
        }
      ];

      logger.info(`[${PluginName}] Copying HTML files...`);

      for (const {source, dest} of copyPaths) {
        if (existsSync(source)) {
          // Create destination directory if it doesn't exist
          mkdirSync(dest, { recursive: true });

          // Read all files in source directory
          const files = readdirSync(source);

          // Copy only HTML files
          const htmlFiles = files.filter(file => file.endsWith('.html'));

          htmlFiles.forEach(file => {
            const sourcePath = path.join(source, file);
            const destPath = path.join(dest, file);

            try {
              copyFileSync(sourcePath, destPath);
              logger.info(`[${PluginName}] ✓ Copied: ${file} to ${path.relative(outDir, dest)}/`);
            } catch (error) {
              logger.error(`[${PluginName}] ✗ Failed to copy ${file}: ${(error as Error).message}`);
            }
          });

          if (htmlFiles.length === 0) {
            logger.warn(`[${PluginName}] No HTML files found in ${path.relative(siteDir, source)}`);
          }
        } else {
          logger.warn(`[${PluginName}] Source directory not found: ${path.relative(siteDir, source)}`);
        }
      }

      logger.success(`[${PluginName}] HTML file copying completed.`);
    },
  };
}
