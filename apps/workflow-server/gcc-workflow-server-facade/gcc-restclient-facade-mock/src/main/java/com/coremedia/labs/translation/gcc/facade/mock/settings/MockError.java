package com.coremedia.labs.translation.gcc.facade.mock.settings;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Arrays;
import java.util.Optional;

/**
 * Possible values for the {@code globalLink.mockError} setting.
 */
public enum MockError {
  /**
   * Provokes a communication failure during the cancellation request.
   */
  CANCEL_COMMUNICATION,
  /**
   * Provokes a server-side failure during the cancellation request.
   */
  CANCEL_RESULT,
  /**
   * Provokes a communication failure during the download.
   */
  DOWNLOAD_COMMUNICATION,
  /**
   * Provokes an invalid XLIFF, that cannot be imported.
   */
  DOWNLOAD_XLIFF,
  /**
   * Provoke an error-state of the submission at the (mocked) GCC
   * backend.
   */
  SUBMISSION_ERROR,
  /**
   * Provokes a communication failure during the upload.
   */
  UPLOAD_COMMUNICATION,
  ;

  @NonNull
  public static Optional<MockError> tryParse(@NonNull String value) {
    if (value.isEmpty()) {
      return Optional.empty();
    }
    return Arrays.stream(values())
      .filter(e -> e.toString().equalsIgnoreCase(value))
      .findAny();
  }
}
