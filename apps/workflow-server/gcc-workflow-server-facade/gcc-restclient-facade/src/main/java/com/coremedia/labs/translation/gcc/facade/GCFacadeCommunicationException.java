package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Signals a communication error with the GCC REST Backend via GCC Java RestClient.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
public class GCFacadeCommunicationException extends GCFacadeException {
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
