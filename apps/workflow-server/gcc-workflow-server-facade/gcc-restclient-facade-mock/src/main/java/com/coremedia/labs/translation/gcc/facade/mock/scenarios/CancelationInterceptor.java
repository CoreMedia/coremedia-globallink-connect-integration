package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/**
 * May intercept the cancelation of a submission.
 * <p>
 * For example, it may simulate an error during cancelation.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public interface CancelationInterceptor {
  /**
   * A cancelation interceptor that does nothing.
   */
  CancelationInterceptor NO_OPERATION = Optional::empty;

  /**
   * Intercept prior to the cancelation of the submission.
   * <p>
   * <strong>Relevant HTTP Code Overrides:</strong>
   * Any one of the possible errors documented in
   * <a href="https://connect.translations.com/docs/api/v2/index.html#submissions_cancel">GlobalLink Documentation</a>,
   * i.e., 400 (Bad Request), 401 (Unauthorized Access), 404 (Not Found), 500
   * (Internal Server Error).
   *
   * @return optional HTTP status code to be returned from the cancelation
   * request (e.g., 500 to simulate a server error); if empty, the cancelation
   * proceeds normally
   * @throws GCFacadeCommunicationException to simulate a communication
   *                                        failure during the cancelation
   *                                        request
   */
  Optional<Integer> startCancelation();
}
