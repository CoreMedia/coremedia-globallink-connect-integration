package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A scenario that simulates a cancelation attempt for a non-existing
 * submission.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class CancelationNotFoundScenario implements Scenario, CancelationInterceptor, SubmissionInterceptor {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * We must not reach any of these states to be able to provoke the
   * cancelation failure.
   */
  private static final Set<GCSubmissionState> FORBIDDEN_CANCELATION_STATES = Set.of(
    GCSubmissionState.CANCELLED,
    GCSubmissionState.CANCELLATION_CONFIRMED,
    GCSubmissionState.COMPLETED,
    GCSubmissionState.DELIVERED,
    GCSubmissionState.REDELIVERED
  );

  /**
   * The identifier of this scenario.
   */
  public static final String ID = "cancelation-not-found";

  @Override
  public String id() {
    return ID;
  }

  /**
   * Ensures that the submission is not in a state that would prevent
   * cancelation.
   *
   * @param base the original submission model
   * @return the original or an adapted submission model
   */
  @Override
  public GCSubmissionModel intercept(GCSubmissionModel base) {
    GCSubmissionState actualState = base.getState();
    if (FORBIDDEN_CANCELATION_STATES.contains(actualState)) {
      return GCSubmissionModel.builder(base)
        .state(GCSubmissionState.TRANSLATE)
        .build();
    }
    return base;
  }

  /**
   * Simulates a cancelation attempt for a non-existing submission.
   * <p>
   * Always returns a 404 status code.
   *
   * @return an {@code Optional} containing the HTTP status code 404
   */
  @Override
  public Optional<Integer> startCancelation() {
    LOG.info("Mock scenario '{}' simulates cancelation of a non-existing submission, returning 404.", ID);
    return Optional.of(404);
  }
}
