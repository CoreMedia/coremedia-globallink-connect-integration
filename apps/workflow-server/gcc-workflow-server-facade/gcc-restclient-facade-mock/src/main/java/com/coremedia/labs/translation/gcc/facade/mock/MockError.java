package com.coremedia.labs.translation.gcc.facade.mock;

/**
 * Possible values for the {@code globalLink.mockError} setting.
 */
enum MockError {
  /**
   * Provokes an invalid XLIFF, that cannot be imported.
   */
  DOWNLOAD_XLIFF,
  /**
   * Provokes a communication failure during the download.
   */
  DOWNLOAD_COMMUNICATION,
  /**
   * Provokes a communication failure during the upload.
   */
  UPLOAD_COMMUNICATION,
  /**
   * Provokes a communication failure during the cancellation request.
   */
  CANCEL_COMMUNICATION,
  /**
   * Provokes a server-side failure during the cancellation request.
   */
  CANCEL_RESULT
}
