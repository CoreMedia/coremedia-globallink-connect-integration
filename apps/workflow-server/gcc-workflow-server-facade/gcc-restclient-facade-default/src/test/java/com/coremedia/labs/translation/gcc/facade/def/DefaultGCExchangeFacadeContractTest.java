package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeAccessException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConnectorKeyConfigException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.facade.config.CharacterType;
import com.coremedia.labs.translation.gcc.facade.config.GCSubmissionInstruction;
import com.coremedia.labs.translation.gcc.facade.config.GCSubmissionName;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.model.GCFile;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.gs4tr.gcc.restclient.operation.ConnectorsConfig;
import org.gs4tr.gcc.restclient.operation.Content;
import org.gs4tr.gcc.restclient.request.PageableRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Contract test for GCC RestClient.
 * </p>
 * <p>
 * This is a test which should run on demand for example if you extended
 * the facade or if either the GCC Java API got updated, or the corresponding
 * GCC REST Backend.
 * </p>
 * <p>
 * It is a so-called contract test and thus tests the contract between
 * the consumer (this facade) and the producer (the GCC Java API).
 * </p>
 * <p>
 * In order to run the test, you need to add a file {@code .gcc.properties}
 * to your user home folder:
 * </p>
 * <pre>{@code
 * apiKey=ab12cd34
 * url=https://connect-dev.translations.com/api/v3/
 * key=0e...abc
 * fileType=xliff
 * }</pre>
 */
