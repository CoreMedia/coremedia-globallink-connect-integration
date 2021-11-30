package com.coremedia.labs.translation.gcc.facade.internal;

import com.coremedia.labs.translation.gcc.facade.DefaultGCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@ExtendWith(MockitoExtension.class)
class InternalGCExchangeFacadeTest {


  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final int TRANSLATION_TIMEOUT_MINUTES = 3;

  private static final String XLIFF_FILE = "LoremIpsum.xliff";

  @Test
  void submissionCanBeForcedToReachCancelledState(TestInfo testInfo) {
    String testName = testInfo.getDisplayName();

    Resource xliffResource = new ClassPathResource(XLIFF_FILE, InternalGCExchangeFacade.class);

    try (InternalGCExchangeFacade facade = new InternalGCExchangeFacade()) {

      String fileId = facade.uploadContent(testName, xliffResource, Locale.getDefault());
      long submissionId = facade.submitSubmission(
              "states:other,cancelled",
              null,
              ZonedDateTime.of(LocalDateTime.now().plusHours(2), ZoneId.systemDefault()),
              null,
              "admin",
              Locale.US, singletonMap(fileId, singletonList(Locale.ROOT)));

      assertSubmissionReachesState(facade, submissionId, GCSubmissionState.CANCELLED);
    }
  }

  @Test
  void facadeAvailableViaServiceLoader() {
    try (GCExchangeFacade facade = DefaultGCExchangeFacadeSessionProvider.defaultFactory().openSession(singletonMap(GCConfigProperty.KEY_TYPE, InternalGCExchangeFacadeProvider.TYPE_TOKEN))) {
      assertThat(facade).isInstanceOf(InternalGCExchangeFacade.class);
    }
  }

  private static void assertSubmissionReachesState(GCExchangeFacade facade, long submissionId, GCSubmissionState desiredState) {
    Awaitility.await("Wait for translation to reach state: " + desiredState)
            .atMost(TRANSLATION_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .pollDelay(1, TimeUnit.SECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .conditionEvaluationListener(condition -> LOG.info("Submission {}, Current State: {}, elapsed time in seconds: {}", submissionId, facade.getSubmission(submissionId), condition.getElapsedTimeInMS() / 1000L))
            .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(desiredState));
  }

}
