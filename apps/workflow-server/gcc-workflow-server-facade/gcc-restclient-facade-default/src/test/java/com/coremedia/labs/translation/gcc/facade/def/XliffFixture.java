package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test fixture for creating and managing XLIFF files in GlobalLink Connect
 * (GCC) integration tests.
 * <p>
 * This record encapsulates the necessary data for generating XLIFF 1.2
 * compliant translation files and provides convenient methods for uploading and
 * submitting them to the GCC facade. The fixture generates standardized XLIFF
 * content with Lorem Ipsum text for consistent testing scenarios.
 *
 * @param ownerId      the unique identifier for the content owner
 * @param filename     the generated filename following the pattern
 *                     {@code <ownerId>_<sourceLocale>2<targetLocale>.xliff}
 * @param content      the complete XLIFF document content as UTF-8 string
 * @param sourceLocale the source language locale for translation
 * @param targetLocale the target language locale for translation
 */
@NullMarked
public record XliffFixture(
  String ownerId,
  String filename,
  String content,
  Locale sourceLocale,
  Locale targetLocale
) {
  private static final String XLIFF_CONTENT_PATTERN = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
      <file original="someId" source-language="{0}" datatype="xml" target-language="{1}">
        <body>
          <trans-unit id="1" datatype="plaintext">
            <source>Lorem Ipsum: {2}</source>
            <target>Lorem Ipsum: {2}</target>
          </trans-unit>
        </body>
      </file>
    </xliff>
    """;
  private static final String FILENAME_PATTERN = "{2}_{0}2{1}.xliff";

  /**
   * Uploads the XLIFF content to the GCC facade without submitting for
   * translation.
   *
   * @param facade the GCC exchange facade for content operations
   * @return upload result containing file ID and locale information
   * @throws RuntimeException if upload fails due to network or API errors
   */
  public UploadResult upload(GCExchangeFacade facade) {
    String fileId = facade.uploadContent(
      filename,
      new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)),
      sourceLocale
    );
    return new UploadResult(fileId, sourceLocale, targetLocale);
  }

  /**
   * Uploads the XLIFF content and immediately submits it for translation
   * processing.
   * <p>
   * This method combines upload and submission operations in a single call,
   * setting a due date two days from the current system time to accommodate
   * GCC API timezone handling limitations.
   *
   * @param facade         the GCC exchange facade for content operations
   * @param submissionName the human-readable name for the translation
   *                       submission
   * @param comment        optional comment describing the submission purpose
   * @param submitterName  the name of the user initiating the submission
   * @return the unique submission ID for tracking translation progress
   * @throws RuntimeException if upload or submission fails
   */
  public long uploadAndSubmit(GCExchangeFacade facade,
                              String submissionName,
                              String comment,
                              String submitterName) {
    UploadResult uploadResult = upload(facade);
    return facade.submitSubmission(
      submissionName,
      comment,
      getSomeDueDate(),
      null,
      submitterName,
      uploadResult.sourceLocale(),
      Map.of(uploadResult.fileId(), List.of(uploadResult.targetLocale()))
    );
  }

  /**
   * Batch uploads multiple XLIFF fixtures and submits them as a single
   * translation job.
   * <p>
   * This static method enables efficient processing of multiple translation
   * files within a single submission, reducing API overhead and providing
   * atomic submission semantics for related content.
   *
   * @param facade         the GCC exchange facade for content operations
   * @param submissionName the human-readable name for the batch submission
   * @param comment        optional comment describing the batch submission
   *                       purpose
   * @param submitterName  the name of the user initiating the batch submission
   * @param sourceLocale   the common source locale for all fixtures in the
   *                       batch
   * @param fixtures       the collection of XLIFF fixtures to upload and submit
   * @return the unique submission ID for tracking the batch translation
   * progress
   * @throws RuntimeException if any upload or the submission fails
   */
  public static long uploadAndSubmitAll(
    GCExchangeFacade facade,
    String submissionName,
    String comment,
    String submitterName,
    Locale sourceLocale,
    Collection<XliffFixture> fixtures
  ) {
    Map<String, List<Locale>> contentMap = fixtures.stream().map(f -> f.upload(facade))
      .map(r -> Map.entry(r.fileId(), List.of(r.targetLocale())))
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue
      ));
    return facade.submitSubmission(
      submissionName,
      comment,
      getSomeDueDate(),
      null,
      submitterName,
      sourceLocale,
      contentMap
    );
  }

  /**
   * Get some due date for testing. Due to an issue within the GCC Java
   * REST Client API (v3.1.3) ignoring UTC time-zone requirement, we should
   * ensure, that the offset is not just some hours, but rather days.
   *
   * @return some due date for testing
   */
  private static ZonedDateTime getSomeDueDate() {
    return ZonedDateTime.of(LocalDateTime.now(ZoneId.systemDefault()).plusDays(2L), ZoneId.systemDefault());
  }

  /**
   * Factory method for creating an XLIFF fixture with generated content and
   * filename.
   * <p>
   * This method generates standardized XLIFF 1.2 content using the provided
   * locale information and owner ID. The resulting fixture contains properly
   * formatted language tags and consistent Lorem Ipsum text for predictable
   * test scenarios.
   *
   * @param ownerId      the unique identifier to embed in the XLIFF content and
   *                     filename
   * @param sourceLocale the source language locale for the translation pair
   * @param targetLocale the target language locale for the translation pair
   * @return a new XLIFF fixture with generated content and filename
   */
  public static XliffFixture of(String ownerId, Locale sourceLocale, Locale targetLocale) {
    String xliffContent = MessageFormat.format(
      XLIFF_CONTENT_PATTERN,
      sourceLocale.toLanguageTag(),
      targetLocale.toLanguageTag(),
      ownerId
    );
    String filename = MessageFormat.format(FILENAME_PATTERN,
      sourceLocale.toLanguageTag(),
      targetLocale.toLanguageTag(),
      ownerId
    );
    return new XliffFixture(
      ownerId,
      filename,
      xliffContent,
      sourceLocale,
      targetLocale
    );
  }

  /**
   * Convenience factory method for creating an XLIFF fixture from a locale
   * direction entry.
   * <p>
   * This overload accepts a {@link Map.Entry} representing a translation
   * direction, where the key is the source locale and the value is the target
   * locale.
   *
   * @param ownerId   the unique identifier to embed in the XLIFF content and
   *                  filename
   * @param direction the translation direction as a key-value pair of locales
   * @return a new XLIFF fixture with generated content and filename
   */
  public static XliffFixture of(String ownerId, Map.Entry<Locale, Locale> direction) {
    return of(ownerId, direction.getKey(), direction.getValue());
  }

  /**
   * Result record capturing the outcome of an XLIFF file upload operation.
   * <p>
   * This record provides the necessary information for subsequent submission
   * operations and locale tracking within the GCC workflow.
   *
   * @param fileId       the unique identifier assigned by GCC for the uploaded
   *                     file
   * @param sourceLocale the source language locale from the original fixture
   * @param targetLocale the target language locale from the original fixture
   */
  public record UploadResult(
    String fileId,
    Locale sourceLocale,
    Locale targetLocale
  ) {
  }
}
