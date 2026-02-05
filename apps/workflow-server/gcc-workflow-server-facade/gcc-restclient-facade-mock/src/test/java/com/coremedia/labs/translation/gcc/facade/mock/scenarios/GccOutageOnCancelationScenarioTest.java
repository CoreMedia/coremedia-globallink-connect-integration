package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
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

import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.xliffResource;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@NullMarked
class GccOutageOnCancelationScenarioTest {
  @Nested
  class FacadeIntegrationBehavior {
    @Test
    void shouldThrowExceptionOnCancelation(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(MockSettings.SCENARIO, GccOutageOnCancelationScenario.ID)
      ));

      String fileId = facade.uploadContent(testName, xliffResource, Locale.US);
      long submissionId = facade.submitSubmission(
        testName,
        null,
        ZonedDateTime.of(LocalDateTime.now(ZoneId.systemDefault()).plusHours(2L), ZoneId.systemDefault()),
        null,
        "admin",
        Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

      assertThatThrownBy(() -> facade.cancelSubmission(submissionId))
        .isInstanceOf(GCFacadeCommunicationException.class);
    }
  }
}
