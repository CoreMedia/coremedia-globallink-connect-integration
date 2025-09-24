package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.slf4j.LoggerFactory.getLogger;

@NullMarked
public enum SubmissionTestUtil {
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final long TRANSLATION_TIMEOUT_MINUTES = 3L;
  private static final String XLIFF_FILE = "LoremIpsum.xliff";

  public static void assertSubmissionReachesState(GCExchangeFacade facade,
                                                  long submissionId,
                                                  GCSubmissionState desiredState) {
    assertSubmissionReachesStateAnyStateOf(facade, submissionId, EnumSet.of(desiredState));
  }

  public static void assertSubmissionReachesStateAnyStateOf(GCExchangeFacade facade,
                                                            long submissionId,
                                                            Set<GCSubmissionState> desiredStates) {
    await("Wait for translation to reach any of the states: %s".formatted(desiredStates))
      .atMost(TRANSLATION_TIMEOUT_MINUTES, TimeUnit.MINUTES)
      .pollDelay(1L, TimeUnit.SECONDS)
      .pollInterval(1L, TimeUnit.SECONDS)
      .conditionEvaluationListener(condition -> LOG.info(
        "Submission {}, Current State: {}, elapsed time in seconds: {}",
        submissionId,
        facade.getSubmission(submissionId).getState(),
        condition.getElapsedTimeInMS() / 1000L)
      )
      .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isIn(desiredStates));
  }

  public static ClassPathResource xliffResource() {
    return new ClassPathResource(XLIFF_FILE, SubmissionTestUtil.class);
  }
}
