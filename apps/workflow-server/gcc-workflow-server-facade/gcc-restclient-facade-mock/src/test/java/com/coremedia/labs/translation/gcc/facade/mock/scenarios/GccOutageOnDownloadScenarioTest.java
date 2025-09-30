package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.assertSubmissionReachesState;
import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.xliffResource;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GccOutageOnDownloadScenarioTest {
  @Nested
  class FacadeIntegrationBehavior {
    @Test
    void shouldThrowExceptionOnDownload(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(
          MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
          MockSettings.SCENARIO, GccOutageOnDownloadScenario.ID
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

      assertThatThrownBy(() -> facade.downloadCompletedTasks(submissionId, new TrueTaskDataConsumer()))
        .isInstanceOf(GCFacadeCommunicationException.class);
    }
  }

  private static class TrueTaskDataConsumer implements BiPredicate<InputStream, GCTaskModel> {
    @Override
    public boolean test(InputStream inputStream, GCTaskModel task) {
      return true;
    }
  }
}
