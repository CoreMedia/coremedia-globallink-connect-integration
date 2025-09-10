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
      <file original="someId" source-language="{0}" datatype="xml" target-language="{1}}">
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

  public UploadResult upload(GCExchangeFacade facade) {
    String fileId = facade.uploadContent(
      filename,
      new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)),
      sourceLocale
    );
    return new UploadResult(fileId, sourceLocale, targetLocale);
  }

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
    return ZonedDateTime.of(LocalDateTime.now().plusDays(2L), ZoneId.systemDefault());
  }

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

  public static XliffFixture of(String ownerId, Map.Entry<Locale, Locale> direction) {
    return of(ownerId, direction.getKey(), direction.getValue());
  }

  public record UploadResult(
    String fileId,
    Locale sourceLocale,
    Locale targetLocale
  ) {
  }
}
