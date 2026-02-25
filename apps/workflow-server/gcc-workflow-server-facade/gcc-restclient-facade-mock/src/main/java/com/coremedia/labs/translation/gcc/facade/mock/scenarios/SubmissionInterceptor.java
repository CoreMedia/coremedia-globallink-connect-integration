package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import org.jspecify.annotations.NullMarked;

/**
 * May intercept a submission model prior to returning it to the caller.
 * <p>
 * For example, it may mark a submission as having an error.
 */
@NullMarked
public interface SubmissionInterceptor {
  SubmissionInterceptor NO_OPERATION = base -> base;

  /**
   * Intercepts the submission model prior to returning it to the caller.
   * May be used, to mock an error state or change the current state of
   * a submission, for example.
   *
   * @param base the original submission model
   * @return the (possibly modified) submission model
   */
  GCSubmissionModel intercept(GCSubmissionModel base);
}
