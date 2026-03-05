package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

/**
 * May intercept the XLIFF download of a task.
 * <p>
 * For example, it may trigger a GCC connection outage during the download.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public interface DownloadInterceptor {
  DownloadInterceptor NO_OPERATION = () -> {
    // No operation.
  };

  /**
   * Intercept prior to the download of the XLIFF for a task.
   */
  void startDownload();
}
