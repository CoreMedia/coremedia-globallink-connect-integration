package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.labs.translation.gcc.util.Zipper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DownloadFromGlobalLinkActionUnitTest {
  private static final Logger LOG = LoggerFactory.getLogger(DownloadFromGlobalLinkActionUnitTest.class);

  private File existingZipFile;

  @BeforeEach
  void setUp() {
    try {
      existingZipFile = Files.createTempFile("ccbwtgdfglaut", ".zip").toFile();
      try (InputStream is = DownloadFromGlobalLinkActionUnitTest.class.getResourceAsStream("/com/coremedia/labs/translation/gcc/workflow/existing.zip");
           OutputStream os = new FileOutputStream(existingZipFile)) {
        IOUtils.copy(is, os);
      }
    } catch (IOException e) {
      assumeTrue(false, "Cannot setup DownloadFromGlobalLinkActionUnitTest: " + e.getMessage());
    }
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.forceDelete(existingZipFile);
  }


  // --- Tests ------------------------------------------------------

  @Test
  void testNoDir() throws IOException {
    File workingDir = tempDir();
    try {
      File actual = DownloadFromGlobalLinkAction.zipXliffs(createResult(workingDir));
      assertNull(actual);
    } finally {
      deleteFile(workingDir);
    }
  }

  @Test
  void testNoEntries() throws IOException {
    File workingDir = tempDir();
    try {
      File newXliffs = new File(workingDir, DownloadFromGlobalLinkAction.NEWXLIFFS);
      assumeTrue(newXliffs.mkdir());
      File actual = DownloadFromGlobalLinkAction.zipXliffs(createResult(workingDir));
      assertNull(actual);
    } finally {
      deleteFile(workingDir);
    }
  }

  @Test
  void testNewEntries() throws IOException {
    // Prepare
    File workingDir = tempDir();
    try {
      prepareNewFiles(workingDir);

      // Test
      File actual = DownloadFromGlobalLinkAction.zipXliffs(createResult(workingDir));

      // Check
      try {
        assertNotNull(actual);
        File extracted = new File(Zipper.unzip(actual.getAbsolutePath(), null), "xliff_issue_details");
        try {
          File[] files = extracted.listFiles();
          assertNotNull(files);
          assertEquals(2, files.length);
          // Check files by well known length, which is sufficiently unique in this test.
          assertEquals(5L, Objects.requireNonNull(extracted.listFiles((dir, name) -> "zipentry2.txt".equals(name)))[0].length());
          assertEquals(6L, Objects.requireNonNull(extracted.listFiles((dir, name) -> "zipentry3.txt".equals(name)))[0].length());
        } finally {
          deleteFile(extracted);
        }
      } finally {
        deleteFile(actual);
      }
    } finally {
      deleteFile(workingDir);
    }
  }


  // --- internal ---------------------------------------------------

  private static DownloadFromGlobalLinkAction.Result createResult(File workingDir) {
    return new DownloadFromGlobalLinkAction.Result(workingDir);
  }

  private static void prepareNewFiles(File workingDir) {
    try {
      File newXliffs = new File(workingDir, DownloadFromGlobalLinkAction.NEWXLIFFS);
      assumeTrue(newXliffs.mkdir());
      copyResourceToDirectory("zipentry2.txt", newXliffs, 5L);
      copyResourceToDirectory("zipentry3.txt", newXliffs, 6L);
    } catch (IOException e) {
      assumeTrue(false, "Cannot prepare working directory: " + e.getMessage());
    }
  }

  private static void copyResourceToDirectory(String resourceName, File targetDir, long length) throws IOException {
    try (InputStream is = DownloadFromGlobalLinkActionUnitTest.class.getResourceAsStream("/com/coremedia/labs/translation/gcc/workflow/" + resourceName)) {
      if (is == null) {
        throw new IllegalStateException("Cannot find resource: " + resourceName);
      }
      long result = Files.copy(is, new File(targetDir, resourceName).toPath());
      if (length >= 0L) {
        assumeTrue(result == length);
      }
    }
  }

  private static void deleteFile(File file) {
    try {
      if (file != null) {
        FileUtils.forceDelete(file);
      }
    } catch (IOException e) {
      LOG.warn("Cannot delete {}, please cleanup manually.", file.getAbsolutePath(), e);
    }
  }

  private static File tempDir() {
    try {
      return Files.createTempDirectory("ccbwtgcc").toFile();
    } catch (IOException e) {
      assumeTrue(false, "Cannot create temp dir: " + e.getMessage());
      return null;  // Unreachable, just to make the compiler happy
    }
  }
}
