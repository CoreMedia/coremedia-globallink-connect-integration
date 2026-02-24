package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/**
 * May intercept the cancellation of a submission.
 * <p>
 * For example, it may simulate an error during cancellation.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public interface CancellationInterceptor {
  /**
   * A cancellation interceptor that does nothing.
   */
  CancellationInterceptor NO_OPERATION = Optional::empty;

  /**
   * Intercept prior to the cancellation of the submission.
   * <p>
   * <strong>Relevant HTTP Code Overrides:</strong>
   * Any one of the possible errors documented in
   * <a href="https://connect.translations.com/docs/api/v2/index.html#submissions_cancel">GlobalLink Documentation</a>,
   * i.e., 400 (Bad Request), 401 (Unauthorized Access), 404 (Not Found), 500
   * (Internal Server Error).
   *
   * @return optional HTTP status code to be returned from the cancellation
   * request (e.g., 500 to simulate a server error); if empty, the cancellation
   * proceeds normally
   * @throws GCFacadeCommunicationException to simulate a communication
   *                                        failure during the cancellation
   *                                        request
   */
  Optional<Integer> startCancellation();

  /**
   * Intercept prior to the cancellation of the submission.
   * <p>
   * <strong>Relevant HTTP Code Overrides:</strong>
   * Any one of the possible errors documented in
   * <a href="https://connect.translations.com/docs/api/v2/index.html#submissions_cancel">GlobalLink Documentation</a>,
   * i.e., 400 (Bad Request), 401 (Unauthorized Access), 404 (Not Found), 500
   * (Internal Server Error).
   *
   * @return optional HTTP status code to be returned from the cancellation
   * request (e.g., 500 to simulate a server error); if empty, the cancellation
   * proceeds normally
   * @throws GCFacadeCommunicationException to simulate a communication
   *                                        failure during the cancellation
   *                                        request
   * @deprecated Use {@link #startCancellation()} instead.
   */
  @SuppressWarnings("SpellCheckingInspection")
  @Deprecated(since = "2512.0.0-1", forRemoval = true)
  default Optional<Integer> startCancelation() {
    return startCancellation();
  }
}
