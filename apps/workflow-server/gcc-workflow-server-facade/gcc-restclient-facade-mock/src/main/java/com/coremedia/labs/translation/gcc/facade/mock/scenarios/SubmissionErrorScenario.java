package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import org.jspecify.annotations.NullMarked;

/**
 * A scenario that marks a submission as having an error. This is a rare but
 * reachable case in GCC, where the REST backend, for example, fails to interact
 * with the Project Director backend.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class SubmissionErrorScenario implements Scenario, SubmissionInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "submission-error";

  @Override
  public String id() {
    return ID;
  }

  /**
   * Marks the submission as having an error.
   *
   * @param base the original submission model
   * @return a copy of the submission model with the error flag set
   */
  @Override
  public GCSubmissionModel intercept(GCSubmissionModel base) {
    return GCSubmissionModel.builder(base)
      .error(true)
      .build();
  }
}
