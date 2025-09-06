package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.DefaultGCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import com.google.common.io.ByteSource;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@NullMarked
class MockedGCExchangeFacadeTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final long TRANSLATION_TIMEOUT_MINUTES = 3L;

  private static final String XLIFF_FILE="LoremIpsum.xliff";

  private static final String PRE_POPULATED_TARGET = "<target>Untranslated</target>";

  @Test
  @SuppressWarnings("squid:S1166")
  void translateXliff(TestInfo testInfo) {
    String testName = testInfo.getDisplayName();

    Resource xliffResource = new ClassPathResource(XLIFF_FILE, MockedGCExchangeFacade.class);

    // Let the tasks proceed faster.
    GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
      Map.of(
        MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
        MockSettings.CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE, 20
      )
    ));

    String fileId = facade.uploadContent(testName, xliffResource, null);
    long submissionId = facade.submitSubmission(
            testName,
            null,
            ZonedDateTime.of(LocalDateTime.now().plusHours(2L), ZoneId.systemDefault()),
            null,
            "admin",
            Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.COMPLETED);

    StringBuilder xliffResult = new StringBuilder();

    facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResult));

    LOG.info("XLIFF Result: {}", xliffResult);

    Assertions.assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(GCSubmissionState.DELIVERED);
    assertThat(xliffResult)
      .describedAs("Some pseudo-translation should have been performed.")
      .doesNotContain(PRE_POPULATED_TARGET)
      .matches("(?s).*<target>[^<]+</target>.*");

  }

  private record TaskDataConsumer(StringBuilder xliffResult) implements BiPredicate<InputStream, GCTaskModel> {
    @Override
    public boolean test(InputStream is, GCTaskModel task) {
      ByteSource byteSource = new ByteSource() {
        @Override
        public InputStream openStream() {
          return is;
        }
      };
      try {
        byteSource.asCharSource(StandardCharsets.UTF_8).copyTo(xliffResult);
      } catch (IOException e) {
        return false;
      }
      return true;
    }
  }

  @Test
  void submissionCanBeForcedToReachCancelledState(TestInfo testInfo) {
    String testName = testInfo.getDisplayName();

    Resource xliffResource = new ClassPathResource(XLIFF_FILE, MockedGCExchangeFacade.class);

    // Let the tasks proceed faster.
    GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
      Map.of(
        MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
        MockSettings.CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE, 20
      )
    ));

    String fileId = facade.uploadContent(testName, xliffResource, Locale.US);
    long submissionId = facade.submitSubmission(
            "states:other,cancelled",
            null,
            ZonedDateTime.of(LocalDateTime.now().plusHours(2L), ZoneId.systemDefault()),
            null,
            "admin",
            Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.CANCELLED);
  }

  @Test
  void facadeAvailableViaServiceLoader() {
    GCExchangeFacade facade = DefaultGCExchangeFacadeSessionProvider.defaultFactory().openSession(singletonMap(GCConfigProperty.KEY_TYPE, MockGCExchangeFacadeProvider.TYPE_TOKEN));
    assertThat(facade).isInstanceOf(MockedGCExchangeFacade.class);
  }

  private static void assertSubmissionReachesState(GCExchangeFacade facade, long submissionId, GCSubmissionState desiredState) {
    Awaitility.await("Wait for translation to reach state: " + desiredState)
      .atMost(TRANSLATION_TIMEOUT_MINUTES, TimeUnit.MINUTES)
      .pollDelay(1L, TimeUnit.SECONDS)
      .pollInterval(1L, TimeUnit.SECONDS)
      .conditionEvaluationListener(condition -> LOG.info("Submission {}, Current State: {}, elapsed time in seconds: {}", submissionId, facade.getSubmission(submissionId).getState(), condition.getElapsedTimeInMS() / 1000L))
      .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(desiredState));
  }

}
