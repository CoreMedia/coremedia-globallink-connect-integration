package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals a communication error with the GCC REST Backend via GCC Java RestClient.
 */
@SuppressWarnings("unused")
@NullMarked
public class GCFacadeCommunicationException extends GCFacadeException {
  @Serial
  private static final long serialVersionUID = -4226793602127027111L;

  public GCFacadeCommunicationException() {
  }

  public GCFacadeCommunicationException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public GCFacadeCommunicationException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeCommunicationException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeCommunicationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
