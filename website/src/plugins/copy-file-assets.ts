import {promises as fs, existsSync, mkdirSync, readdirSync, copyFileSync, statSync} from 'fs';
import * as path from 'path';
import logger from '@docusaurus/logger';
import type {LoadContext, Plugin} from '@docusaurus/types';

const PluginName = 'copy-file-assets-plugin';

interface PluginOptions {
  paths: string[];
  fileExtensions?: string[];
}

/**
 * This plugin recursively searches for asset files (by default HTML files) within
 * 'files/' subdirectories in specified paths and copies them to the build directory.
 * This is a workaround for the limitation of the Docusaurus Remark plugin
 * `transformLinks` that by intention ignores certain files, including `*.html` files.
 * 
 * @param paths - Array of paths to search for 'files/' subdirectories (e.g., ['docs', 'dev'])
 * @param fileExtensions - Array of file extensions to copy (defaults to ['.html'])
 */
export default function copyFileAssetsPlugin(
  context: LoadContext,
  options: PluginOptions = { paths: ['docs'] }
): Plugin<void> {
  const { paths, fileExtensions = ['.html'] } = options;
  
  return {
    name: PluginName,
    async postBuild(props) {
      const {siteDir, outDir} = props;

      logger.info(`[${PluginName}] Searching for asset files in paths: ${paths.join(', ')}`);
      
      let totalCopied = 0;

      for (const searchPath of paths) {
        const basePath = path.join(siteDir, searchPath);
        
        if (!existsSync(basePath)) {
          logger.warn(`[${PluginName}] Search path not found: ${searchPath}`);
          continue;
        }

        // Find all 'files/' directories recursively
        const filesDirs = findFilesDirsRecursively(basePath);
        
        for (const filesDir of filesDirs) {
          // Calculate relative path from the search base to maintain directory structure
          const relativePath = path.relative(basePath, filesDir);
          const destDir = path.join(outDir, relativePath);
          
          const copiedCount = copyAssetsFromDirectory(filesDir, destDir, fileExtensions, outDir);
          totalCopied += copiedCount;
        }
      }

      if (totalCopied > 0) {
        logger.success(`[${PluginName}] Successfully copied ${totalCopied} asset files.`);
      } else {
        logger.info(`[${PluginName}] No asset files found to copy.`);
      }
    },
  };
}

/**
 * Recursively find all directories named 'files' within a base directory
 */
function findFilesDirsRecursively(baseDir: string): string[] {
  const filesDirs: string[] = [];
  
  function searchDirectory(currentDir: string) {
    try {
      const entries = readdirSync(currentDir, { withFileTypes: true });
      
      for (const entry of entries) {
        const fullPath = path.join(currentDir, entry.name);
        
        if (entry.isDirectory()) {
          if (entry.name === 'files') {
            filesDirs.push(fullPath);
          }
          // Continue searching recursively
          searchDirectory(fullPath);
        }
      }
    } catch (error) {
      logger.warn(`[${PluginName}] Could not read directory ${currentDir}: ${(error as Error).message}`);
    }
  }
  
  searchDirectory(baseDir);
  return filesDirs;
}

/**
 * Copy asset files from source directory to destination directory recursively
 */
function copyAssetsFromDirectory(
  sourceDir: string, 
  destDir: string, 
  fileExtensions: string[],
  buildRoot: string
): number {
  let copiedCount = 0;
  
  function copyRecursively(srcDir: string, dstDir: string) {
    if (!existsSync(srcDir)) return;
    
    // Create destination directory if it doesn't exist
    mkdirSync(dstDir, { recursive: true });
    
    const entries = readdirSync(srcDir, { withFileTypes: true });
    
    for (const entry of entries) {
      const srcPath = path.join(srcDir, entry.name);
      const dstPath = path.join(dstDir, entry.name);
      
      if (entry.isDirectory()) {
        // Recursively copy subdirectories
        copyRecursively(srcPath, dstPath);
      } else if (entry.isFile()) {
        const ext = path.extname(entry.name).toLowerCase();
        
        if (fileExtensions.includes(ext)) {
          try {
            copyFileSync(srcPath, dstPath);
            const relativeDest = path.relative(buildRoot, dstPath);
            logger.info(`[${PluginName}] ✓ Copied: ${entry.name} to ${path.dirname(relativeDest)}/`);
            copiedCount++;
          } catch (error) {
            logger.error(`[${PluginName}] ✗ Failed to copy ${entry.name}: ${(error as Error).message}`);
          }
        }
      }
    }
  }
  
  copyRecursively(sourceDir, destDir);
  return copiedCount;
}
