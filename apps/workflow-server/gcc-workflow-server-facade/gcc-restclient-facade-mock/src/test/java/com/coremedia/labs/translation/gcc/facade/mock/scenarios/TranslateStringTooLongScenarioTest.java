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
import static org.assertj.core.api.Assertions.assertThat;

@NullMarked
class TranslateStringTooLongScenarioTest {
  @Nested
  class FacadeIntegrationBehavior {
    @Test
    void shouldMockXliffChallengingStringPropertyMaxLength(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(
          MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
          MockSettings.SCENARIO, TranslateStringTooLongScenario.ID
        )
      ));

      String fileId = facade.uploadContent(testName, xliffResource, Locale.US);
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

      assertThat(xliffResult)
        .as("XLIFF should contain long strings in target nodes")
        .matches("(?s).*<target>[^<]*Lorem ipsum dolor sit amet.*");
    }
  }
}
