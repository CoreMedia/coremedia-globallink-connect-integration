package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import org.awaitility.Awaitility;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.GCFile;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.gs4tr.gcc.restclient.operation.ConnectorsConfig;
import org.gs4tr.gcc.restclient.operation.Content;
import org.gs4tr.gcc.restclient.request.PageableRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Contract test for GCC RestClient.
 * </p>
 * <p>
 * This is a test which should run on demand for example if you extended
 * the facade or if either the GCC Java API got updated or the corresponding
 * GCC REST Backend.
 * </p>
 * <p>
 * It is a so called contract test and thus tests the contract between
 * the consumer (this facade) and the producer (the GCC Java API).
 * </p>
 * <p>
 * In order to run the test, you need to add a file {@code .gcc.properties}
 * to your user home folder:
 * </p>
 * <pre>
 * username=JohnDoe
 * password=secret!
 * url=https://connect-dev.translations.com/api/v2/
 * key=0e...abc
 * fileType=xliff
 * </pre>
 */
@ExtendWith(GccCredentialsExtension.class)
class DefaultGCExchangeFacadeContractTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final String XML_CONTENT = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><test>Lorem Ipsum</test>";
  private static final String XLIFF_CONTENT_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
          "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" version=\"1.2\">\n" +
          "  <file original=\"someId\" source-language=\"%s\" datatype=\"xml\" target-language=\"%s\">\n" +
          "    <body>\n" +
          "      <trans-unit id=\"1\" datatype=\"plaintext\">\n" +
          "        <source>Lorem Ipsum</source>\n" +
          "        <target>Lorem Ipsum</target>\n" +
          "      </trans-unit>\n" +
          "    </body>\n" +
          "  </file>\n" +
          "</xliff>\n";
  private static final int TRANSLATION_TIMEOUT_MINUTES = 30;
  private static final int SUBMISSION_VALID_TIMEOUT_MINUTES = 2;

  @Test
  @DisplayName("Validate that login works.")
  void login(Map<String, Object> gccProperties) {
    LOG.info("Properties: {}", gccProperties);
    try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
      assertThat(facade.getDelegate()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Tests for available File Types")
  class FileTypes {
    @ParameterizedTest(name = "[{index}] Optional File Type {0} should be available.")
    @ValueSource(strings = {"xml"})
    @DisplayName("Ensure that optional file types are available.")
    void optionalFileTypesAvailable(String type, Map<String, Object> gccProperties) {
      // These file types are optional. They may be required for testing, but they are not
      // important for production usage.
      assertFileTypeAvailable(type, gccProperties);
    }

    @ParameterizedTest(name = "[{index}] Required File Type {0} should be available.")
    @ValueSource(strings = {"xliff"})
    @DisplayName("Ensure that required file types are available.")
    void requiredFileTypesAvailable(String type, Map<String, Object> gccProperties) {
      // These file types are crucial for this GCC client.
      assertFileTypeAvailable(type, gccProperties);
    }

    private void assertFileTypeAvailable(String type, Map<String, Object> gccProperties) {
      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        GCExchange delegate = facade.getDelegate();
        ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig = delegate.getConnectorsConfig();
        List<String> availableTypes = connectorsConfig.getFileTypes();
        assertThat(type).isIn(availableTypes);
      }
    }
  }

  @Nested
  @DisplayName("Tests for available Supported Locales")
  class SupportedLocales {
    @ParameterizedTest(name = "[{index}] Required Target Locale {0} should be available.")
    @ValueSource(strings = {"de-DE", "fr-FR"})
    @DisplayName("Ensure that target locales required by tests are available.")
    void requiredTargetLocalesAreAvailable(String expectedSupportedLocale, Map<String, Object> gccProperties) {
      assertSupportedLocaleAvailable(expectedSupportedLocale, lc -> !lc.getIsSource(), gccProperties);
    }

    @ParameterizedTest(name = "[{index}] Required Source Locale {0} should be available.")
    @ValueSource(strings = {"en-US"})
    @DisplayName("Ensure that source locales required by tests are available.")
    void requiredSourceLocalesAreAvailable(String expectedSupportedLocale, Map<String, Object> gccProperties) {
      assertSupportedLocaleAvailable(expectedSupportedLocale, LocaleConfig::getIsSource, gccProperties);
    }

    private void assertSupportedLocaleAvailable(String expectedSupportedLocale, Predicate<LocaleConfig> localeConfigPredicate, Map<String, Object> gccProperties) {
      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig = facade.getDelegate().getConnectorsConfig();
        List<Locale> supportedLocales = getSupportedLocaleStream(connectorsConfig, localeConfigPredicate).collect(Collectors.toList());
        Locale expected = Locale.forLanguageTag(expectedSupportedLocale);
        LOG.info("Available locales: {}", supportedLocales);
        assertThat(supportedLocales).anySatisfy(tl -> assertThat(tl).isEqualTo(expected));
      }
    }
  }

  @Nested
  @DisplayName("Test for content upload")
  class ContentUpload {
    @Test
    @DisplayName("Upload File.")
    void upload(TestInfo testInfo, Map<String, Object> gccProperties) {
      Instant startTimeUtc = Instant.now().atZone(ZoneOffset.UTC).toInstant();
      String fileName = testInfo.getDisplayName();

      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        GCExchange delegate = facade.getDelegate();

        long contentCountBefore = getTotalRecordsCount(delegate);
        String fileId = facade.uploadContent(fileName, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));

        assertThat(fileId).isNotEmpty();

        long contentCountAfter = getTotalRecordsCount(delegate);

        // Fail-early test: Ensure that we actually received any content.
        // Note, that we expect no latency here. If we experience that the
        // uploaded content is not immediately available, we have to introduce
        // a wait statement here (e. g. using Awaitility).
        assertThat(contentCountAfter).isGreaterThan(contentCountBefore);

        Content.ContentResponseData contentList = delegate.getContentList(new PageableRequest(1L, contentCountAfter));

        List<GCFile> filesWithToString = contentList.getResponseData().stream().map(f -> {
          GCFile spy = Mockito.spy(f);
          Mockito.when(spy.toString()).thenReturn(String.format("%s [id=%s, contentId=%s, type=%s, updated=%s]", f.getName(), f.getId(), f.getContentId(), f.getFileType(), f.getUpdatedAt()));
          return spy;
        }).collect(Collectors.toList());
        assertThat(filesWithToString).anySatisfy(
                f -> {
                  assertThat(f).extracting(GCFile::getContentId).isEqualTo(fileId);
                  assertThat(f.getUpdatedAt()).matches((Predicate<Date>) date -> date.toInstant().isAfter(startTimeUtc));
                }
        );
      }
    }

    long getTotalRecordsCount(GCExchange exchange) {
      Content.ContentResponseData contentList = exchange.getContentList();
      return contentList.getTotalRecordsCount();
    }
  }

  @Nested
  @DisplayName("Tests for cancellation")
  class Cancellation {
    @Test
    @DisplayName("Be aware of submission/task cancellation.")
    void shouldBeCancellationAware(TestInfo testInfo, Map<String, Object> gccProperties) {
      String testName = testInfo.getDisplayName();

      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        GCExchange delegate = facade.getDelegate();

        String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));
        long submissionId = facade.submitSubmission(
                testName,
                null,
                ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
                null,
                "admin",
                Locale.US, singletonMap(fileId, singletonList(Locale.GERMANY)));

        assertThat(submissionId).isGreaterThan(0L);

        // Yes, we need to wait here. Directly after being started, a submission state
        // may be 'null' (which we internally map to "other").
        Awaitility.await("Wait for submission to be valid (has some well-known state).")
                .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState())
                        .isNotIn(
                                GCSubmissionState.OTHER,
                                GCSubmissionState.IN_PRE_PROCESS
                        )
                );

        /*
         * If cancellation fails because of invalid state: Extend the forbidden
         * states in the Awaitility call above.
         */
        delegate.cancelSubmission(submissionId);

        Awaitility.await("Wait until submission is marked as cancelled.")
                .atMost(2, TimeUnit.MINUTES)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(GCSubmissionState.CANCELLED));

        Awaitility.await("Wait until cancellation got confirmed for submission.")
                .atMost(2, TimeUnit.MINUTES)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.SECONDS)
                .conditionEvaluationListener(condition -> {
                  // Some tasks may have already reached completed state.
                  // Thus simulate a successful download for them.
                  facade.downloadCompletedTasks(submissionId, new TrueTaskDataConsumer());
                  facade.confirmCancelledTasks(submissionId);
                })
                .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(GCSubmissionState.CANCELLATION_CONFIRMED));
      }
    }
  }

  private static class TrueTaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
    @Override
    public boolean test(InputStream inputStream, GCTaskModel task) {
      return true;
    }
  }

  @Nested
  @DisplayName("Test general content submission")
  class ContentSubmission {
    @Test
    @DisplayName("Test simple submission")
    void submitXml(TestInfo testInfo, Map<String, Object> gccProperties) {
      String testName = testInfo.getDisplayName();

      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));
        long submissionId = facade.submitSubmission(
                testName,
                null,
                ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
                null,
                "admin",
                Locale.US, singletonMap(fileId, singletonList(Locale.GERMANY)));

        assertThat(submissionId).isGreaterThan(0L);

        // Yes, we need to wait here. Directly after being started, a submission state
        // may be 'null' (which we internally map to "other").
        Awaitility.await("Wait for submission to be valid (has some well-known state).")
                .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isNotEqualTo(GCSubmissionState.OTHER));
      }
    }

    @Test
    @DisplayName("Tests dealing with submission name length restrictions (currently 150 chars): Mode: ASCII, skip additional information")
    void submissionNameTruncationAsciiSkipAdditionalInfo(TestInfo testInfo, Map<String, Object> gccProperties) {
      String testName = testInfo.getDisplayName();
      String submissionName = padEnd(testName, 150, 'a', 'z');

      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));
        long submissionId = facade.submitSubmission(
                submissionName,
                null,
                ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
                null,
                "admin",
                Locale.US, singletonMap(fileId, singletonList(Locale.GERMANY)));

        assertThat(submissionId).isGreaterThan(0L);
      }
    }

    @Test
    @DisplayName("Tests dealing with submission name length restrictions (currently 150 chars): Mode: ASCII, subject truncation")
    void submissionNameTruncationAscii(TestInfo testInfo, Map<String, Object> gccProperties) {
      String testName = testInfo.getDisplayName();
      String submissionName = padEnd(testName, 200, 'a', 'z');

      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));
        long submissionId = facade.submitSubmission(
                submissionName,
                null,
                ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
                null,
                "admin",
                Locale.US, singletonMap(fileId, singletonList(Locale.GERMANY)));

        assertThat(submissionId).isGreaterThan(0L);
      }
    }

    @Test
    @DisplayName("Tests dealing with submission name length restrictions (currently 150 chars): Mode: Unicode, skip additional information")
    void submissionNameTruncationUnicode(TestInfo testInfo, Map<String, Object> gccProperties) {
      String testName = testInfo.getDisplayName();
      // 2190..21FF Arrows
      String submissionName = padEnd(testName, 150, '\u2190', '\u21FF');

      try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
        String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));
        long submissionId = facade.submitSubmission(
                submissionName,
                null,
                ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
                null,
                "admin",
                Locale.US, singletonMap(fileId, singletonList(Locale.GERMANY)));

        assertThat(submissionId).isGreaterThan(0L);
      }
    }

    private String padEnd(String str, int minLength, char startChar, char endChar) {
      StringBuilder builder = new StringBuilder(str);
      char currentChar = startChar;
      while (builder.length() < minLength) {
        builder.append(currentChar);
        if (currentChar == endChar) {
          // Loop from beginning.
          currentChar = startChar;
        } else {
          currentChar++;
        }
      }
      return builder.toString();
    }
  }

  /**
   * This test addresses the full process of submitting files and receiving them
   * from GCC sandbox. The locales to use will be derived from the supported locales
   * at GCC. The test requires at minimum 2 supported locales and will fail otherwise.
   *
   * @param testInfo      test-info to generate names
   * @param gccProperties properties to log in
   */
  @Test
  @Tag("slow")
  @Tag("full")
  @DisplayName("Translate XLIFF and receive results (takes about 10 Minutes)")
  void translateXliff(TestInfo testInfo, Map<String, Object> gccProperties) {
    String testName = testInfo.getDisplayName();

    try (GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties)) {
      ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig = facade.getDelegate().getConnectorsConfig();
      List<Locale> targetLocales = getSupportedLocaleStream(connectorsConfig, lc -> !lc.getIsSource()).collect(Collectors.toList());
      Locale masterLocale = getSupportedLocaleStream(connectorsConfig, LocaleConfig::getIsSource)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("At least one source locale required."));

      if (targetLocales.isEmpty()) {
        throw new IllegalStateException("At least one target locale (non-source) required.");
      }

      Map<String, List<Locale>> contentMap = uploadContents(facade, testName, masterLocale, targetLocales);
      long submissionId = facade.submitSubmission(
              testName,
              null,
              ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
              null,
              "admin",
              masterLocale, contentMap);

      assertSubmissionReachesState(facade, submissionId, GCSubmissionState.COMPLETED, TRANSLATION_TIMEOUT_MINUTES);

      List<String> xliffResults = new ArrayList<>();

      facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResults));

      assertThat(xliffResults)
              .describedAs("All XLIFFs shall have been pseudo-translated.")
              .hasSize(targetLocales.size())
              .allSatisfy(s -> assertThat(s).doesNotContain("<target>Lorem Ipsum"));

      //After all tasks have been marked as delivered also the submission shall be marked as delivered.
      assertSubmissionReachesState(facade, submissionId, GCSubmissionState.DELIVERED, 5);
    }
  }

  private static class TaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
    private final List<String> xliffResults;

    private TaskDataConsumer(List<String> xliffResults) {
      this.xliffResults = xliffResults;
    }

    @Override
    public boolean test(InputStream is, GCTaskModel task) {
      ByteSource byteSource = new ByteSource() {
        @Override
        public InputStream openStream() {
          return is;
        }
      };
      try {
        StringBuilder xliffResult = new StringBuilder();
        byteSource.asCharSource(StandardCharsets.UTF_8).copyTo(xliffResult);
        xliffResults.add(xliffResult.toString());
      } catch (IOException e) {
        return false;
      }
      return true;
    }
  }

  /**
   * Retrieves all supported locales matching the given predicate.
   * Note, that the implementation uses {@link LocaleConfig#getLocaleLabel()} which
   * is expected to be a language-tag. Mapping might need to be changed, if this
   * shall be the PD locale or connector locale instead.
   *
   * @param connectorsConfig      the answer from connectors config
   * @param localeConfigPredicate predicate to apply
   * @return stream of matching locales; uses {@link LocaleConfig#getLocaleLabel()} for transformation
   */
  private static Stream<Locale> getSupportedLocaleStream(ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig, Predicate<LocaleConfig> localeConfigPredicate) {
    return connectorsConfig.getSupportedLocales().stream()
            .filter(localeConfigPredicate)
            .map(LocaleConfig::getLocaleLabel)
            // GCC REST Backend Bug Workaround: Locale contains/may contain trailing space.
            .map(String::trim)
            .map(Locale::forLanguageTag);
  }

  private static Map<String, List<Locale>> uploadContents(GCExchangeFacade facade, String testName, Locale masterLocale, List<Locale> targetLocales) {
    ImmutableMap.Builder<String, List<Locale>> contentMapBuilder = ImmutableMap.builder();

    for (Locale targetLocale : targetLocales) {
      String xliffContent = String.format(XLIFF_CONTENT_PATTERN, masterLocale.toLanguageTag(), targetLocale.toLanguageTag());
      String fileName = String.format("%s_%s2%s.xliff", testName, masterLocale.toLanguageTag(), targetLocale.toLanguageTag());
      String fileId = facade.uploadContent(fileName, new ByteArrayResource(xliffContent.getBytes(StandardCharsets.UTF_8)));
      contentMapBuilder.put(fileId, Collections.singletonList(targetLocale));
    }

    return contentMapBuilder.build();
  }

  private static void assertSubmissionReachesState(GCExchangeFacade facade, long submissionId, GCSubmissionState stateToReach, int timeout) {
    Awaitility.await("Wait for translation to complete.")
            .atMost(timeout, TimeUnit.MINUTES)
            .pollDelay(1, TimeUnit.MINUTES)
            .pollInterval(1, TimeUnit.MINUTES)
            .conditionEvaluationListener(condition -> LOG.info("Submission {}, Current State: {}, elapsed time in seconds: {}", submissionId, facade.getSubmission(submissionId).getState(), condition.getElapsedTimeInMS() / 1000L))
            .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(stateToReach));
  }
}
