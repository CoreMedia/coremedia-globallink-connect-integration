package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.DefaultGCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.scenarios.SubmissionCanceledByGlobalLinkScenario;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import com.coremedia.labs.translation.gcc.util.Settings;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.assertSubmissionReachesState;
import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.xliffResource;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@NullMarked
class MockedGCExchangeFacadeTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final String PRE_POPULATED_TARGET = "<target>Untranslated</target>";

  @Test
  @SuppressWarnings("squid:S1166")
  void translateXliff(TestInfo testInfo) {
    String testName = testInfo.getDisplayName();

    Resource xliffResource = xliffResource();

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
      ZonedDateTime.of(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2L), ZoneId.systemDefault()),
      null,
      "admin",
      Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.COMPLETED);

    StringBuilder xliffResult = new StringBuilder();

    facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResult));

    LOG.debug("XLIFF Result: {}", xliffResult);

    Assertions.assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(GCSubmissionState.DELIVERED);
    assertThat(xliffResult)
      .describedAs("Some pseudo-translation should have been performed.")
      .doesNotContain(PRE_POPULATED_TARGET)
      .matches("(?s).*<target>[^<]+</target>.*");

  }

  @Test
  void submissionCanBeForcedToReachCancelledState(TestInfo testInfo) {
    String testName = testInfo.getDisplayName();

    Resource xliffResource = xliffResource();

    // Let the tasks proceed faster.
    GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
      Map.of(
        MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
        MockSettings.CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE, 20,
        MockSettings.SCENARIO, SubmissionCanceledByGlobalLinkScenario.ID
      )
    ));

    String fileId = facade.uploadContent(testName, xliffResource, Locale.US);
    long submissionId = facade.submitSubmission(
      "Canceled by GCC via scenario",
      null,
      ZonedDateTime.of(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2L), ZoneId.systemDefault()),
      null,
      "admin",
      Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

    assertSubmissionReachesState(facade, submissionId, GCSubmissionState.CANCELLED);
  }

  @Test
  void facadeAvailableViaServiceLoader() {
    GCExchangeFacade facade = DefaultGCExchangeFacadeSessionProvider.defaultFactory().openSession(new Settings(Map.of(GCConfigProperty.KEY_TYPE, MockGCExchangeFacadeProvider.TYPE_TOKEN)));
    assertThat(facade).isInstanceOf(MockedGCExchangeFacade.class);
  }
}
