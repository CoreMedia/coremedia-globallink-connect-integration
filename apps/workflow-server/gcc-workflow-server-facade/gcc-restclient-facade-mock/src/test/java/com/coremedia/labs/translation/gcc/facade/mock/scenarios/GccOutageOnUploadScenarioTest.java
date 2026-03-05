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

import java.util.Locale;
import java.util.Map;

import static com.coremedia.labs.translation.gcc.facade.mock.SubmissionTestUtil.xliffResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@NullMarked
class GccOutageOnUploadScenarioTest {
  @Nested
  class FacadeIntegrationBehavior {
    @Test
    void shouldThrowExceptionOnUpload(TestInfo testInfo) {
      String testName = testInfo.getDisplayName();

      Resource xliffResource = xliffResource();

      GCExchangeFacade facade = new MockedGCExchangeFacade(MockSettings.fromMockConfig(
        Map.of(
          MockSettings.CONFIG_STATE_CHANGE_DELAY_SECONDS, 2L,
          MockSettings.SCENARIO, GccOutageOnUploadScenario.ID
        )
      ));

      assertThatThrownBy(() -> facade.uploadContent(testName, xliffResource, Locale.US))
        .isInstanceOf(GCFacadeCommunicationException.class);
    }
  }

}
