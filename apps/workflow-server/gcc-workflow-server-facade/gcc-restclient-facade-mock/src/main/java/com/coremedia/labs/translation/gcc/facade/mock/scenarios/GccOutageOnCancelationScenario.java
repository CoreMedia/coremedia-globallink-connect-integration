package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.Set;

/**
 * A scenario that simulates a cancelation attempt resulting in a communication error.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class GccOutageOnCancelationScenario implements Scenario, CancelationInterceptor, SubmissionInterceptor  {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "gcc-outage-on-cancelation";

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
    throw new GCFacadeCommunicationException("Exception to test cancel communication errors with translation service.");
  }
}
