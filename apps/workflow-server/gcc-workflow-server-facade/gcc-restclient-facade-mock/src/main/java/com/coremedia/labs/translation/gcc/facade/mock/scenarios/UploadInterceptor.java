package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import org.jspecify.annotations.NullMarked;

/**
 * May intercept the XLIFF upload of a task.
 * <p>
 * For example, it may trigger a GCC connection outage during the upload.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public interface UploadInterceptor {
  /**
   * A no-operation interceptor.
   */
  UploadInterceptor NO_OPERATION = () -> {
    // No operation.
  };

  /**
   * Intercept prior to the upload of an XLIFF file.
   *
   * @throws GCFacadeCommunicationException to simulate a communication
   *                                        failure during the upload
   */
  void startUpload();
}
