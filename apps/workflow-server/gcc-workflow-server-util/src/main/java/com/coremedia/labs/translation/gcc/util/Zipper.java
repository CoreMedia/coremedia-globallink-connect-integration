package com.coremedia.labs.translation.gcc.util;

import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * One-call zip and unzip convenience
 */
public final class Zipper {
  private static final Logger LOG = LoggerFactory.getLogger(Zipper.class);
  private static final String TMPFILE_PREFIX = "gcczipper";

  // static class
  private Zipper() {}


  // --- features ---------------------------------------------------

  /**
   * Extracts the zip file into a temp directory.
   * <p>
   * The invoker is responsible for deletion of the output directory.
   *
   * @param zipFile the zip file, URL or file path
   * @param prefix extract only entries that start with this prefix (optional)
   * @return the output directory
   */
  @NonNull
  public static File unzip(String zipFile, @Nullable String prefix) {
    File extractionDir = tempDir();
    try {
      unzip(extractionDir, new File(zipFile), prefix);
      return extractionDir;
    } catch (RuntimeException e) {
      forceDelete(extractionDir);
      throw e;
    }
  }

  /**
   * Extracts the zip file into the target directory.
   *
   * @param targetDir the target directory
   * @param zipFile the zip file, URL or file path
   * @param prefix extract only entries that start with this prefix (optional)
   */
  public static void unzip(File targetDir, String zipFile, @Nullable String prefix) {
    unzip(targetDir, new File(zipFile), prefix);
  }

  /**
   * Zip the source directory into the zip file.
   *
   * @param zipFile the zip file, writable URL or file path
   * @param srcDir the directory to be zipped
   * @param prefix include only entries that start with the prefix (optional)
   */
  public static void zip(String zipFile, File srcDir, @Nullable String prefix) {
    zip(new File(zipFile), srcDir, prefix);
  }


  // --- internal -------------------------------------------------

  private static void unzip(File targetDir, File zipFile, @Nullable String prefix) {
    try (ZipInputStream zipStream = new ZipInputStream(new FileInputStream(zipFile))) {
      mkDirs(targetDir);
      for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry()) {
        if (PathUtil.isReferringToParent(entry.getName())) {
          throw new IllegalArgumentException(zipFile.getAbsolutePath() + " exploits the Zip Slip Vulnerability. Extraction aborted.");
        }
        if (!entry.isDirectory() && (prefix==null || entry.getName().startsWith(prefix))) {
          File file = new File(targetDir, entry.getName());
          mkParentDirs(file);
          try (OutputStream fos = new FileOutputStream(file)) {
            ByteStreams.copy(zipStream, fos);
          }
        }
        zipStream.closeEntry();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot extract " + zipFile.getAbsolutePath(), e);
    }
  }

  private static void zip(File targetUrl, File srcDir, @Nullable String prefix) {
    try {
      File zipFile = Paths.get(targetUrl.toURI()).toFile();
      try (FileOutputStream fos = new FileOutputStream(zipFile);
           ZipOutputStream zos = new ZipOutputStream(fos)) {
        recZip(zos, srcDir, srcDir, zipFile, prefix);
        zos.finish();
        zos.flush();
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot write zip file to " + targetUrl.getAbsolutePath(), e);
    }
  }

  private static void recZip(ZipOutputStream zos, File zipRoot, File file, File theZipFileItself, String prefix) throws IOException {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          recZip(zos, zipRoot, child, theZipFileItself, prefix);
        }
      }
    } else if (!file.equals(theZipFileItself)) {
      String name = zipEntryName(zipRoot, file);
      if (prefix==null || name.startsWith(prefix)) {
        try (FileInputStream fis = new FileInputStream(file)) {
          zos.putNextEntry(new ZipEntry(name));
          IOUtils.copy(fis, zos);
        } finally {
          zos.closeEntry();
        }
      }
    }
  }

  /**
   * File to zip entry name.
   * <p>
   * If you can think of nicer implementation, be aware that it must also work
   * on Windows!
   */
  private static String zipEntryName(File zipRoot, File entryFile) {
    String relpath = entryFile.getAbsolutePath().substring(zipRoot.getAbsolutePath().length());
    if (relpath.startsWith(File.separator)) {
      relpath = relpath.substring(1);
    }
    return relpath.replace(File.separator, "/");
  }

  private static void mkParentDirs(File file) {
    File parent = file.getParentFile();
    if (parent != null) {
      mkDirs(parent);
    }
  }

  private static void mkDirs(File file) {
    if (file.exists()) {
      if (!file.isDirectory()) {
        throw new IllegalArgumentException("Cannot mkdirs " + file.getAbsolutePath());
      }
    } else {
      if (!file.mkdirs()) {
        throw new IllegalArgumentException("Cannot mkdirs " + file.getAbsolutePath());
      }
    }
  }

  @NonNull
  private static File tempDir() {
    try {
      return Files.createTempDirectory(TMPFILE_PREFIX).toFile();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create temp dir!", e);
    }
  }

  private static void forceDelete(File file) {
    try {
      FileUtils.forceDelete(file);
    } catch (IOException e) {
      LOG.warn("Cannot delete {}, please cleanup manually.", file.getAbsolutePath(), e);
    }
  }
}

