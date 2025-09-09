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
import com.google.common.io.ByteSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

import static com.coremedia.labs.translation.gcc.facade.GCConfigProperty.KEY_FILE_TYPE;
import static com.coremedia.labs.translation.gcc.facade.def.ExtendedDefaultGCExchangeFacade.connect;
import static java.lang.invoke.MethodHandles.lookup;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.awaitility.Awaitility.await;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Contract test for GCC RestClient.
 * <p>
 * This is a test which should run on demand for example if you extended
 * the facade or if either the GCC Java API got updated, or the corresponding
 * GCC REST Backend.
 * <p>
 * It is a so-called contract test and thus tests the contract between
 * the consumer (this facade) and the producer (the GCC Java API).
 * <p>
 * In order to run the test, you need to add a file {@code .gcc.properties}
 * to your user home folder:
 * <pre>{@code
 * apiKey=ab12cd34
 * url=https://connect-dev.translations.com/api/v3/
 * key=0e...abc
 * fileType=xliff
 * }</pre>
 */
@ExtendWith(GccCredentialsExtension.class)
@NullMarked
class DefaultGCExchangeFacadeContractTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());
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
  void setUp(TestInfo testInfo) {
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
    void shouldLoginSuccessfully(Map<String, Object> gccProperties) {
      LOG.info("Properties: {}", gccProperties);
      assertThatCode(() -> connect(gccProperties))
        .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validate that invalid login is denied.")
    void shouldFailToLoginWithInvalidApiKey(Map<String, Object> gccProperties) {
      Map<String, Object> patchedProperties = new HashMap<>(gccProperties);
      patchedProperties.put("apiKey", "invalid");
      LOG.info("Properties: {} patched to {}", gccProperties, patchedProperties);
      assertThatCode(() -> connect(patchedProperties))
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
    void shouldValidateConnectorKeyInitially(Map<String, Object> gccProperties) {
      Map<String, Object> patchedProperties = new HashMap<>(gccProperties);
      patchedProperties.put("key", "invalid");
      LOG.info("Properties: {} patched to {}", gccProperties, patchedProperties);
      assertThatCode(() -> connect(patchedProperties))
        .isInstanceOf(GCFacadeConnectorKeyConfigException.class)
        .hasNoCause();
    }
  }

  /**
   * We expect some configuration, that suits our requirements for subsequent
   * tests. These will be validated here.
   */
  @Nested
  @NullUnmarked
  class ConfigurationValidation {
    private static ExtendedDefaultGCExchangeFacade facade;
    private static Map<String, Object> gccProperties;

    @BeforeAll
    static void beforeAll(Map<String, Object> gccProperties) {
      facade = connect(gccProperties);
      ConfigurationValidation.gccProperties = gccProperties;
    }

    @Test
    void shouldHaveAnyFileType() {
      assertThat(facade.fileTypes()).isNotEmpty();
    }

    @Test
    void shouldHaveExpectedFileTypeFromProperties() {
      Object propertyValue = gccProperties.get(KEY_FILE_TYPE);
      assumeThat(propertyValue)
        .as("Should have configured expected file type as String at %s, but is: %s".formatted(KEY_FILE_TYPE, propertyValue))
        .isInstanceOf(String.class);
      assertThat(facade.fileTypes())
        .contains(propertyValue.toString());
    }

    @Test
    void shouldHaveAnySourceLocaleAvailable(Map<String, Object> gccProperties) {
      assertThat(connect(gccProperties).supportedSourceLocales())
        .isNotEmpty();
    }

    @Test
    void shouldHaveAnyTargetLocaleAvailable(Map<String, Object> gccProperties) {
      assertThat(connect(gccProperties).supportedTargetLocales())
        .isNotEmpty();
    }

    @Test
    void shouldHaveAnyTranslationDirectionsConfigured(Map<String, Object> gccProperties) {
      assertThat(connect(gccProperties).translationDirections())
        .isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Test for content upload")
  @NullUnmarked
  class ContentUpload {
    private static ExtendedDefaultGCExchangeFacade facade;

    @BeforeAll
    static void beforeAll(Map<String, Object> gccProperties) {
      facade = connect(gccProperties);
    }

    @Test
    @DisplayName("Upload File.")
    void upload() {
      Instant startTimeUtc = Instant.now().atZone(ZoneOffset.UTC).toInstant();

      long contentCountBefore = facade.totalRecordsCount();

      XliffFixture fixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      String fileId = fixture.upload(facade).fileId();

      assertThat(fileId).isNotEmpty();

      long contentCountAfter = facade.totalRecordsCount();

      // Fail-early test: Ensure that we actually received any content.
      // Note, that we expect no latency here. If we experience that the
      // uploaded content is not immediately available, we have to introduce
      // a wait statement here (e.g., using Awaitility).
      assertThat(contentCountAfter).isGreaterThan(contentCountBefore);

      assertThat(facade.getContentList()).anySatisfy(f -> {
        assertThat(f.contentId()).isEqualTo(fileId);
        assertThat(f.updatedAt()).isAfter(startTimeUtc);
      });
    }
  }

  @Nested
  @DisplayName("Tests for cancellation")
  @NullUnmarked
  class Cancellation {
    private static ExtendedDefaultGCExchangeFacade facade;

    @BeforeAll
    static void beforeAll(Map<String, Object> gccProperties) {
      facade = connect(gccProperties);
    }

    @Test
    @DisplayName("Be aware of submission/task cancellation.")
    void shouldBeCancellationAware() {
      XliffFixture fixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      long submissionId = fixture.uploadAndSubmit(
        facade,
        submissionName,
        "Submission is meant to be cancelled via API and later confirmed.",
        testName
      );

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

  @Nested
  @DisplayName("Test general content submission")
  class ContentSubmission {
    @Test
    @DisplayName("Test simple submission")
    void shouldPerformSimpleSubmissionSuccessfully(Map<String, Object> gccProperties) {
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      XliffFixture fixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      long submissionId = fixture.uploadAndSubmit(
        facade,
        submissionName,
        "Testing just a simple submission.",
        testName
      );

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
    void shouldRespectSubmitter(SendSubmitter sendSubmitter,
                                Map<String, Object> originalGccProperties) {
      Map<String, Object> gccProperties = new HashMap<>(originalGccProperties);
      Boolean sendSubmitterFlag = sendSubmitter.getSendSubmitter();
      if (sendSubmitterFlag != null) {
        gccProperties.put(GCConfigProperty.KEY_IS_SEND_SUBMITTER, sendSubmitterFlag);
      }
      String comment = switch (sendSubmitter) {
        case YES -> "Respect send submitter-name explicitly (%s)".formatted(testName);
        case NO -> "Ignore submitter-name (use credentials user instead)";
        case DEFAULT -> "Default: Ignore submitter-name (use credentials user instead)";
      };
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      XliffFixture fixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      long submissionId = fixture.uploadAndSubmit(
        facade,
        "%s; %s".formatted(submissionName, sendSubmitter),
        comment,
        testName
      );

      assertThat(submissionId).isGreaterThan(0L);

      if (Boolean.TRUE.equals(sendSubmitterFlag)) {
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
    void shouldAcceptSubmitterNameChallenge(SupplementaryMultilingualPlaneChallenge challenge,
                                            Map<String, Object> originalGccProperties) {
      Map<String, Object> gccProperties = new HashMap<>(originalGccProperties);
      gccProperties.put(GCConfigProperty.KEY_IS_SEND_SUBMITTER, true);
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      String submitterName = "%s(%s)".formatted(testName, challenge.getChallenge());
      XliffFixture fixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      long submissionId = fixture.uploadAndSubmit(
        facade,
        "%s; %s".formatted(submissionName, challenge),
        "Submitter Name Challenge",
        submitterName
      );

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
    void shouldExposeErrorStateToClient(Map<String, Object> originalGccProperties) {
      Map<String, Object> gccProperties = new HashMap<>(originalGccProperties);
      // The only known way to provoke a failure for now is using a
      // high Unicode character and set it unmodified as instruction text.
      gccProperties.put(GCConfigProperty.KEY_SUBMISSION_INSTRUCTION, Map.of(GCSubmissionInstruction.CHARACTER_TYPE_KEY, CharacterType.UNICODE));
      String unicodeDove = "\uD83D\uDD4A";
      String comment = "Instruction to break GCC by directly passing Unicode character from Supplementary Multilingual Plane: %s".formatted(unicodeDove);
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      XliffFixture fixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      long submissionId = fixture.uploadAndSubmit(
        facade,
        submissionName,
        comment,
        testName
      );

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
    void shouldRespectInstructions(CommentFixture commentFixture,
                                   Map<String, Object> gccProperties) {
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      XliffFixture xliffFixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      long submissionId = xliffFixture.uploadAndSubmit(
        facade,
        "%s; %s".formatted(submissionName, commentFixture.name()),
        commentFixture.getComment(),
        testName
      );

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
    void shouldPreemptivelyReplaceProblematicCharactersInSubmissionNames(SupplementaryMultilingualPlaneChallenge challenge,
                                                                         Map<String, Object> gccProperties) {
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      XliffFixture xliffFixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      String submissionNameChallenge = "%s(%s)".formatted(submissionName, challenge.getChallenge());
      long submissionId = xliffFixture.uploadAndSubmit(
        facade,
        submissionNameChallenge,
        "Submission name with possible problematic characters: challenge ID '%s'".formatted(challenge),
        testName
      );

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
    void shouldPreemptivelyTruncateLongSubmissionNames(SubmissionNameLengthFixture fixture,
                                                       Map<String, Object> gccProperties) {
      ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
      XliffFixture xliffFixture = facade.translationDirectionsStream()
        .map(d -> XliffFixture.of(testName, d))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Missing translation direction to create relevant testcase."));

      String paddedSubmissionName = fixture.pad(submissionName);
      // Unmet Failure Scenario: If the length of the submission name eventually
      // passed to the GCC REST Backend is longer than the available maximum
      // length, a 400-error is raised along with a message that states the
      // maximum length.
      long submissionId = xliffFixture.uploadAndSubmit(
        facade,
        paddedSubmissionName,
        "%s (%s)\nOriginal Submission Name (%d chars, expected to be shortened if required to at maximum %d characters):\n\t%s".formatted(
          testName,
          fixture,
          paddedSubmissionName.length(),
          GCSubmissionName.DEFAULT_MAX_LENGTH,
          paddedSubmissionName
        ),
        testName
      );

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
  void translateXliff(Map<String, Object> gccProperties) {
    ExtendedDefaultGCExchangeFacade facade = ExtendedDefaultGCExchangeFacade.connect(gccProperties);
    // Ensure, that we have one source locale only.
    AtomicReference<@Nullable Locale> firstMasterLocale = new AtomicReference<>();
    List<XliffFixture> xliffFixtures = facade.translationDirectionsStream()
      .filter(td -> {
        Locale currentSource = td.source();
        Locale firstSource = firstMasterLocale.get();
        if (firstSource == null) {
          firstMasterLocale.set(currentSource);
          return true;
        }
        return currentSource.equals(firstSource);
      })
      .map(d -> XliffFixture.of(testName, d))
      .toList();

    if (xliffFixtures.isEmpty()) {
      throw new IllegalStateException("At least relevant translation direction must be available.");
    }

    long submissionId =
      XliffFixture.uploadAndSubmitAll(
        facade,
        submissionName,
        "Full translation workflow test from submission to retrieving results.",
        testName,
        Objects.requireNonNull(firstMasterLocale.get()),
        xliffFixtures
      );

    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.COMPLETED, TRANSLATION_TIMEOUT_MINUTES);

    List<String> xliffResults = new ArrayList<>();

    facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResults));

    assertThat(xliffResults)
      .describedAs("All XLIFFs shall have been pseudo-translated.")
      .hasSize(xliffFixtures.size())
      .allSatisfy(s -> assertThat(s).doesNotContain("<target>Lorem Ipsum"));

    //After all tasks have been marked as delivered also the submission shall be marked as delivered.
    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.DELIVERED, 5L);
  }

  private static class TrueTaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
    @Override
    public boolean test(InputStream inputStream, GCTaskModel task) {
      return true;
    }
  }

  private record TaskDataConsumer(List<String> xliffResults) implements BiPredicate<InputStream, GCTaskModel> {
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
        byteSource.asCharSource(UTF_8).copyTo(xliffResult);
        xliffResults.add(xliffResult.toString());
      } catch (IOException e) {
        return false;
      }
      return true;
    }
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

  private static String padEnd(String str, int minLength, char startChar, char endChar) {
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

    private final @Nullable Boolean isSendSubmitter;

    SendSubmitter(@Nullable Boolean isSendSubmitter) {
      this.isSendSubmitter = isSendSubmitter;
    }

    public @Nullable Boolean getSendSubmitter() {
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

    public String pad(String original) {
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

    private final String comment;

    CommentFixture(String comment) {
      this.comment = comment;
    }

    public String getComment() {
      return comment;
    }
  }

  @SuppressWarnings("UnnecessaryUnicodeEscape")
  enum SupplementaryMultilingualPlaneChallenge {
    ASCII("<em>!&;,:_"),
    BMP("ä&→\uFF01"),
    SMP("Dove: \uD83D\uDD4A");

    private final String challenge;

    SupplementaryMultilingualPlaneChallenge(String challenge) {
      this.challenge = challenge;
    }

    public String getChallenge() {
      return challenge;
    }
  }
}
