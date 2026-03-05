package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A scenario that simulates a submission being redelivered after it was already
 * completed once. Prior to reaching redelivered state, it will signal a failed
 * XLIFF download by translating the XLIFF to an invalid format (as done in
 * {@link TranslateInvalidXliffScenario}).
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class SubmissionRedeliveredScenario extends TranslateInvalidXliffScenario implements Scenario, SubmissionInterceptor {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * The identifier of this scenario.
   */
  public static final String ID = "submission-redelivered";

  /**
   * Remembers submissions that were already completed once, so we can
   * simulate a redelivery on subsequent completions.
   */
  private final List<Long> completedSubmissions = new ArrayList<>();

  @Override
  public String id() {
    return ID;
  }

  /**
   * If the submission is in state {@link GCSubmissionState#COMPLETED} and
   * it was not yet completed before, remember it as completed. If it was
   * already completed before, change its state to
   * {@link GCSubmissionState#REDELIVERED}.
   * <p>
   * Prior to that, you may expect, that the XLIFF download fails due to
   * the invalid XLIFF that is produced by the superclass
   * {@link TranslateInvalidXliffScenario}.
   *
   * @param base the original submission model
   * @return either the original submission model, or a copy with state changed
   * to {@code REDELIVERED}
   */
  @Override
  public GCSubmissionModel intercept(GCSubmissionModel base) {
    long submissionId = base.getSubmissionId();
    GCSubmissionState submissionState = base.getState();
    if (submissionState == GCSubmissionState.COMPLETED) {
      if (completedSubmissions.contains(submissionId)) {
        LOG.info("Submission {} was already completed once (and we mocked a failed XLIFF download afterward), now simulating redelivery.", submissionId);
        return GCSubmissionModel.builder(base)
          .state(GCSubmissionState.REDELIVERED)
          .build();
      }
      completedSubmissions.add(submissionId);
    }
    return base;
  }
}
