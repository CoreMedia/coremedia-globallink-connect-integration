package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import org.jspecify.annotations.NullMarked;

/**
 * A scenario that simulates an outage of the GCC service during the download.
 * <p>
 * This scenario throws a {@link GCFacadeCommunicationException} when the
 * download is started. This can be used to test error handling in the workflow
 * when the GCC service is not reachable.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class GccOutageOnDownloadScenario implements Scenario, DownloadInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "gcc-outage-on-download";

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
  public void startDownload() {
    throw new GCFacadeCommunicationException("Exception to test download communication errors with translation service.");
  }
}
