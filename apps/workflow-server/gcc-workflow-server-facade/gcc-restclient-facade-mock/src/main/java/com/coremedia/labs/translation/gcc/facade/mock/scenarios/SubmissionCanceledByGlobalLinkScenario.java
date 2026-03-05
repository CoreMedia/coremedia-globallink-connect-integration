package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;
import java.util.Set;

/**
 * A scenario that simulates submissions being canceled by GlobalLink.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class SubmissionCanceledByGlobalLinkScenario implements Scenario, SubmissionInterceptor {
  /**
   * ID of this scenario.
   */
  public static final String ID = "submission-canceled-by-globallink";
  private static final Set<GCSubmissionState> overriddenStates = EnumSet.of(
    GCSubmissionState.COMPLETED,
    GCSubmissionState.DELIVERED
  );

  @Override
  public String id() {
    return ID;
  }

  /**
   * Intercepts the submission state and changes it to
   * {@link GCSubmissionState#CANCELLED} if it is in one of the overridden
   * states.
   *
   * @param base the original submission model
   * @return the modified submission model with state set to
   * {@link GCSubmissionState#CANCELLED} if it was in one of the overridden
   * states; otherwise, returns the original model
   */
  @Override
  public GCSubmissionModel intercept(GCSubmissionModel base) {
    GCSubmissionState submissionState = base.getState();
    if (overriddenStates.contains(submissionState)) {
      return GCSubmissionModel.builder(base)
        .state(GCSubmissionState.CANCELLED)
        .build();
    }
    return base;
  }
}
