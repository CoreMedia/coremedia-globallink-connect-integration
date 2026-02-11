package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.assertSubmissionReachesState;
import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.xliffResource;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@NullMarked
class SubmissionCanceledByGlobalLinkScenarioTest {
  @Nested
  class FacadeIntegrationBehavior {
    @Test
    void shouldMockCancelationByGlobalLink(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(
          MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
          MockSettings.SCENARIO, SubmissionCanceledByGlobalLinkScenario.ID
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

      assertSubmissionReachesState(facade, submissionId, GCSubmissionState.CANCELLED);

      facade.confirmCancelledTasks(submissionId);

      assertSubmissionReachesState(facade, submissionId, GCSubmissionState.CANCELLATION_CONFIRMED);
    }
  }

}
