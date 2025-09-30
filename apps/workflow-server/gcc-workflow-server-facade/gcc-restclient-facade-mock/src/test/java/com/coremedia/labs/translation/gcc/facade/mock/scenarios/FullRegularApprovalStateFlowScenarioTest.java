package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.xliffResource;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

@NullMarked
class FullRegularApprovalStateFlowScenarioTest {
  @Nested
  class FacadeIntegrationBehavior {
    @Test
    // Using a timeout to avoid endless loops in case of errors reaching
    // the delivered state.
    @Timeout(30L)
    void shouldStepThroughAllExpectedSubmissionStates(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(MockSettings.SCENARIO, FullRegularApprovalStateFlowScenario.ID)
      ));

      String fileId = facade.uploadContent(testName, xliffResource, Locale.US);
      long submissionId = facade.submitSubmission(
        testName,
        null,
        ZonedDateTime.of(LocalDateTime.now().plusHours(2L), ZoneId.systemDefault()),
        null,
        "admin",
        Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

      GCSubmissionState currentState;

      List<GCSubmissionState> seenStates = new ArrayList<>();

      do {
        // Side effect: Must invoke `getSubmission` again and again, as this
        // will activate the mocked state transitions.
        currentState = facade.getSubmission(submissionId).getState();
        seenStates.add(currentState);
        // Note, that we don't signal a delivery, so that we will only reach
        // state COMPLETED, not DELIVERED.
      } while (currentState != GCSubmissionState.COMPLETED);

      assertThat(seenStates)
        .containsExactly(
          GCSubmissionState.IN_PRE_PROCESS,
          GCSubmissionState.STARTED,
          GCSubmissionState.ANALYZED,
          GCSubmissionState.AWAITING_APPROVAL,
          GCSubmissionState.AWAITING_QUOTE_APPROVAL,
          GCSubmissionState.TRANSLATE,
          GCSubmissionState.REVIEW,
          GCSubmissionState.COMPLETED
        );
    }
  }

}
