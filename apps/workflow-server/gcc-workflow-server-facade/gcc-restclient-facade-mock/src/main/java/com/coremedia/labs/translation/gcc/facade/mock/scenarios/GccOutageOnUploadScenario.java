package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import org.jspecify.annotations.NullMarked;

/**
 * A scenario that simulates an outage of the GCC service during the upload.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class GccOutageOnUploadScenario implements Scenario, UploadInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "gcc-outage-on-upload";

  @Override
  public String id() {
    return ID;
  }

  /**
   * Simulates an outage by throwing a {@link GCFacadeCommunicationException}.
   *
   * @throws GCFacadeCommunicationException always thrown to simulate the outage
   */
  @Override
  public void startUpload() {
    throw new GCFacadeCommunicationException("Exception to test upload communication errors with translation service.");
  }
}
