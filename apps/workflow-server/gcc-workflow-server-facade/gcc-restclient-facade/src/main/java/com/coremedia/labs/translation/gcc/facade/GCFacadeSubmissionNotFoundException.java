package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.Serial;

/**
 * Signals that a submission was not found at GlobalLink.
 */
public class GCFacadeSubmissionNotFoundException extends GCFacadeSubmissionException {
  @Serial
  private static final long serialVersionUID = 7381206719419170238L;

  public GCFacadeSubmissionNotFoundException() {
  }

  public GCFacadeSubmissionNotFoundException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public GCFacadeSubmissionNotFoundException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeSubmissionNotFoundException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeSubmissionNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