@ExtendWith(GccCredentialsExtension.class)
class DefaultGCExchangeFacadeContractTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final String XML_CONTENT = """
    <?xml version="1.0" encoding="utf-8" standalone="yes"?><test>Lorem Ipsum</test>""";
  private static final String XLIFF_CONTENT_PATTERN = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
      <file original="someId" source-language="%s" datatype="xml" target-language="%s">
        <body>
          <trans-unit id="1" datatype="plaintext">
            <source>Lorem Ipsum</source>
            <target>Lorem Ipsum</target>
          </trans-unit>
        </body>
      </file>
    </xliff>
    """;
  private static final long TRANSLATION_TIMEOUT_MINUTES = 30L;
  private static final long SUBMISSION_VALID_TIMEOUT_MINUTES = 2L;
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  /**
   * An ID to make the submissions easier to identify within the GCC backend.
   * The included timestamp is meant to ease the identification of a given
   * test run.
   */
  private static final String TEST_ID = "CT#%s".formatted(LocalDateTime.now().format(DATE_TIME_FORMATTER));
  private String submissionName;
  private String testName;

  @BeforeEach
  void setUp(@NonNull TestInfo testInfo) {
    testName = testInfo.getTestMethod().map(Method::getName).orElse("noname");
    submissionName = "%s: %s".formatted(TEST_ID, testName);
  }

  @AfterAll
  static void afterAll() {
    LOG.info("Contract test finished for ID '{}'.", TEST_ID);
  }

  @Nested
  @DisplayName("Tests for login")
  class Login {
    @Test
    @DisplayName("Validate that login works.")
    void shouldLoginSuccessfully(@NonNull Map<String, Object> gccProperties) {
      LOG.info("Properties: {}", gccProperties);
      assertThatCode(() -> new DefaultGCExchangeFacade(gccProperties))
        .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validate that invalid login is denied.")
    void shouldFailToLoginWithInvalidApiKey(@NonNull Map<String, Object> gccProperties) {
      Map<String, Object> patchedProperties = new HashMap<>(gccProperties);
      patchedProperties.put("apiKey", "invalid");
      LOG.info("Properties: {} patched to {}", gccProperties, patchedProperties);
      assertThatCode(() -> new DefaultGCExchangeFacade(patchedProperties))
        .isInstanceOf(GCFacadeAccessException.class)
        .hasCauseInstanceOf(IllegalAccessError.class);
    }

    /**
     * We cannot trust GCC to validate the connector key in every situation.
     * To prevent unexpected, hard to handle results, that do not expose an
     * issue with the connector key (such as when trying to retrieve a
     * submission by ID), we validate the connector key initially instead.
     */
    @Test
    void shouldValidateConnectorKeyInitially(@NonNull Map<String, Object> gccProperties) {
      Map<String, Object> patchedProperties = new HashMap<>(gccProperties);
      patchedProperties.put("key", "invalid");
      LOG.info("Properties: {} patched to {}", gccProperties, patchedProperties);
      assertThatCode(() -> new DefaultGCExchangeFacade(patchedProperties))
        .isInstanceOf(GCFacadeConnectorKeyConfigException.class)
        .hasNoCause();
    }
  }

  @Nested
  @DisplayName("Tests for available File Types")
  class FileTypes {
    @ParameterizedTest(name = "[{index}] Optional File Type {0} should be available.")
    @ValueSource(strings = "xml")
    @DisplayName("Ensure that optional file types are available.")
    void optionalFileTypesAvailable(String type, Map<String, Object> gccProperties) {
      // These file types are optional. They may be required for testing, but they are not
      // important for production usage.
      assertFileTypeAvailable(type, gccProperties);
    }

    @ParameterizedTest(name = "[{index}] Required File Type {0} should be available.")
    @ValueSource(strings = "xliff")
    @DisplayName("Ensure that required file types are available.")
    void requiredFileTypesAvailable(String type, Map<String, Object> gccProperties) {
      // These file types are crucial for this GCC client.
      assertFileTypeAvailable(type, gccProperties);
    }

    private static void assertFileTypeAvailable(String type, Map<String, Object> gccProperties) {
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      GCExchange delegate = facade.getDelegate();
      ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig = delegate.getConnectorsConfig();
      List<String> availableTypes = connectorsConfig.getFileTypes();
      assertThat(type).isIn(availableTypes);
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
    @ValueSource(strings = "en-US")
    @DisplayName("Ensure that source locales required by tests are available.")
    void requiredSourceLocalesAreAvailable(String expectedSupportedLocale, Map<String, Object> gccProperties) {
      assertSupportedLocaleAvailable(expectedSupportedLocale, LocaleConfig::getIsSource, gccProperties);
    }

    private static void assertSupportedLocaleAvailable(String expectedSupportedLocale, Predicate<LocaleConfig> localeConfigPredicate, Map<String, Object> gccProperties) {
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig = facade.getDelegate().getConnectorsConfig();
      List<Locale> supportedLocales = getSupportedLocaleStream(connectorsConfig, localeConfigPredicate).toList();
      Locale expected = Locale.forLanguageTag(expectedSupportedLocale);
      LOG.info("Available locales: {}", supportedLocales.stream().map(Locale::toLanguageTag).toList());
      assertThat(supportedLocales).anySatisfy(tl -> assertThat(tl).isEqualTo(expected));
    }
  }

  @Nested
  @DisplayName("Test for content upload")
  class ContentUpload {
    @Test
    @DisplayName("Upload File.")
    void upload(@NonNull Map<String, Object> gccProperties) {
      Instant startTimeUtc = Instant.now().atZone(ZoneOffset.UTC).toInstant();

      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      GCExchange delegate = facade.getDelegate();

      long contentCountBefore = getTotalRecordsCount(delegate);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);

      assertThat(fileId).isNotEmpty();

      long contentCountAfter = getTotalRecordsCount(delegate);

      // Fail-early test: Ensure that we actually received any content.
      // Note, that we expect no latency here. If we experience that the
      // uploaded content is not immediately available, we have to introduce
      // a wait statement here (e.g., using Awaitility).
      assertThat(contentCountAfter).isGreaterThan(contentCountBefore);

      Content.ContentResponseData contentList = delegate.getContentList(new PageableRequest(1L, contentCountAfter));

      List<GCFile> filesWithToString = contentList.getResponseData().stream().map(f -> {
        GCFile spy = Mockito.spy(f);
        Mockito.when(spy.toString()).thenReturn(String.format("%s [id=%s, contentId=%s, type=%s, updated=%s]", f.getName(), f.getId(), f.getContentId(), f.getFileType(), f.getUpdatedAt()));
        return spy;
      }).toList();
      assertThat(filesWithToString).anySatisfy(
        f -> {
          assertThat(f).extracting(GCFile::getContentId).isEqualTo(fileId);
          assertThat(f.getUpdatedAt()).matches(date -> date.toInstant().isAfter(startTimeUtc));
        }
      );
    }

    static long getTotalRecordsCount(GCExchange exchange) {
      Content.ContentResponseData contentList = exchange.getContentList();
      return contentList.getTotalRecordsCount();
    }
  }

  @Nested
  @DisplayName("Tests for cancellation")
  class Cancellation {
    @Test
    @DisplayName("Be aware of submission/task cancellation.")
    void shouldBeCancellationAware(@NonNull Map<String, Object> gccProperties) {
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      GCExchange delegate = facade.getDelegate();

      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      long submissionId = facade.submitSubmission(
        submissionName,
        "Submission is meant to be cancelled via API and later confirmed.",
        getSomeDueDate(),
        null,
        testName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      // Yes, we need to wait here. Directly after being started, a submission state
      // may be 'null' (which we internally map to "other").
      await("Wait for submission to be valid (has some well-known state).")
        .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .pollDelay(1L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
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
      int status = facade.cancelSubmission(submissionId);

      assertThat(status)
        .as("Cancellation should have been successful.")
        .isEqualTo(HTTP_OK);

      await("Wait until submission is marked as cancelled.")
        .atMost(2L, TimeUnit.MINUTES)
        .pollDelay(5L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(GCSubmissionState.CANCELLED));

      await("Wait until cancellation got confirmed for submission.")
        .atMost(2L, TimeUnit.MINUTES)
        .pollDelay(10L, TimeUnit.SECONDS)
        .pollInterval(20L, TimeUnit.SECONDS)
        .conditionEvaluationListener(condition -> {
          try {
            // Some tasks may have already reached completed state.
            // Thus simulate a successful download for them.
            facade.downloadCompletedTasks(submissionId, new TrueTaskDataConsumer());
            facade.confirmCancelledTasks(submissionId);
          } catch (GCFacadeCommunicationException e) {
            LOG.info("Ignoring communication exception. Rather trying again later.", e);
          }
        })
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(GCSubmissionState.CANCELLATION_CONFIRMED));
    }
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
    void submitXml(@NonNull Map<String, Object> gccProperties) {
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      long submissionId = facade.submitSubmission(
        submissionName,
        null,
        getSomeDueDate(),
        null,
        "admin",
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      // Yes, we need to wait here. Directly after being started, a submission state
      // may be 'null' (which we internally map to "other").
      await("Wait for submission to be valid (has some well-known state).")
        .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .pollDelay(1L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isNotEqualTo(GCSubmissionState.OTHER));
    }

    /**
     * Results of this test are meant to be reviewed as part of the manual
     * test-steps. Keep them aligned.
     */
    @ParameterizedTest
    @DisplayName("Should respect isSendSubmitter state.")
    @EnumSource(SendSubmitter.class)
    void shouldRespectSubmitter(@NonNull SendSubmitter sendSubmitter,
                                @NonNull Map<String, Object> originalGccProperties) {
      Map<String, Object> gccProperties = new HashMap<>(originalGccProperties);
      gccProperties.put(GCConfigProperty.KEY_IS_SEND_SUBMITTER, sendSubmitter.getSendSubmitter());
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      String comment = switch (sendSubmitter) {
        case YES -> "Respect send submitter-name explicitly (%s)".formatted(testName);
        case NO -> "Ignore submitter-name (use credentials user instead)";
        case DEFAULT -> "Default: Ignore submitter-name (use credentials user instead)";
      };
      long submissionId = facade.submitSubmission(
        "%s; %s".formatted(submissionName, sendSubmitter),
        comment,
        getSomeDueDate(),
        null,
        testName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      if (Boolean.TRUE.equals(sendSubmitter.getSendSubmitter())) {
        await("Submission should have submitter 'admin'.")
          .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
          .pollDelay(1L, TimeUnit.SECONDS)
          .pollInterval(10L, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).findSubmitter()).hasValue(testName));
      } else {
        await("Submission should not have submitter 'admin', but some system user (irrelevant, which)")
          .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
          .pollDelay(1L, TimeUnit.SECONDS)
          .pollInterval(10L, TimeUnit.SECONDS)
          .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).findSubmitter())
            .hasValueSatisfying(name -> assertThat(name).isNotEqualTo(testName))
          );
      }
    }

    @ParameterizedTest
    @DisplayName("Should accept various characters in the submitter name.")
    @EnumSource(SupplementaryMultilingualPlaneChallenge.class)
    void shouldAcceptSubmitterNameChallenge(@NonNull SupplementaryMultilingualPlaneChallenge challenge,
                                            @NonNull Map<String, Object> originalGccProperties) {
      Map<String, Object> gccProperties = new HashMap<>(originalGccProperties);
      gccProperties.put(GCConfigProperty.KEY_IS_SEND_SUBMITTER, true);
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      String submitterName = "%s(%s)".formatted(testName, challenge.getChallenge());
      long submissionId = facade.submitSubmission(
        "%s; %s".formatted(submissionName, challenge),
        null,
        getSomeDueDate(),
        null,
        submitterName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      // Just test, that the submission does not escalate.
      // We cannot validate the content of the submission instructions
      // as they are not returned by the GCC REST API.
      assertSubmissionReachesAnyStateOf(
        facade,
        submissionId,
        List.of(
          GCSubmissionState.STARTED,
          GCSubmissionState.ANALYZED,
          GCSubmissionState.TRANSLATE,
          GCSubmissionState.COMPLETED
        ),
        SUBMISSION_VALID_TIMEOUT_MINUTES
      );

      await("Submission should have submitter '%s'.".formatted(submitterName))
        .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .pollDelay(1L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
        .failFast(
          "The submission should not reach an error state.",
          () -> assertThat(facade.getSubmission(submissionId).isError()).isFalse()
        )
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).findSubmitter()).hasValue(submitterName));
    }

    /**
     * This test requires a known way how to make the GCC REST Backend fail
     * internally. For now, it is passing instructions that contain
     * Unicode characters from Supplementary Multilingual Plane without
     * escaping them.
     * <p>
     * This test relies on this ability to fail. If the behavior is changed,
     * and there is no other way to provoke an error, this test should be
     * removed.
     */
    @Test
    void shouldExposeErrorStateToClient(@NonNull Map<String, Object> originalGccProperties) {
      Map<String, Object> gccProperties = new HashMap<>(originalGccProperties);
      // The only known way to provoke a failure for now is using a
      // high Unicode character and set it unmodified as instruction text.
      gccProperties.put(GCConfigProperty.KEY_SUBMISSION_INSTRUCTION, Map.of(GCSubmissionInstruction.CHARACTER_TYPE_KEY, CharacterType.UNICODE));
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      String unicodeDove = "\uD83D\uDD4A";
      String comment = "Instruction to break GCC by directly passing Unicode character from Supplementary Multilingual Plane: %s".formatted(unicodeDove);

      long submissionId = facade.submitSubmission(
        submissionName,
        comment,
        getSomeDueDate(),
        null,
        testName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      await("Submission is expected to fail with error state.")
        .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .pollDelay(1L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).isError())
          .isTrue()
        );
    }

    /**
     * Tests instructions (forwarded from Workflow Comments) to be handed over
     * to the GCC backend. The instructions are expected to be in plain text.
     * <p>
     * <strong>Manual Test Reference</strong>: Note, that the test data created
     * by this test are also referenced in manual test steps. If you adjust
     * them, please also adjust the manual test steps.
     */
    @ParameterizedTest
    @DisplayName("Should respect and nicely handle instructions aka comments.")
    @EnumSource(CommentFixture.class)
    void shouldRespectInstructions(@NonNull CommentFixture fixture,
                                   @NonNull Map<String, Object> gccProperties) {
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      long submissionId = facade.submitSubmission(
        "%s; %s".formatted(submissionName, fixture.name()),
        fixture.getComment(),
        getSomeDueDate(),
        null,
        testName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      // Just test, that the submission does not escalate.
      // We cannot validate the content of the submission instructions
      // as they are not returned by the GCC REST API.
      assertSubmissionReachesAnyStateOf(
        facade,
        submissionId,
        List.of(
          GCSubmissionState.STARTED,
          GCSubmissionState.ANALYZED,
          GCSubmissionState.TRANSLATE,
          GCSubmissionState.COMPLETED
        ),
        SUBMISSION_VALID_TIMEOUT_MINUTES
      );
    }

    @ParameterizedTest
    @DisplayName("Should prevent failures in GCC backend for problematic characters in the submission name.")
    @EnumSource(SupplementaryMultilingualPlaneChallenge.class)
    void shouldPreemptivelyReplaceProblematicCharactersInSubmissionNames(@NonNull SupplementaryMultilingualPlaneChallenge challenge,
                                                                         @NonNull Map<String, Object> gccProperties) {
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      String submissionNameChallenge = "%s(%s)".formatted(submissionName, challenge.getChallenge());
      long submissionId = facade.submitSubmission(
        submissionNameChallenge,
        "Submission name with possible problematic characters: challenge ID '%s'".formatted(challenge),
        getSomeDueDate(),
        null,
        testName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      // Main focus of this test is to ensure that the submission name
      // does not contain any problematic characters that make the GCC backend
      // struggle.
      assertSubmissionReachesAnyStateOf(
        facade,
        submissionId,
        List.of(
          GCSubmissionState.STARTED,
          GCSubmissionState.ANALYZED,
          GCSubmissionState.TRANSLATE,
          GCSubmissionState.COMPLETED
        ),
        SUBMISSION_VALID_TIMEOUT_MINUTES
      );

      // Just validating, that some name is set.
      await("Submission is expected to have a submission name.")
        .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .pollDelay(1L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getName())
          .startsWith(submissionName)
        );
    }

    @ParameterizedTest
    @EnumSource(SubmissionNameLengthFixture.class)
    void shouldPreemptivelyTruncateLongSubmissionNames(@NonNull SubmissionNameLengthFixture fixture,
                                                       @NonNull Map<String, Object> gccProperties) {
      String paddedSubmissionName = fixture.pad(submissionName);
      GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
      String fileId = facade.uploadContent(testName, new ByteArrayResource(XML_CONTENT.getBytes(UTF_8)), null);
      // Unmet Failure Scenario: If the length of the submission name eventually
      // passed to the GCC REST Backend is longer than the available maximum
      // length, a 400-error is raised along with a message that states the
      // maximum length.
      long submissionId = facade.submitSubmission(
        paddedSubmissionName,
        "%s (%s)\nOriginal Submission Name (%d chars, expected to be shortened if required to at maximum %d characters):\n\t%s".formatted(
          testName,
          fixture,
          paddedSubmissionName.length(),
          GCSubmissionName.DEFAULT_MAX_LENGTH,
          paddedSubmissionName
        ),
        getSomeDueDate(),
        null,
        testName,
        Locale.US, Map.of(fileId, List.of(Locale.GERMANY)));

      assertThat(submissionId).isGreaterThan(0L);

      await("Submission is expected to have a truncated submission name.")
        .atMost(SUBMISSION_VALID_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        .pollDelay(1L, TimeUnit.SECONDS)
        .pollInterval(10L, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getName())
          .startsWith(paddedSubmissionName.substring(0, Math.min(GCSubmissionName.DEFAULT_MAX_LENGTH, paddedSubmissionName.length())))
        );
    }
  }

  /**
   * This test addresses the full process of submitting files and receiving them
   * from GCC sandbox. The locales to use will be derived from the supported locales
   * at GCC. The test requires at minimum 2 supported locales and will fail otherwise.
   * <p>
   * You may exclude this test during normal tests runs via:
   * <pre>{@code
   * mvn test -DexcludedGroups=slow
   * }</pre>
   *
   * @param gccProperties properties to log in
   */
  @Test
  @Tag("slow")
  @Tag("full")
  @DisplayName("Translate XLIFF and receive results (takes about 10 Minutes)")
  void translateXliff(@NonNull Map<String, Object> gccProperties) {
    GCExchangeFacade facade = new DefaultGCExchangeFacade(gccProperties);
    ConnectorsConfig.ConnectorsConfigResponseData connectorsConfig = facade.getDelegate().getConnectorsConfig();
    List<Locale> targetLocales = getSupportedLocaleStream(connectorsConfig, lc -> !lc.getIsSource()).toList();
    Locale masterLocale = getSupportedLocaleStream(connectorsConfig, LocaleConfig::getIsSource)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("At least one source locale required."));

    if (targetLocales.isEmpty()) {
      throw new IllegalStateException("At least one target locale (non-source) required.");
    }

    Map<String, List<Locale>> contentMap = uploadContents(facade, testName, masterLocale, targetLocales);
    long submissionId = facade.submitSubmission(
      submissionName,
      "Full translation workflow test from submission to retrieving results.",
      getSomeDueDate(),
      null,
      testName,
      masterLocale, contentMap);

    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.COMPLETED, TRANSLATION_TIMEOUT_MINUTES);

    List<String> xliffResults = new ArrayList<>();

    facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResults));

    assertThat(xliffResults)
      .describedAs("All XLIFFs shall have been pseudo-translated.")
      .hasSize(targetLocales.size())
      .allSatisfy(s -> assertThat(s).doesNotContain("<target>Lorem Ipsum"));

    //After all tasks have been marked as delivered also the submission shall be marked as delivered.
    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.DELIVERED, 5L);
  }

  private record TaskDataConsumer(List<String> xliffResults) implements BiPredicate<InputStream, GCTaskModel> {
    @Override
    public boolean test(InputStream is, GCTaskModel task) {
      ByteSource byteSource = new ByteSource() {
        @Override
        @NonNull
        public InputStream openStream() {
          return is;
        }
      };
      try {
        StringBuilder xliffResult = new StringBuilder();
        byteSource.asCharSource(UTF_8).copyTo(xliffResult);
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
      String fileId = facade.uploadContent(fileName, new ByteArrayResource(xliffContent.getBytes(UTF_8)), masterLocale);
      contentMapBuilder.put(fileId, List.of(targetLocale));
    }

    return contentMapBuilder.build();
  }

  private static void assertSubmissionReachesState(GCExchangeFacade facade, long submissionId, GCSubmissionState stateToReach, long timeout) {
    assertSubmissionReachesAnyStateOf(facade, submissionId, List.of(stateToReach), timeout);
  }

  private static void assertSubmissionReachesAnyStateOf(GCExchangeFacade facade, long submissionId, List<GCSubmissionState> expectedStates, long timeout) {
    long startNanos = System.nanoTime();
    await("Wait for translation to reach any of the state(s): %s".formatted(expectedStates))
      .atMost(timeout, TimeUnit.MINUTES)
      .pollDelay(1L, TimeUnit.MINUTES)
      .pollInterval(1L, TimeUnit.MINUTES)
      .failFast(
        "The submission should not reach an error state.",
        () -> assertThat(facade.getSubmission(submissionId).isError()).isFalse()
      )
      .conditionEvaluationListener(condition -> {
        GCSubmissionModel submission = facade.getSubmission(submissionId);
        Duration elapsed = Duration.ofMillis(condition.getElapsedTimeInMS());
        Duration remaining = Duration.ofMillis(condition.getRemainingTimeInMS());
        LOG.info("Submission: {}; Current State: {}; Expected States: {}; Time: elapsed {}, remaining {}; Submission Details: {}",
          submission.getSubmissionId(),
          submission.getState(),
          expectedStates,
          elapsed,
          remaining,
          submission.describe()
        );
      })
      .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isIn(expectedStates));
    if (LOG.isInfoEnabled()) {
      String submissionDetails = facade.getSubmission(submissionId).describe();
      long elapsedNanos = System.nanoTime() - startNanos;
      LOG.info("Success after {}: {}", Duration.ofNanos(elapsedNanos), submissionDetails);
    }
  }

  @NonNull
  private static String padEnd(@NonNull String str, int minLength, char startChar, char endChar) {
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

  enum SendSubmitter {
    YES(true),
    NO(false),
    DEFAULT(null);

    @Nullable
    private final Boolean isSendSubmitter;

    SendSubmitter(@Nullable Boolean isSendSubmitter) {
      this.isSendSubmitter = isSendSubmitter;
    }

    @Nullable
    public Boolean getSendSubmitter() {
      return isSendSubmitter;
    }
  }

  @SuppressWarnings("UnnecessaryUnicodeEscape")
  enum SubmissionNameLengthFixture {
    /**
     * Expectation is, that the additional information (from/to languages) will
     * be partially visible in the submission name. Truncated though, as the
     * actually set submission name takes precedence.
     */
    ASCII_CLOSE_TO_LIMIT(GCSubmissionName.DEFAULT_MAX_LENGTH - 10, 'a', 'z'),
    /**
     * Same as {@link #ASCII_CLOSE_TO_LIMIT}, but the submission name is expected
     * to miss even more information.
     */
    ASCII_VERY_CLOSE_TO_LIMIT(GCSubmissionName.DEFAULT_MAX_LENGTH - 3, 'a', 'z'),
    /**
     * No additional information is expected to be visible in the submission
     * name.
     */
    ASCII_HIT_LIMIT(GCSubmissionName.DEFAULT_MAX_LENGTH, 'a', 'z'),
    /**
     * Even the passed submission name is expected to be truncated.
     */
    ASCII_EXCEED_LIMIT(GCSubmissionName.DEFAULT_MAX_LENGTH * 2, 'a', 'z'),
    /**
     * Same as {@link #ASCII_HIT_LIMIT}, but with a different character set.
     */
    BMP_HIT_LIMIT(GCSubmissionName.DEFAULT_MAX_LENGTH, '\u2190', '\u21FF'),
    /**
     * Same as {@link #ASCII_EXCEED_LIMIT}, but with a different character set.
     */
    BMP_EXCEED_LIMIT(GCSubmissionName.DEFAULT_MAX_LENGTH * 2, '\u2190', '\u21FF'),
    ;

    private final int minLength;
    private final char startChar;
    private final char endChar;

    SubmissionNameLengthFixture(int minLength, char startChar, char endChar) {
      this.minLength = minLength;
      this.startChar = startChar;
      this.endChar = endChar;
    }

    public String pad(@NonNull String original) {
      return padEnd(original, minLength, startChar, endChar);
    }
  }

  /**
   * Fixtures for the comment/instructions feature.
   */
  @SuppressWarnings("UnnecessaryUnicodeEscape")
  enum CommentFixture {
    BMP("""
      Basic Multilingual Plane (BMP) Characters expected to be supported:
      \t* Umlauts: äöüÄÖÜß€µ
      \t* Special Characters: !@#$%^&*()_+
      \t* Arrows: ←↑→↓↔↕↖↗↘↙
      \t* BMP (Plane 0):
      \t\t* Fullwidth Exclamation Mark: \uFF01
      \t\t* Fullwidth Question Mark: \uFF1F
      \t\t* Fullwidth Left/Right Parenthesis: Before\uFF08Between\uFF09After
      EOM"""),
    FORMAT("""
      Formatting Characters should be transformed: Linux Newline
      Windows Newline\r
      Mac Newline\r\
      Tab Indent:
      \t* Item 1
      \t* Item 2
      EOM"""),
    HTML_AS_TEXT("""
      HTML/XML Special Characters should be escaped (expecting plain text only):
      \t* Known Tags to be represented as plain-text: <strong>Probe</strong><br>
      \t* Known HTML Entities to be represented as plain-text: &lt;br&gt;&amp;quot;
      EOM"""),
    UNICODE_SMP("""
      Unicode: As of know Supplementary Multilingual Planes are unsupported at GCC.
      To make them work but also still kind of distinguishable, we transform them
      to plain ASCII, just representing their Unicode code points:
      \t* Block: Miscellaneous Symbols and Pictographs, Dove: \uD83D\uDD4A (U+1F54A)
      EOM""");

    @NonNull
    private final String comment;

    CommentFixture(@NonNull String comment) {
      this.comment = comment;
    }

    @NonNull
    public String getComment() {
      return comment;
    }
  }

  @SuppressWarnings("UnnecessaryUnicodeEscape")
  enum SupplementaryMultilingualPlaneChallenge {
    ASCII("<em>!&;,:_"),
    BMP("ä&→\uFF01"),
    SMP("Dove: \uD83D\uDD4A");

    @NonNull
    private final String challenge;

    SupplementaryMultilingualPlaneChallenge(@NonNull String challenge) {
      this.challenge = challenge;
    }

    @NonNull
    public String getChallenge() {
      return challenge;
    }
  }
}
