package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals an issue with a submission at GlobalLink.
 */
@NullMarked
public class GCFacadeSubmissionException extends GCFacadeException {
  @Serial
  private static final long serialVersionUID = 3746655613283049534L;

  public GCFacadeSubmissionException() {
  }

  public GCFacadeSubmissionException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public GCFacadeSubmissionException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeSubmissionException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeSubmissionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
