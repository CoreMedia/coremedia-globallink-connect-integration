package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.TaskDataConsumer;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
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
class SubmissionRedeliveredScenarioTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  @Nested
  class FacadeIntegrationBehavior {
    @Test
    void shouldSimulateRedeliveredBehaviorWithBrokenXliff(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(
          MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
          MockSettings.SCENARIO, SubmissionRedeliveredScenario.ID
        )
      ));

      String fileId = facade.uploadContent(testName, xliffResource, Locale.US);
      long submissionId = facade.submitSubmission(
        testName,
        null,
        ZonedDateTime.of(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2L), ZoneId.systemDefault()),
        null,
        "admin",
        Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

      assertSubmissionReachesState(facade, submissionId, GCSubmissionState.REDELIVERED);

      StringBuilder xliffResult = new StringBuilder();
      facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResult));

      LOG.info("XLIFF Result: {}", xliffResult);

      assertThat(xliffResult)
        .as("XLIFF should simulate to be broken")
        .contains("<invalid")
        .contains("xmlns=\"intentionally\"")
      ;

    }
  }
}
